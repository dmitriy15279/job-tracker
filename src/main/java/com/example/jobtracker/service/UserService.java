package com.example.jobtracker.service;

import com.example.jobtracker.controller.dto.CompanyResponse;
import com.example.jobtracker.controller.dto.CreateUserRequest;
import com.example.jobtracker.controller.dto.PageResponse;
import com.example.jobtracker.controller.dto.UserResponse;
import com.example.jobtracker.persistence.UserRepository;
import com.example.jobtracker.persistence.UserSpecifications;
import com.example.jobtracker.persistence.entity.Company;
import com.example.jobtracker.persistence.entity.User;
import com.example.jobtracker.persistence.entity.UserType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository repository;
    private final Clock clock;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw duplicateEmail(email);
        }

        User user = new User(
                null,
                request.firstName().strip(),
                request.lastName().strip(),
                request.birthDate(),
                email,
                request.address() == null || request.address().isBlank() ? null : request.address().strip(),
                request.userType(),
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS),
                new HashSet<>());

        User saved = repository.save(user);
        log.info("Created {} user {}", saved.getUserType(), saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(
            int page, int size, String firstName, String lastName, String email, UserType userType,
            LocalDate birthDateFrom, LocalDate birthDateTo, UUID companyId) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be >= 0 and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (birthDateFrom != null && birthDateTo != null && birthDateFrom.isAfter(birthDateTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDateFrom cannot be after birthDateTo");
        }
        Specification<User> spec = UserSpecifications.filter(
                firstName, lastName, email, userType, birthDateFrom, birthDateTo, companyId);
        Page<UserResponse> result = repository
                .findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(UserService::toResponse);
        log.debug("Fetched page {} of {} ({} total users) with filters firstName='{}', lastName='{}', email='{}', "
                        + "userType={}, birthDateFrom={}, birthDateTo={}, companyId={}",
                result.getNumber(), result.getTotalPages(), result.getTotalElements(),
                firstName, lastName, email, userType, birthDateFrom, birthDateTo, companyId);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOrThrow(id));
        log.info("Deleted user {}", id);
    }

    private User findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User {} not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + id + " not found");
                });
    }

    static UserResponse toResponse(User user) {
        List<CompanyResponse> companies = user.getCompanies().stream()
                .sorted(Comparator.comparing(Company::getName))
                .map(CompanyService::toResponse)
                .toList();
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate(),
                user.getEmail(),
                user.getAddress(),
                user.getUserType(),
                user.getCreatedAt(),
                companies);
    }

    private static ResponseStatusException duplicateEmail(String email) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "User with email '" + email + "' already exists");
    }
}
