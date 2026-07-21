package com.makershub.service;

import com.makershub.dto.request.AuthRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.entity.User;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.UserRepository;
import com.makershub.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private com.makershub.repository.OtpVerificationRepository otpVerificationRepository;
    @Mock
    private com.makershub.notification.SmsService smsService;
    @Mock
    private org.springframework.core.env.Environment environment;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsNewUser() {
        AuthRequest.RegisterRequest request = new AuthRequest.RegisterRequest();
        request.setPhoneNumber("+233241234567");
        request.setPassword("SecurePass123");
        request.setFullName("Kwame Mensah");
        request.setRole(UserRole.SME_OWNER);

        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        AuthResponse.UserSummaryResponse response = authService.register(request);

        assertThat(response.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
        assertThat(response.getFullName()).isEqualTo(request.getFullName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsWhenPhoneExists() {
        AuthRequest.RegisterRequest request = new AuthRequest.RegisterRequest();
        request.setPhoneNumber("+233241234567");
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void resendOtp_success() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("+233241234567")
                .isActive(true)
                .build();

        AuthRequest.ResendOtpRequest request = new AuthRequest.ResendOtpRequest();
        request.setPhoneNumber("+233241234567");

        when(userRepository.findByPhoneNumberAndDeletedAtIsNull("+233241234567")).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findByPhoneNumber("+233241234567")).thenReturn(Optional.empty());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        AuthResponse.PendingAuthResponse response = authService.resendOtp(request);

        assertThat(response.getPhoneNumber()).isEqualTo("+233241234567");
        assertThat(response.getOtpCode()).isNotNull();
        verify(smsService).send(any());
        verify(otpVerificationRepository).save(any());
    }

    @Test
    void resendOtp_userNotFound_throwsException() {
        AuthRequest.ResendOtpRequest request = new AuthRequest.ResendOtpRequest();
        request.setPhoneNumber("+233241234567");

        when(userRepository.findByPhoneNumberAndDeletedAtIsNull("+233241234567")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resendOtp(request))
                .isInstanceOf(com.makershub.exception.ResourceNotFoundException.class);
    }
}
