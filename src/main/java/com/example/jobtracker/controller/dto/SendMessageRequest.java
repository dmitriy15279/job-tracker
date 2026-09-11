package com.example.jobtracker.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank String login,
        @NotBlank String message) {
}
