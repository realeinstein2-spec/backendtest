package com.makershub.controller;

import com.makershub.dto.request.FactoryRequest;
import com.makershub.dto.request.UserRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.dto.response.ApiResponse;
import com.makershub.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserSummaryResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PostMapping("/factory-profile")
    @PreAuthorize("hasRole('FACTORY_OWNER')")
    public ResponseEntity<AuthResponse.UserSummaryResponse> createFactoryProfile(@Valid @RequestBody FactoryRequest.CreateFactoryProfileRequest request) {
        return ResponseEntity.ok(userService.createFactoryProfile(request));
    }

    @PutMapping("/factory-profile")
    @PreAuthorize("hasRole('FACTORY_OWNER')")
    public ResponseEntity<AuthResponse.UserSummaryResponse> updateFactoryProfile(@Valid @RequestBody FactoryRequest.CreateFactoryProfileRequest request) {
        return ResponseEntity.ok(userService.updateFactoryProfile(request));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse.UserSummaryResponse> updateProfile(@Valid @RequestBody UserRequest.UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateFcmToken(@Valid @RequestBody UserRequest.UpdateFcmTokenRequest request) {
        userService.updateFcmToken(request);
        return ResponseEntity.ok().build();
    }
}
