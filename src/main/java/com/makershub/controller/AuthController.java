package com.makershub.controller;

import com.makershub.dto.request.AuthRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new SME or factory owner")
    public ResponseEntity<AuthResponse.UserSummaryResponse> register(@Valid @RequestBody AuthRequest.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with credentials; issues OTP for MFA verification")
    public ResponseEntity<AuthResponse.PendingAuthResponse> login(@Valid @RequestBody AuthRequest.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse.TokenResponse> refresh(@Valid @RequestBody AuthRequest.RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP and receive JWT tokens")
    public ResponseEntity<AuthResponse.TokenResponse> verify(@Valid @RequestBody AuthRequest.OtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }
}
