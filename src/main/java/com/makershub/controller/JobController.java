package com.makershub.controller;

import com.makershub.dto.request.JobRequest;
import com.makershub.dto.response.ApiResponse;
import com.makershub.dto.response.JobResponse;
import com.makershub.service.JobService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Job Posting")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SME_OWNER', 'ENTERPRISE')")
    public ResponseEntity<JobResponse.JobDetailResponse> createJob(@Valid @RequestBody JobRequest.CreateJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('FACTORY_OWNER')")
    public ResponseEntity<ApiResponse.PagedResponse<JobResponse.JobDetailResponse>> listOpenJobs(
            @RequestParam(required = false) String sectorTag,
            @RequestParam(required = false) String minBudget,
            @RequestParam(required = false) String maxBudget,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<JobResponse.JobDetailResponse> page = jobService.listOpenJobs(sectorTag, minBudget, maxBudget, pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.<JobResponse.JobDetailResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build());
    }

    @GetMapping("/my-jobs")
    @PreAuthorize("hasAnyRole('SME_OWNER', 'ENTERPRISE')")
    public ResponseEntity<ApiResponse.PagedResponse<JobResponse.JobDetailResponse>> listMyJobs(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<JobResponse.JobDetailResponse> page = jobService.listMyJobs(pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.<JobResponse.JobDetailResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse.JobDetailResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }
}
