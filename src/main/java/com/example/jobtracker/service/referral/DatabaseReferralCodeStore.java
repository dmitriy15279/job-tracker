package com.example.jobtracker.service.referral;

import com.example.jobtracker.persistence.ReferralCodeRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@ConditionalOnProperty(name = "referral.storage", havingValue = "database")
public class DatabaseReferralCodeStore implements ReferralCodeStore {

    private final ReferralCodeRepository repository;
    private final Clock clock;

    public DatabaseReferralCodeStore(ReferralCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        log.info("Referral codes are stored in the database (expired by cleanup cron job)");
    }

    @Override
    public boolean saveIfAbsent(String code, UUID companyId, LocalDateTime createdAt, Duration ttl) {
        return repository.insertIfAbsent(code, companyId, createdAt, createdAt.plus(ttl)) == 1;
    }

    @Override
    public Optional<UUID> findCompanyId(String code) {
        return repository.findActiveCompanyId(code, LocalDateTime.now(clock));
    }
}
