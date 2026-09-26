package com.example.jobtracker.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("avatar")
public record AvatarProperties(
        @Min(1) int maxDimension,
        @Min(1) int thumbnailSize,
        @NotNull Duration urlTtl,
        @Valid @NotNull S3 s3) {

    public record S3(
            @NotBlank String endpoint,
            @NotBlank String publicEndpoint,
            @NotBlank String region,
            @NotBlank String accessKey,
            @NotBlank String secretKey,
            @NotBlank String bucket) {
    }
}
