package com.makershub.service;

import com.makershub.audit.AuditLogger;
import com.makershub.dto.request.SupportTicketRequest;
import com.makershub.dto.response.SupportTicketResponse;
import com.makershub.entity.SupportMessage;
import com.makershub.entity.SupportTicket;
import com.makershub.entity.User;
import com.makershub.enums.*;
import com.makershub.exception.UnauthorizedException;
import com.makershub.notification.NotificationService;
import com.makershub.repository.SupportMessageRepository;
import com.makershub.repository.SupportTicketRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private SupportMessageRepository supportMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private SupportTicketService supportTicketService;

    private User testUser;
    private User adminUser;
    private SupportTicket testTicket;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Kofi Mensah")
                .email("kofi@sme.com")
                .phoneNumber("+233240000000")
                .role(UserRole.SME_OWNER)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Admin User")
                .email("admin@makershub.gh")
                .phoneNumber("+233249999999")
                .role(UserRole.ADMIN)
                .build();

        testTicket = SupportTicket.builder()
                .id(UUID.randomUUID())
                .ticketNumber("SUP-20260722-1234")
                .user(testUser)
                .subject("Payment Issue")
                .description("Escrow not released")
                .category(SupportTicketCategory.PAYMENT)
                .priority(SupportTicketPriority.HIGH)
                .status(SupportTicketStatus.OPEN)
                .targetSlaDeadline(Instant.now().plus(Duration.ofHours(12)))
                .createdAt(Instant.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicket_success_sets12HourSla() {
        authenticate(testUser);

        SupportTicketRequest.CreateTicketRequest request = new SupportTicketRequest.CreateTicketRequest();
        request.setSubject("Payment Issue");
        request.setDescription("Escrow not released");
        request.setCategory(SupportTicketCategory.PAYMENT);
        request.setPriority(SupportTicketPriority.HIGH);

        when(userRepository.findByIdAndDeletedAtIsNull(testUser.getId())).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

        SupportTicketResponse.SupportTicketDetailResponse response = supportTicketService.createTicket(request);

        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("Payment Issue");
        assertThat(response.getCategory()).isEqualTo(SupportTicketCategory.PAYMENT);
        verify(supportTicketRepository).save(any(SupportTicket.class));
        verify(supportMessageRepository).save(any(SupportMessage.class));
        verify(notificationService).sendNotification(any());
    }

    @Test
    void getUserTickets_returnsUserTicketPage() {
        authenticate(testUser);
        Pageable pageable = PageRequest.of(0, 10);
        Page<SupportTicket> page = new PageImpl<>(List.of(testTicket));

        when(userRepository.findByIdAndDeletedAtIsNull(testUser.getId())).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findByUserIdAndDeletedAtIsNull(testUser.getId(), pageable)).thenReturn(page);

        Page<SupportTicketResponse.SupportTicketSummaryResponse> result = supportTicketService.getUserTickets(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTicketNumber()).isEqualTo("SUP-20260722-1234");
    }

    @Test
    void getTicketDetails_success_owner() {
        authenticate(testUser);

        when(userRepository.findByIdAndDeletedAtIsNull(testUser.getId())).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findByIdAndDeletedAtIsNull(testTicket.getId())).thenReturn(Optional.of(testTicket));

        SupportTicketResponse.SupportTicketDetailResponse response = supportTicketService.getTicketDetails(testTicket.getId());

        assertThat(response).isNotNull();
        assertThat(response.getTicketNumber()).isEqualTo("SUP-20260722-1234");
    }

    @Test
    void getTicketDetails_throwsUnauthorized_whenOtherUser() {
        User otherUser = User.builder().id(UUID.randomUUID()).role(UserRole.SME_OWNER).build();
        authenticate(otherUser);

        when(userRepository.findByIdAndDeletedAtIsNull(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(supportTicketRepository.findByIdAndDeletedAtIsNull(testTicket.getId())).thenReturn(Optional.of(testTicket));

        assertThatThrownBy(() -> supportTicketService.getTicketDetails(testTicket.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void addMessage_success_adminReplies() {
        authenticate(adminUser);

        SupportTicketRequest.AddMessageRequest request = new SupportTicketRequest.AddMessageRequest();
        request.setMessage("We are inspecting your payment now");

        when(userRepository.findByIdAndDeletedAtIsNull(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(supportTicketRepository.findByIdAndDeletedAtIsNull(testTicket.getId())).thenReturn(Optional.of(testTicket));
        when(supportMessageRepository.save(any(SupportMessage.class))).thenAnswer(i -> i.getArgument(0));

        SupportTicketResponse.SupportMessageDetailResponse response = supportTicketService.addMessage(testTicket.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("We are inspecting your payment now");
        assertThat(testTicket.getStatus()).isEqualTo(SupportTicketStatus.IN_PROGRESS);
        assertThat(testTicket.getAssignedAdmin()).isEqualTo(adminUser);
        verify(notificationService).sendNotification(any());
    }

    @Test
    void addMessage_throwsBusinessException_whenTicketClosedOrResolved() {
        authenticate(testUser);
        testTicket.setStatus(SupportTicketStatus.CLOSED);

        SupportTicketRequest.AddMessageRequest request = new SupportTicketRequest.AddMessageRequest();
        request.setMessage("Trying to reply to closed ticket");

        when(userRepository.findByIdAndDeletedAtIsNull(testUser.getId())).thenReturn(Optional.of(testUser));
        when(supportTicketRepository.findByIdAndDeletedAtIsNull(testTicket.getId())).thenReturn(Optional.of(testTicket));

        assertThatThrownBy(() -> supportTicketService.addMessage(testTicket.getId(), request))
                .isInstanceOf(com.makershub.exception.BusinessException.class)
                .hasMessageContaining("Cannot add a message to a resolved or closed ticket");
    }

    @Test
    void adminUpdateTicketStatus_success() {
        authenticate(adminUser);

        SupportTicketRequest.UpdateTicketStatusRequest request = new SupportTicketRequest.UpdateTicketStatusRequest();
        request.setStatus(SupportTicketStatus.RESOLVED);
        request.setAdminNotes("Refund approved and processed");

        when(userRepository.findByIdAndDeletedAtIsNull(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(supportTicketRepository.findByIdAndDeletedAtIsNull(testTicket.getId())).thenReturn(Optional.of(testTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

        SupportTicketResponse.SupportTicketDetailResponse response = supportTicketService.adminUpdateTicketStatus(testTicket.getId(), request);

        assertThat(response).isNotNull();
        assertThat(testTicket.getStatus()).isEqualTo(SupportTicketStatus.RESOLVED);
        assertThat(testTicket.getAdminNotes()).isEqualTo("Refund approved and processed");
        assertThat(testTicket.getResolvedAt()).isNotNull();
    }

    @Test
    void adminGetStats_returnsStats() {
        when(supportTicketRepository.countByStatusAndDeletedAtIsNull(SupportTicketStatus.OPEN)).thenReturn(5L);
        when(supportTicketRepository.countByStatusAndDeletedAtIsNull(SupportTicketStatus.IN_PROGRESS)).thenReturn(2L);
        when(supportTicketRepository.countBreachedSla(any(), any())).thenReturn(1L);

        SupportTicketResponse.SupportTicketStatsResponse stats = supportTicketService.adminGetStats();

        assertThat(stats).isNotNull();
        assertThat(stats.getOpenTicketsCount()).isEqualTo(5L);
        assertThat(stats.getInProgressTicketsCount()).isEqualTo(2L);
        assertThat(stats.getTotalBreachedSlaCount()).isEqualTo(1L);
        assertThat(stats.getSlaWindowHours()).isEqualTo(12);
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
