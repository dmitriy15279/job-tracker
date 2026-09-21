package com.example.jobtracker.controller.dto;

public record SeedJobApplicationsResponse(
        int requestedCount,
        int createdCount,
        int failedCount) {
}
