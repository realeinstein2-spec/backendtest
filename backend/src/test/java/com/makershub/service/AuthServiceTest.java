package com.makershub.service;

import com.makershub.dto.request.AuthRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.entity.User;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.UserRepository;
import com.makershub.repository.RefreshTokenRepository;
import com.makershub.entity.RefreshToken;
import com.makershub.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;

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
    @Mock
    private com.makershub.security.FirebaseTokenVerifier firebaseTokenVerifier;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

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

    @Test
    void socialLogin_google_success() throws Exception {
        AuthRequest.SocialLoginRequest request = new AuthRequest.SocialLoginRequest();
        request.setIdToken("mock-firebase-id-token");
        request.setRole(UserRole.SME_OWNER);
        request.setFullName("Kofi Mensah");
        request.setPhoneNumber("+233241234567");

        com.google.firebase.auth.FirebaseToken mockDecodedToken = mock(com.google.firebase.auth.FirebaseToken.class);
        when(mockDecodedToken.getEmail()).thenReturn("kofi@example.com");

        when(firebaseTokenVerifier.verifyIdToken("mock-firebase-id-token")).thenReturn(mockDecodedToken);
        when(userRepository.findByEmailAndDeletedAtIsNull("kofi@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhoneNumber("+233241234567")).thenReturn(false);

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("kofi@example.com")
                .fullName("Kofi Mensah")
                .role(UserRole.SME_OWNER)
                .isActive(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateAccessToken(any(com.makershub.security.UserDetailsImpl.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(com.makershub.security.UserDetailsImpl.class))).thenReturn("refresh-token");

        AuthResponse.TokenResponse response = authService.socialLogin(request, "google");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(firebaseTokenVerifier).verifyIdToken("mock-firebase-id-token");
    }

    @Test
    void socialLogin_google_existingUser_success() throws Exception {
        AuthRequest.SocialLoginRequest request = new AuthRequest.SocialLoginRequest();
        request.setIdToken("mock-firebase-id-token");
        request.setRole(UserRole.SME_OWNER);

        com.google.firebase.auth.FirebaseToken mockDecodedToken = mock(com.google.firebase.auth.FirebaseToken.class);
        when(mockDecodedToken.getEmail()).thenReturn("kofi@example.com");

        when(firebaseTokenVerifier.verifyIdToken("mock-firebase-id-token")).thenReturn(mockDecodedToken);

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("kofi@example.com")
                .fullName("Kofi Mensah")
                .role(UserRole.SME_OWNER)
                .isActive(true)
                .build();
        when(userRepository.findByEmailAndDeletedAtIsNull("kofi@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateAccessToken(any(com.makershub.security.UserDetailsImpl.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(com.makershub.security.UserDetailsImpl.class))).thenReturn("refresh-token");

        AuthResponse.TokenResponse response = authService.socialLogin(request, "google");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(firebaseTokenVerifier).verifyIdToken("mock-firebase-id-token");
    }

    @Test
    void socialLogin_google_verificationFailed_throwsException() throws Exception {
        AuthRequest.SocialLoginRequest request = new AuthRequest.SocialLoginRequest();
        request.setIdToken("invalid-token");
        request.setRole(UserRole.SME_OWNER);

        when(firebaseTokenVerifier.verifyIdToken("invalid-token")).thenThrow(new RuntimeException("Token expired"));

        assertThatThrownBy(() -> authService.socialLogin(request, "google"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Firebase verification failed");
    }

    @Test
    void refresh_success_withWhitelistAndRotation() {
        AuthRequest.RefreshRequest request = new AuthRequest.RefreshRequest();
        request.setRefreshToken("valid-refresh-token");

        User user = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("+233241234567")
                .role(UserRole.SME_OWNER)
                .isActive(true)
                .isVerified(true)
                .build();

        RefreshToken dbToken = RefreshToken.builder()
                .token("valid-refresh-token")
                .user(user)
                .expiryTime(Instant.now().plusSeconds(3600))
                .build();

        when(jwtUtil.isTokenValid("valid-refresh-token", "REFRESH")).thenReturn(true);
        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(dbToken));
        when(jwtUtil.extractUserId("valid-refresh-token")).thenReturn(user.getId());
        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));

        when(jwtUtil.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtUtil.getAccessExpiry()).thenReturn(Instant.now().plusSeconds(1800));
        when(jwtUtil.getRefreshExpiry()).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse.TokenResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).delete(dbToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_fails_whenTokenNotInWhitelist() {
        AuthRequest.RefreshRequest request = new AuthRequest.RefreshRequest();
        request.setRefreshToken("unknown-refresh-token");

        when(jwtUtil.isTokenValid("unknown-refresh-token", "REFRESH")).thenReturn(true);
        when(refreshTokenRepository.findByToken("unknown-refresh-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid or logged out refresh token");
    }

    @Test
    void refresh_fails_whenTokenExpiredInDb() {
        AuthRequest.RefreshRequest request = new AuthRequest.RefreshRequest();
        request.setRefreshToken("expired-refresh-token");

        RefreshToken dbToken = RefreshToken.builder()
                .token("expired-refresh-token")
                .expiryTime(Instant.now().minusSeconds(10))
                .build();

        when(jwtUtil.isTokenValid("expired-refresh-token", "REFRESH")).thenReturn(true);
        when(refreshTokenRepository.findByToken("expired-refresh-token")).thenReturn(Optional.of(dbToken));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh token has expired");
        verify(refreshTokenRepository).delete(dbToken);
    }

    @Test
    void logout_deletesToken() {
        AuthRequest.RefreshRequest request = new AuthRequest.RefreshRequest();
        request.setRefreshToken("some-refresh-token");

        authService.logout(request);

        verify(refreshTokenRepository).deleteByToken("some-refresh-token");
    }
}
