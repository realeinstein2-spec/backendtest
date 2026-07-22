package com.makershub.service;

import com.makershub.dto.request.FactoryRequest;
import com.makershub.dto.request.UserRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.entity.Factory;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.UserRole;
import com.makershub.enums.VerificationStatus;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.FactoryRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FactoryRepository factoryRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_returnsUserSummaryResponse() {
        User user = createUser(UserRole.SME_OWNER);
        authenticate(user);

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));

        AuthResponse.UserSummaryResponse response = userService.getCurrentUser();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getFullName()).isEqualTo(user.getFullName());
        assertThat(response.getPhoneNumber()).isEqualTo(user.getPhoneNumber());
        assertThat(response.getRole()).isEqualTo(user.getRole());
    }

    @Test
    void getCurrentUser_throwsResourceNotFoundException_whenUserNotFound() {
        User user = createUser(UserRole.SME_OWNER);
        authenticate(user);

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void createFactoryProfile_success() {
        User user = createUser(UserRole.FACTORY_OWNER);
        authenticate(user);

        FactoryRequest.CreateFactoryProfileRequest request = new FactoryRequest.CreateFactoryProfileRequest();
        request.setCompanyName("Accra Carpentry");
        request.setDescription("Fine wood works");
        request.setSectorTags(List.of("woodwork", "furniture"));
        request.setMachineryList("CNC router, Table saw");
        request.setMinOrderQuantity(10);
        request.setMaxOrderQuantity(1000);
        request.setLatitude(5.6037);
        request.setLongitude(-0.1870);
        request.setAddress("12 Ring Road, Accra");

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(factoryRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        Factory mockFactory = Factory.builder()
                .id(UUID.randomUUID())
                .build();
        when(factoryRepository.save(any(Factory.class))).thenReturn(mockFactory);

        AuthResponse.UserSummaryResponse response = userService.createFactoryProfile(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(user.getId());

        verify(factoryRepository).save(any(Factory.class));
        verify(auditLogger).log(eq(AuditAction.CREATE), eq("FACTORY"), eq(mockFactory.getId()), any(), any());
    }

    @Test
    void createFactoryProfile_throwsUnauthorizedException_whenNotFactoryOwner() {
        User user = createUser(UserRole.SME_OWNER);
        authenticate(user);

        FactoryRequest.CreateFactoryProfileRequest request = new FactoryRequest.CreateFactoryProfileRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.createFactoryProfile(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only factory owners");

        verifyNoInteractions(factoryRepository);
        verify(auditLogger, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void updateFcmToken_success() {
        User user = createUser(UserRole.SME_OWNER);
        authenticate(user);

        UserRequest.UpdateFcmTokenRequest request = new UserRequest.UpdateFcmTokenRequest();
        request.setFcmToken("fcm-device-token-12345");

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateFcmToken(request);

        assertThat(user.getFcmToken()).isEqualTo("fcm-device-token-12345");
        verify(userRepository).save(user);
        verify(auditLogger).log(eq(AuditAction.UPDATE), eq("USER"), eq(user.getId()), eq("FCM_TOKEN_REGISTERED"), any());
    }

    @Test
    void createFactoryProfile_throwsBusinessException_whenFactoryAlreadyExists() {
        User user = createUser(UserRole.FACTORY_OWNER);
        authenticate(user);

        FactoryRequest.CreateFactoryProfileRequest request = new FactoryRequest.CreateFactoryProfileRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(factoryRepository.findByUserId(user.getId())).thenReturn(Optional.of(new Factory()));

        assertThatThrownBy(() -> userService.createFactoryProfile(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Factory profile already exists");

        verify(factoryRepository, never()).save(any(Factory.class));
        verifyNoInteractions(auditLogger);
    }

    @Test
    void deleteCurrentUser_success_noFactory() {
        User user = createUser(UserRole.SME_OWNER);
        authenticate(user);

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteCurrentUser();

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
        verify(auditLogger).log(eq(AuditAction.DELETE), eq("USER"), eq(user.getId()), eq("SELF_SOFT_DELETE"), any());
    }

    @Test
    void deleteCurrentUser_success_withFactory() {
        User user = createUser(UserRole.FACTORY_OWNER);
        Factory factory = Factory.builder().id(UUID.randomUUID()).build();
        user.setFactory(factory);
        authenticate(user);

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteCurrentUser();

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getIsActive()).isFalse();
        assertThat(factory.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(auditLogger).log(eq(AuditAction.DELETE), eq("USER"), eq(user.getId()), eq("SELF_SOFT_DELETE"), any());
    }

    private User createUser(UserRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("+233241234567")
                .fullName("Kwame Appiah")
                .role(role)
                .isVerified(true)
                .isActive(true)
                .passwordHash("password_hash")
                .build();
    }

    private void authenticate(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
