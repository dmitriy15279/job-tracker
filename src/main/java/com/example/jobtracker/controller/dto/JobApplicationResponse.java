package com.example.jobtracker.controller.dto;

import java.time.LocalDate;

public record JobApplicationResponse(
        Long id,
        String company,
        String position,
        LocalDate appliedDate) {
}
