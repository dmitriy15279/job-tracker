package com.example.jobtracker.controller;

import com.example.jobtracker.controller.dto.CompanyResponse;
import com.example.jobtracker.controller.dto.CreateCompanyRequest;
import com.example.jobtracker.controller.dto.PageResponse;
import com.example.jobtracker.service.CompanyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        log.info("POST /api/companies name='{}'", request.name());
        CompanyResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/companies/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<CompanyResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name) {
        log.debug("GET /api/companies page={} size={} name={}", page, size, name);
        return service.search(page, size, name);
    }

    @GetMapping("/{id}")
    public CompanyResponse getById(@PathVariable UUID id) {
        log.debug("GET /api/companies/{}", id);
        return service.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("DELETE /api/companies/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
