package com.makershub.controller;

import com.makershub.dto.request.OrderProgressRequest;
import com.makershub.dto.request.OrderRequest;
import com.makershub.dto.response.OrderProgressResponse;
import com.makershub.dto.response.OrderResponse;
import com.makershub.service.OrderService;
import java.util.List;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order Lifecycle")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponse.OrderDetailResponse>> listMyOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.listMyOrders(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse.OrderDetailResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FACTORY_OWNER')")
    public ResponseEntity<OrderResponse.OrderDetailResponse> updateStatus(@PathVariable UUID id,
                                                                         @Valid @RequestBody OrderRequest.StatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    @PostMapping("/{id}/confirm-delivery")
    @PreAuthorize("hasRole('SME_OWNER')")
    public ResponseEntity<OrderResponse.OrderDetailResponse> confirmDelivery(@PathVariable UUID id,
                                                                             @Valid @RequestBody OrderRequest.ConfirmDeliveryRequest request) {
        return ResponseEntity.ok(orderService.confirmDelivery(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SME_OWNER', 'ENTERPRISE')")
    public ResponseEntity<OrderResponse.OrderDetailResponse> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PostMapping("/{id}/progress")
    @PreAuthorize("hasRole('FACTORY_OWNER')")
    public ResponseEntity<OrderProgressResponse> addProgress(
            @PathVariable UUID id,
            @Valid @RequestBody OrderProgressRequest request) {
        return ResponseEntity.ok(orderService.addProgress(id, request));
    }

    @GetMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderProgressResponse>> getProgressHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getProgressHistory(id));
    }
}

