package com.example.jobtracker.service;

import com.example.jobtracker.controller.dto.CompanyResponse;
import com.example.jobtracker.controller.dto.CreateCompanyRequest;
import com.example.jobtracker.controller.dto.PageResponse;
import com.example.jobtracker.persistence.CompanyRepository;
import com.example.jobtracker.persistence.CompanySpecifications;
import com.example.jobtracker.persistence.entity.Company;
import com.example.jobtracker.persistence.entity.UserType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CompanyRepository repository;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {
        String name = request.name().strip();
        if (repository.existsByNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        Company saved;
        try {
            saved = repository.saveAndFlush(new Company(null, name));
        } catch (DataIntegrityViolationException e) {
            throw duplicateName(name);
        }
        log.info("Created company {} '{}'", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> search(int page, int size, String name) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be >= 0 and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        Page<CompanyResponse> result = repository
                .findAll(CompanySpecifications.filter(name), PageRequest.of(page, size, Sort.by("name")))
                .map(CompanyService::toResponse);
        log.debug("Fetched page {} of {} ({} total companies) with filter name='{}'",
                result.getNumber(), result.getTotalPages(), result.getTotalElements(), name);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public void delete(UUID id) {
        Company company = findOrThrow(id);
        if (repository.existsUserWithOnlyCompany(id, UserType.BUSINESS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Company " + id + " is the only company of a business user and cannot be deleted");
        }
        repository.delete(company);
        log.info("Deleted company {}", id);
    }

    static CompanyResponse toResponse(Company company) {
        return new CompanyResponse(company.getId(), company.getName());
    }

    private Company findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Company {} not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Company " + id + " not found");
                });
    }

    private static ResponseStatusException duplicateName(String name) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Company '" + name + "' already exists");
    }
}
