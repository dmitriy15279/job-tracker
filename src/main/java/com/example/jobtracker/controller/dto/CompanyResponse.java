package com.example.jobtracker.controller.dto;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name) {
}
