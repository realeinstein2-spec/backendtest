package com.makershub.orderpayment.controller;

import com.makershub.orderpayment.dto.OrderDto;
import com.makershub.orderpayment.service.OrderPaymentAppService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderPaymentAppService orderPaymentAppService;

    @PostMapping
    public ResponseEntity<OrderDto.OrderDetailResponse> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody OrderDto.CreateOrderRequest request) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderPaymentAppService.createOrder(userId, request));
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto.OrderDetailResponse>> getOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(orderPaymentAppService.getOrdersForUser(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.OrderDetailResponse> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderPaymentAppService.getOrderById(id));
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
