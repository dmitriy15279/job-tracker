package com.example.jobtracker.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Validated
@ConfigurationProperties("referral")
public record ReferralProperties(
        @NotNull Storage storage,
        @NotNull Duration ttl,
        @Min(6) @Max(16) int codeLength,
        @NotBlank String cleanupCron) {

    public enum Storage {
        REDIS,
        DATABASE
    }
}
