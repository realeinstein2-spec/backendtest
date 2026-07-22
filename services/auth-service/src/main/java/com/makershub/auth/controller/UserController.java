package com.makershub.auth.controller;

import com.makershub.auth.dto.AuthResponse;
import com.makershub.auth.dto.UserRequest;
import com.makershub.auth.service.UserAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAppService userAppService;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserSummaryResponse> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(userAppService.getUserById(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse.UserSummaryResponse> updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody UserRequest.UpdateProfileRequest request) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(userAppService.updateProfile(userId, request));
    }

    @PutMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody UserRequest.UpdateFcmTokenRequest request) {
        UUID userId = parseUserId(userIdHeader);
        userAppService.updateFcmToken(userId, request);
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
