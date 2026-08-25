package com.example.jobtracker.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateJobApplicationRequest(
        @NotBlank String company,
        @NotBlank String position,
        @NotNull LocalDate appliedDate) {
}
