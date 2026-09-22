package com.example.jobtracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job-tracker")
public record JobTrackerProperties(String baseUrl) {
}
