package com.example.jobtracker.controller;

import com.example.jobtracker.controller.dto.RandomValueResponse;
import com.example.jobtracker.service.RandomValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/random")
public class RandomValueController {

    private final RandomValueService service;

    public RandomValueController(RandomValueService service) {
        this.service = service;
    }

    @GetMapping
    public RandomValueResponse getR() {
        log.debug("GET /api/random");
        return service.getR();
    }
}
