package com.makershub.orderpayment.controller;

import com.makershub.orderpayment.dto.DisputeDto;
import com.makershub.orderpayment.service.OrderPaymentAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final OrderPaymentAppService orderPaymentAppService;

    @PostMapping
    public ResponseEntity<DisputeDto.DisputeDetailResponse> raiseDispute(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody DisputeDto.RaiseDisputeRequest request) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderPaymentAppService.raiseDispute(userId, request));
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
