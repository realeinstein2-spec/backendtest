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
}
