package com.makershub.controller;

import com.makershub.dto.request.DisputeRequest;
import com.makershub.dto.response.ApiResponse;
import com.makershub.dto.response.DisputeResponse;
import com.makershub.service.DisputeService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Disputes")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/orders/{orderId}/dispute")
    @PreAuthorize("hasAnyRole('SME_OWNER', 'FACTORY_OWNER')")
    public ResponseEntity<DisputeResponse.DisputeDetailResponse> raiseDispute(@PathVariable UUID orderId,
                                                                              @Valid @RequestBody DisputeRequest.CreateDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disputeService.raiseDispute(orderId, request));
    }

    @GetMapping("/admin/disputes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse.PagedResponse<DisputeResponse.DisputeDetailResponse>> listDisputes(@PageableDefault(size = 20) Pageable pageable) {
        Page<DisputeResponse.DisputeDetailResponse> page = disputeService.listDisputes(pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.<DisputeResponse.DisputeDetailResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build());
    }

    @PatchMapping("/admin/disputes/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeResponse.DisputeDetailResponse> resolveDispute(@PathVariable UUID id,
                                                                                @Valid @RequestBody DisputeRequest.ResolveDisputeRequest request) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, request));
    }
}
