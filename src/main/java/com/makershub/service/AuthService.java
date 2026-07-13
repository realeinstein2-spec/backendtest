package com.makershub.service;

import com.makershub.dto.request.AuthRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.UserRepository;
import com.makershub.security.JwtUtil;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuditLogger auditLogger;

    @Transactional
    public AuthResponse.UserSummaryResponse register(AuthRequest.RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number already registered", HttpStatus.CONFLICT, "PHONE_EXISTS");
        }
        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : UserRole.SME_OWNER)
                .ghanaCardNumber(request.getGhanaCardNumber())
                .region(request.getRegion())
                .town(request.getTown())
                .isVerified(false)
                .isActive(true)
                .ratingAvg(0.0)
                .totalOrders(0)
                .build();
        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.CREATE, "USER", saved.getId(), null, null);
        return AuthResponse.UserSummaryResponse.builder()
                .id(saved.getId())
                .phoneNumber(saved.getPhoneNumber())
                .fullName(saved.getFullName())
                .role(saved.getRole())
                .isVerified(saved.getIsVerified())
                .region(saved.getRegion())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse.TokenResponse login(AuthRequest.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword()));
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return buildTokenResponse(userDetails);
    }

    @Transactional(readOnly = true)
    public AuthResponse.TokenResponse refresh(AuthRequest.RefreshRequest request) {
        if (!jwtUtil.isTokenValid(request.getRefreshToken(), "REFRESH")) {
            throw new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }
        UUID userId = jwtUtil.extractUserId(request.getRefreshToken());
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return buildTokenResponse(new UserDetailsImpl(user));
    }

    private AuthResponse.TokenResponse buildTokenResponse(UserDetailsImpl userDetails) {
        return AuthResponse.TokenResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .accessTokenExpiry(jwtUtil.getAccessExpiry())
                .refreshTokenExpiry(jwtUtil.getRefreshExpiry())
                .tokenType("Bearer")
                .build();
    }
}
