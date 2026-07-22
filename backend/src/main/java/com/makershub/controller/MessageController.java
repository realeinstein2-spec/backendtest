package com.makershub.controller;

import com.makershub.dto.request.MessageRequest;
import com.makershub.dto.response.ApiResponse;
import com.makershub.dto.response.MessageResponse;
import com.makershub.service.MessageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Messaging")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse.MessageDetailResponse> sendMessage(@Valid @RequestBody MessageRequest.CreateMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(request));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse.PagedResponse<MessageResponse.MessageDetailResponse>> listMessages(
            @PathVariable UUID orderId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<MessageResponse.MessageDetailResponse> page = messageService.listMessages(orderId, pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.<MessageResponse.MessageDetailResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build());
    }
}
