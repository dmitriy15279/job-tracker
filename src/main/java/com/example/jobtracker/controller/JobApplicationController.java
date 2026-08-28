package com.example.jobtracker.controller;

import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.controller.dto.CreateJobApplicationRequest;
import com.example.jobtracker.controller.dto.JobApplicationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody CreateJobApplicationRequest request) {
        JobApplicationResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/job-applications/" + response.id())).body(response);
    }

    @GetMapping
    public List<JobApplicationResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public JobApplicationResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
