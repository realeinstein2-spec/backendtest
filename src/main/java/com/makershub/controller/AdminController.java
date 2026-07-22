package com.makershub.controller;

import com.makershub.dto.request.FactoryRequest;
import com.makershub.dto.response.ApiResponse;
import com.makershub.entity.Factory;
import com.makershub.enums.VerificationStatus;
import com.makershub.mapper.DtoMapper;
import com.makershub.service.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;
    private final DtoMapper mapper;

    @GetMapping("/factories/verification-queue")
    public ResponseEntity<Page<Factory>> getVerificationQueue(@RequestParam(required = false) VerificationStatus status,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getVerificationQueue(status, pageable));
    }

    @PatchMapping("/factories/{id}/verify")
    public ResponseEntity<Factory> verifyFactory(@PathVariable UUID id, @Valid @RequestBody FactoryRequest.VerificationReviewRequest request) {
        return ResponseEntity.ok(adminService.reviewFactoryVerification(id, request));
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable UUID id, @RequestParam String reason) {
        adminService.suspendUser(id, reason);
        return ResponseEntity.ok(ApiResponse.ErrorResponse.builder().message("User suspended").build());
    }

    @PutMapping("/users/{id}/unsuspend")
    public ResponseEntity<?> unsuspendUser(@PathVariable UUID id) {
        adminService.unsuspendUser(id);
        return ResponseEntity.ok(ApiResponse.ErrorResponse.builder().message("User unsuspended").build());
    }

    @GetMapping("/users/active")
    public ResponseEntity<ApiResponse.PagedResponse<com.makershub.dto.response.AuthResponse.UserSummaryResponse>> getActiveUsers(
            @RequestParam(defaultValue = "5") int minutes,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<com.makershub.entity.User> page = adminService.getActiveUsers(minutes, pageable);
        Page<com.makershub.dto.response.AuthResponse.UserSummaryResponse> mapped = page.map(mapper::toUserSummary);
        return ResponseEntity.ok(ApiResponse.PagedResponse.<com.makershub.dto.response.AuthResponse.UserSummaryResponse>builder()
                .content(mapped.getContent())
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalElements(mapped.getTotalElements())
                .totalPages(mapped.getTotalPages())
                .build());
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse.PagedResponse<com.makershub.dto.response.AuthResponse.UserSummaryResponse>> getUsers(
            @RequestParam(required = false) com.makershub.enums.UserRole role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<com.makershub.entity.User> page = adminService.getUsersList(role, isActive, search, pageable);
        Page<com.makershub.dto.response.AuthResponse.UserSummaryResponse> mapped = page.map(mapper::toUserSummary);
        return ResponseEntity.ok(ApiResponse.PagedResponse.<com.makershub.dto.response.AuthResponse.UserSummaryResponse>builder()
                .content(mapped.getContent())
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalElements(mapped.getTotalElements())
                .totalPages(mapped.getTotalPages())
                .build());
    }

    @GetMapping("/users/all")
    public ResponseEntity<java.util.List<com.makershub.dto.response.AuthResponse.UserSummaryResponse>> getAllUsers() {
        java.util.List<com.makershub.entity.User> users = adminService.getAllUsers();
        java.util.List<com.makershub.dto.response.AuthResponse.UserSummaryResponse> mapped = users.stream()
                .map(mapper::toUserSummary)
                .toList();
        return ResponseEntity.ok(mapped);
    }
}
