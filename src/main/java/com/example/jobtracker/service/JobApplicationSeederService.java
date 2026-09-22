package com.example.jobtracker.service;

import com.example.jobtracker.config.JobTrackerProperties;
import com.example.jobtracker.controller.dto.CreateJobApplicationRequest;
import com.example.jobtracker.controller.dto.JobApplicationResponse;
import com.example.jobtracker.controller.dto.SeedJobApplicationsResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationSeederService {

    private static final int MAX_SEED_COUNT = 10_000;

    private static final List<String> COMPANIES = List.of(
            "Acme Corp", "Globex", "Initech", "Umbrella Corp", "Stark Industries",
            "Wayne Enterprises", "Hooli", "Wonka Industries", "Soylent Corp", "Cyberdyne Systems");

    private static final List<String> POSITIONS = List.of(
            "Backend Developer", "Frontend Developer", "Full Stack Engineer", "QA Engineer",
            "DevOps Engineer", "Data Engineer", "Product Manager", "Engineering Manager");

    private final RestTemplate restTemplate;
    private final Clock clock;
    private final JobTrackerProperties properties;

    public SeedJobApplicationsResponse seed(int count) {
        if (count < 1 || count > MAX_SEED_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "count must be between 1 and " + MAX_SEED_COUNT);
        }

        int created = 0;
        int failed = 0;
        String createUrl = properties.baseUrl() + "/api/job-applications";

        for (int i = 0; i < count; i++) {
            CreateJobApplicationRequest request = randomRequest();
            try {
                restTemplate.postForEntity(createUrl, request, JobApplicationResponse.class);
                created++;
            } catch (RestClientException e) {
                failed++;
                log.warn("Failed to seed job application for company '{}': {}", request.company(), e.getMessage());
            }
        }

        log.info("Seeded job applications: requested={}, created={}, failed={}", count, created, failed);
        return new SeedJobApplicationsResponse(count, created, failed);
    }

    private CreateJobApplicationRequest randomRequest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String company = COMPANIES.get(random.nextInt(COMPANIES.size()));
        String position = POSITIONS.get(random.nextInt(POSITIONS.size()));
        LocalDate appliedDate = LocalDate.now(clock).minusDays(random.nextInt(0, 90));
        return new CreateJobApplicationRequest(company, position, appliedDate);
    }
}
