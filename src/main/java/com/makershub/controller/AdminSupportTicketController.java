package com.makershub.controller;

import com.makershub.dto.request.SupportTicketRequest;
import com.makershub.dto.response.SupportTicketResponse;
import com.makershub.enums.SupportTicketCategory;
import com.makershub.enums.SupportTicketStatus;
import com.makershub.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/support/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Support Management")
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    @Operation(summary = "Admin: Get all support tickets",
               description = "Returns paginated list of all support tickets with optional status and category filtering.")
    public ResponseEntity<Page<SupportTicketResponse.SupportTicketSummaryResponse>> getAllTickets(
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportTicketCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(supportTicketService.adminGetAllTickets(status, category, pageable));
    }

    @PutMapping("/{ticketId}/status")
    @Operation(summary = "Admin: Update ticket status & resolution",
               description = "Allows admin to update ticket status (OPEN, IN_PROGRESS, RESOLVED, CLOSED), assign admin, and append notes.")
    public ResponseEntity<SupportTicketResponse.SupportTicketDetailResponse> updateTicketStatus(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportTicketRequest.UpdateTicketStatusRequest request) {
        return ResponseEntity.ok(supportTicketService.adminUpdateTicketStatus(ticketId, request));
    }

    @GetMapping("/stats")
    @Operation(summary = "Admin: Get support system stats & SLA breaches",
               description = "Returns support ticket metrics including count of tickets breaching the 12-hour SLA window.")
    public ResponseEntity<SupportTicketResponse.SupportTicketStatsResponse> getStats() {
        return ResponseEntity.ok(supportTicketService.adminGetStats());
    }
}
