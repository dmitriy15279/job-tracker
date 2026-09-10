package com.example.jobtracker.controller.dto;

import java.time.LocalDateTime;

public record RandomValueResponse(int r, LocalDateTime generatedAt) {
}
