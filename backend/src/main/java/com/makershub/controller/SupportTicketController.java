package com.makershub.controller;

import com.makershub.dto.request.SupportTicketRequest;
import com.makershub.dto.response.SupportTicketResponse;
import com.makershub.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Support Tickets")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    @Operation(summary = "Create support ticket",
               description = "Allows users (SMEs / Manufacturers) to request support. Tickets automatically get a 12-hour SLA resolution deadline.")
    public ResponseEntity<SupportTicketResponse.SupportTicketDetailResponse> createTicket(
            @Valid @RequestBody SupportTicketRequest.CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.createTicket(request));
    }

    @GetMapping
    @Operation(summary = "Get user's support tickets",
               description = "Returns a paginated list of support tickets created by the authenticated user.")
    public ResponseEntity<Page<SupportTicketResponse.SupportTicketSummaryResponse>> getUserTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(supportTicketService.getUserTickets(pageable));
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get support ticket details",
               description = "Returns full support ticket details and discussion thread.")
    public ResponseEntity<SupportTicketResponse.SupportTicketDetailResponse> getTicketDetails(
            @PathVariable UUID ticketId) {
        return ResponseEntity.ok(supportTicketService.getTicketDetails(ticketId));
    }

    @PostMapping("/{ticketId}/messages")
    @Operation(summary = "Reply to support ticket thread",
               description = "Adds a new reply message to the support ticket thread.")
    public ResponseEntity<SupportTicketResponse.SupportMessageDetailResponse> addMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportTicketRequest.AddMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.addMessage(ticketId, request));
    }
}
