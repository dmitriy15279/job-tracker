package com.example.jobtracker.controller.dto;

import java.time.LocalDateTime;

public record AvatarResponse(
        String originalUrl,
        String thumbnailUrl,
        LocalDateTime expiresAt,
        String contentType,
        long sizeBytes,
        int width,
        int height,
        LocalDateTime uploadedAt) {
}
