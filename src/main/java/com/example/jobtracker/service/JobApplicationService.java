package com.example.jobtracker.service;

import com.example.jobtracker.persistence.entity.JobApplication;
import com.example.jobtracker.config.CacheConfig;
import com.example.jobtracker.controller.dto.CreateJobApplicationRequest;
import com.example.jobtracker.controller.dto.JobApplicationResponse;
import com.example.jobtracker.controller.dto.PageResponse;
import com.example.jobtracker.persistence.JobApplicationRepository;
import com.example.jobtracker.persistence.JobApplicationSpecifications;
import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobApplicationRepository repository;
    private final Clock clock;

    public JobApplicationResponse create(CreateJobApplicationRequest request) {
        if (request.appliedDate().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appliedDate cannot be in the future");
        }
        JobApplication jobApplication = new JobApplication(
                null,
                request.company(),
                request.position(),
                request.appliedDate());
        JobApplication saved = repository.save(jobApplication);
        log.info("Created job application {} for company '{}'", saved.getId(), saved.getCompany());
        return toResponse(saved);
    }

    public PageResponse<JobApplicationResponse> getAll(
            int page, int size, String company, String position, LocalDate appliedFrom, LocalDate appliedTo) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be >= 0 and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (appliedFrom != null && appliedTo != null && appliedFrom.isAfter(appliedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appliedFrom cannot be after appliedTo");
        }
        Specification<JobApplication> spec =
                JobApplicationSpecifications.filter(company, position, appliedFrom, appliedTo);
        Page<JobApplicationResponse> result = repository.findAll(spec, PageRequest.of(page, size))
                .map(this::toResponse);
        log.debug("Fetched page {} of {} ({} total job applications) with filters company='{}', position='{}', appliedFrom={}, appliedTo={}",
                result.getNumber(), result.getTotalPages(), result.getTotalElements(), company, position, appliedFrom, appliedTo);
        return PageResponse.from(result);
    }

    @Cacheable(cacheNames = CacheConfig.JOB_APPLICATIONS_CACHE, key = "#id", cacheManager = "redisCacheManager")
    public JobApplicationResponse getById(Long id) {
        log.info("Cache miss for job application {} - loading from database", id);
        JobApplication jobApplication = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Job application {} not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Job application " + id + " not found");
                });
        return toResponse(jobApplication);
    }

    private JobApplicationResponse toResponse(JobApplication jobApplication) {
        return new JobApplicationResponse(
                jobApplication.getId(),
                jobApplication.getCompany(),
                jobApplication.getPosition(),
                jobApplication.getAppliedDate());
    }
}
