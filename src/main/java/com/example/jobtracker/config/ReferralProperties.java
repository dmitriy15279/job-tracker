package com.example.jobtracker.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Referral code settings ({@code referral.*} in application.yml).
 * {@code storage} selects which {@code ReferralCodeStore} implementation is created at startup.
 */
@Validated
@ConfigurationProperties("referral")
public record ReferralProperties(
        @NotNull Storage storage,
        @NotNull Duration ttl,
        // referral_codes.code is VARCHAR(16)
        @Min(6) @Max(16) int codeLength,
        @NotBlank String cleanupCron) {

    public enum Storage {
        REDIS,
        DATABASE
    }
}
