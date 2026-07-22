package com.makershub.notification.controller;

import com.makershub.notification.dto.NotificationResponse;
import com.makershub.notification.service.NotificationAppService;
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
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationAppService notificationAppService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse.NotificationDetailResponse>> getNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(notificationAppService.getNotifications(userId, pageable));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable UUID id) {
        UUID userId = parseUserId(userIdHeader);
        notificationAppService.markAsRead(userId, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        UUID userId = parseUserId(userIdHeader);
        notificationAppService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
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
