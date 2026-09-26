package com.example.jobtracker.controller;

import com.example.jobtracker.controller.dto.AvatarResponse;
import com.example.jobtracker.controller.dto.CreateUserRequest;
import com.example.jobtracker.controller.dto.JoinCompanyRequest;
import com.example.jobtracker.controller.dto.PageResponse;
import com.example.jobtracker.controller.dto.UserResponse;
import com.example.jobtracker.persistence.entity.UserType;
import com.example.jobtracker.service.AvatarService;
import com.example.jobtracker.service.ReferralService;
import com.example.jobtracker.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;
    private final ReferralService referralService;
    private final AvatarService avatarService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/users userType={}", request.userType());
        UserResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<UserResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDateTo,
            @RequestParam(required = false) UUID companyId) {
        log.debug("GET /api/users page={} size={} firstName={} lastName={} email={} userType={} "
                        + "birthDateFrom={} birthDateTo={} companyId={}",
                page, size, firstName, lastName, email, userType, birthDateFrom, birthDateTo, companyId);
        return service.search(page, size, firstName, lastName, email, userType, birthDateFrom, birthDateTo, companyId);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        log.debug("GET /api/users/{}", id);
        return service.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("DELETE /api/users/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/companies/join")
    public UserResponse joinCompany(@PathVariable UUID id, @Valid @RequestBody JoinCompanyRequest request) {
        log.info("POST /api/users/{}/companies/join", id);
        return referralService.join(id, request.code());
    }

    @PutMapping(path = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarResponse uploadAvatar(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        log.info("PUT /api/users/{}/avatar fileName={} size={}", id, file.getOriginalFilename(), file.getSize());
        return avatarService.upload(id, file);
    }

    @GetMapping("/{id}/avatar")
    public AvatarResponse getAvatar(@PathVariable UUID id) {
        log.debug("GET /api/users/{}/avatar", id);
        return avatarService.get(id);
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<Void> deleteAvatar(@PathVariable UUID id) {
        log.info("DELETE /api/users/{}/avatar", id);
        avatarService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
