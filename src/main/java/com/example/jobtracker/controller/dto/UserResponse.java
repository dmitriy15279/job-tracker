package com.example.jobtracker.controller.dto;

import com.example.jobtracker.persistence.entity.UserType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email,
        String address,
        UserType userType,
        LocalDateTime createdAt,
        List<CompanyResponse> companies) {
}
