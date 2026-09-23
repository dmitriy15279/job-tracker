package com.example.jobtracker.service;

import com.example.jobtracker.config.ReferralProperties;
import com.example.jobtracker.controller.dto.ReferralCodeResponse;
import com.example.jobtracker.controller.dto.UserResponse;
import com.example.jobtracker.persistence.CompanyRepository;
import com.example.jobtracker.persistence.UserRepository;
import com.example.jobtracker.persistence.entity.Company;
import com.example.jobtracker.persistence.entity.User;
import com.example.jobtracker.persistence.entity.UserType;
import com.example.jobtracker.service.referral.ReferralCodeGenerator;
import com.example.jobtracker.service.referral.ReferralCodeStore;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final ReferralCodeStore store;
    private final ReferralCodeGenerator generator;
    private final ReferralProperties properties;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ReferralCodeResponse generate(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw companyNotFound(companyId);
        }
        LocalDateTime createdAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = generator.generate();
            boolean saved;
            try {
                saved = store.saveIfAbsent(code, companyId, createdAt, properties.ttl());
            } catch (DataIntegrityViolationException e) {
                // Database storage only: the company was deleted between the check above and the insert
                throw companyNotFound(companyId);
            }
            if (saved) {
                log.info("Created referral code for company {} (expires in {})", companyId, properties.ttl());
                return new ReferralCodeResponse(code, companyId, createdAt.plus(properties.ttl()));
            }
            log.warn("Referral code collision on attempt {}, generating another one", attempt);
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate a unique referral code");
    }

    @Transactional
    public UserResponse join(UUID userId, String rawCode) {
        String code = rawCode.strip().toUpperCase(Locale.ROOT);
        UUID companyId = store.findCompanyId(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Referral code is invalid or expired"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + userId + " not found"));
        // A Redis code can outlive its company, so the company is checked separately
        Company company = companyRepository.findById(companyId).orElseThrow(() -> companyNotFound(companyId));

        if (user.getUserType() == UserType.INDIVIDUAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "individual user cannot join a company");
        }
        if (user.getCompanies().stream().anyMatch(c -> c.getId().equals(companyId))) {
            throw alreadyMember(userId, companyId);
        }
        user.getCompanies().add(company);
        try {
            // Flush now so a concurrent join of the same user hits the join table's primary key here
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw alreadyMember(userId, companyId);
        }
        log.info("User {} joined company {} by referral code", userId, companyId);
        return UserService.toResponse(user);
    }

    private static ResponseStatusException companyNotFound(UUID companyId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Company " + companyId + " not found");
    }

    private static ResponseStatusException alreadyMember(UUID userId, UUID companyId) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "User " + userId + " is already a member of company " + companyId);
    }
}
