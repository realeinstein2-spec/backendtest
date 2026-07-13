package com.makershub.controller;

import com.makershub.dto.request.OrderRequest;
import com.makershub.dto.response.OrderResponse;
import com.makershub.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
