package com.example.jobtracker.service.referral;

import com.example.jobtracker.persistence.ReferralCodeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@ConditionalOnProperty(name = "referral.storage", havingValue = "database")
public class ReferralCodeCleanupJob {

    private final ReferralCodeRepository repository;
    private final Clock clock;

    public ReferralCodeCleanupJob(ReferralCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${referral.cleanup-cron}")
    public void deleteExpired() {
        int deleted = repository.deleteExpired(LocalDateTime.now(clock));
        log.info("Referral code cleanup: deleted {} expired codes", deleted);
    }
}
