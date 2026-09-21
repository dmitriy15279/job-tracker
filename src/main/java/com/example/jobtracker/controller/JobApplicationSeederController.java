package com.example.jobtracker.controller;

import com.example.jobtracker.controller.dto.SeedJobApplicationsResponse;
import com.example.jobtracker.service.JobApplicationSeederService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationSeederController {

    private final JobApplicationSeederService service;

    public JobApplicationSeederController(JobApplicationSeederService service) {
        this.service = service;
    }

    @PostMapping("/seed")
    public SeedJobApplicationsResponse seed(@RequestParam(defaultValue = "10") int count) {
        log.info("POST /api/job-applications/seed count={}", count);
        return service.seed(count);
    }
}
