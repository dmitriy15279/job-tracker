package com.example.jobtracker.service;

import com.example.jobtracker.persistence.entity.JobApplication;
import com.example.jobtracker.config.CacheConfig;
import com.example.jobtracker.controller.dto.CreateJobApplicationRequest;
import com.example.jobtracker.controller.dto.JobApplicationResponse;
import com.example.jobtracker.controller.dto.PageResponse;
import com.example.jobtracker.persistence.JobApplicationRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

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

    public PageResponse<JobApplicationResponse> getAll(int page, int size) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be >= 1");
        }
        Page<JobApplicationResponse> result = repository.findAll(PageRequest.of(page, size))
                .map(this::toResponse);
        log.debug("Fetched page {} of {} ({} total job applications)",
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
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
