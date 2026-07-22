package com.makershub.auth.service;

import com.makershub.auth.dto.AuthRequest;
import com.makershub.auth.dto.AuthResponse;
import com.makershub.auth.entity.OtpVerification;
import com.makershub.auth.entity.User;
import com.makershub.auth.enums.UserRole;
import com.makershub.auth.repository.OtpVerificationRepository;
import com.makershub.auth.repository.UserRepository;
import com.makershub.auth.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthAppService authAppService;

    @Test
    void register_success() {
        AuthRequest.RegisterRequest request = new AuthRequest.RegisterRequest();
        request.setPhoneNumber("+233201234567");
        request.setPassword("password123");
        request.setFullName("Kofi Mensah");
        request.setRole(UserRole.SME_OWNER);

        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponse.UserSummaryResponse response = authAppService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getPhoneNumber()).isEqualTo("+233201234567");
        assertThat(response.getRole()).isEqualTo(UserRole.SME_OWNER);
        verify(otpVerificationRepository).save(any(OtpVerification.class));
    }

    @Test
    void register_throwsConflict_whenPhoneExists() {
        AuthRequest.RegisterRequest request = new AuthRequest.RegisterRequest();
        request.setPhoneNumber("+233201234567");
        request.setPassword("password123");
        request.setFullName("Kofi Mensah");
        request.setRole(UserRole.SME_OWNER);

        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);

        assertThatThrownBy(() -> authAppService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Phone number already registered");
    }

    @Test
    void verifyOtp_success() {
        UUID userId = UUID.randomUUID();
        AuthRequest.OtpRequest request = new AuthRequest.OtpRequest();
        request.setPhoneNumber("+233201234567");
        request.setOtp("1234");

        OtpVerification verification = OtpVerification.builder()
                .phoneNumber(request.getPhoneNumber())
                .otpCode("1234")
                .expiryTime(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        User user = User.builder()
                .id(userId)
                .phoneNumber(request.getPhoneNumber())
                .fullName("Kofi Mensah")
                .role(UserRole.SME_OWNER)
                .isVerified(false)
                .isActive(true)
                .build();

        when(otpVerificationRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.of(verification));
        when(userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse.TokenResponse response = authAppService.verifyOtp(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(user.getIsVerified()).isTrue();
        verify(otpVerificationRepository).delete(verification);
    }
}
