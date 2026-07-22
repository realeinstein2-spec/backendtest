package com.makershub.service;

import com.makershub.entity.Factory;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.UserRole;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.FactoryRepository;
import com.makershub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FactoryRepository factoryRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdminService adminService;

    @Test
    void deleteUser_success_noFactory() {
        User user = createUser(UserRole.SME_OWNER);
        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.deleteUser(user.getId());

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
        verify(auditLogger).log(eq(AuditAction.DELETE), eq("USER"), eq(user.getId()), eq("ADMIN_SOFT_DELETE"), any());
    }

    @Test
    void deleteUser_success_withFactory() {
        User user = createUser(UserRole.FACTORY_OWNER);
        Factory factory = Factory.builder().id(UUID.randomUUID()).build();
        user.setFactory(factory);

        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.deleteUser(user.getId());

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getIsActive()).isFalse();
        assertThat(factory.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(auditLogger).log(eq(AuditAction.DELETE), eq("USER"), eq(user.getId()), eq("ADMIN_SOFT_DELETE"), any());
    }

    @Test
    void deleteUser_notFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogger);
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
}
