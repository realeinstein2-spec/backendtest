package com.makershub.auth.controller;

import com.makershub.auth.dto.AuthRequest;
import com.makershub.auth.dto.AuthResponse;
import com.makershub.auth.service.AuthAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse.UserSummaryResponse> register(@Valid @RequestBody AuthRequest.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authAppService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse.PendingAuthResponse> login(@Valid @RequestBody AuthRequest.LoginRequest request) {
        return ResponseEntity.ok(authAppService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse.TokenResponse> refresh(@Valid @RequestBody AuthRequest.RefreshRequest request) {
        return ResponseEntity.ok(authAppService.refresh(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse.TokenResponse> verify(@Valid @RequestBody AuthRequest.OtpRequest request) {
        return ResponseEntity.ok(authAppService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse.PendingAuthResponse> resendOtp(@Valid @RequestBody AuthRequest.ResendOtpRequest request) {
        return ResponseEntity.ok(authAppService.resendOtp(request));
    }
}
