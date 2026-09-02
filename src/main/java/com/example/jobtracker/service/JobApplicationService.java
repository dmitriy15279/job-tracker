package com.example.jobtracker.service;

import com.example.jobtracker.persistence.entity.JobApplication;
import com.example.jobtracker.controller.dto.CreateJobApplicationRequest;
import com.example.jobtracker.controller.dto.JobApplicationResponse;
import com.example.jobtracker.persistence.JobApplicationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

    public List<JobApplicationResponse> getAll() {
        List<JobApplicationResponse> responses = repository.findAll().stream()
                .map(this::toResponse)
                .toList();
        log.debug("Fetched {} job applications", responses.size());
        return responses;
    }

    public JobApplicationResponse getById(Long id) {
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
