package com.makershub.marketplace.controller;

import com.makershub.marketplace.dto.JobDto;
import com.makershub.marketplace.service.MarketplaceAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final MarketplaceAppService marketplaceAppService;

    @GetMapping
    public ResponseEntity<Page<JobDto.JobDetailResponse>> getOpenJobs(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(marketplaceAppService.getOpenJobs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDto.JobDetailResponse> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(marketplaceAppService.getJobById(id));
    }

    @PostMapping
    public ResponseEntity<JobDto.JobDetailResponse> createJob(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody JobDto.CreateJobRequest request) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplaceAppService.createJob(userId, request));
    }

    private UUID parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id format");
        }
    }
}
