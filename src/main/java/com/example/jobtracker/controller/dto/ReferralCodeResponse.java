package com.example.jobtracker.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReferralCodeResponse(
        String code,
        UUID companyId,
        LocalDateTime expiresAt) {
}
