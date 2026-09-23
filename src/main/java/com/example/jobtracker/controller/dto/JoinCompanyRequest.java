package com.example.jobtracker.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinCompanyRequest(
        @NotBlank @Size(max = 16) String code) {
}
