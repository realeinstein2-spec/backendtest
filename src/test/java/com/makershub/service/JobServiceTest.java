package com.makershub.service;

import com.makershub.dto.request.JobRequest;
import com.makershub.dto.response.JobResponse;
import com.makershub.entity.Factory;
import com.makershub.entity.JobListing;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.JobStatus;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.mapper.DtoMapper;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.FactoryRepository;
import com.makershub.repository.JobListingRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobListingRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FactoryRepository factoryRepository;

    @Mock
    private DtoMapper mapper;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JobNotificationService jobNotificationService;

    @InjectMocks
    private JobService jobService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createJob_success_smeOwner() {
        User sme = createUser(UserRole.SME_OWNER);
        authenticate(sme);

        JobRequest.CreateJobRequest request = createJobRequest();
        JobListing job = JobListing.builder().id(UUID.randomUUID()).sme(sme).status(JobStatus.OPEN).build();

        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));
        when(jobRepository.save(any(JobListing.class))).thenReturn(job);

        JobResponse.JobDetailResponse expectedResponse = JobResponse.JobDetailResponse.builder().build();
        when(mapper.toJobResponse(job)).thenReturn(expectedResponse);

        JobResponse.JobDetailResponse response = jobService.createJob(request);

        assertThat(response).isNotNull();
        verify(jobRepository).save(any(JobListing.class));
        verify(auditLogger).log(eq(AuditAction.CREATE), eq("JOB"), eq(job.getId()), any(), any());
        verify(jobNotificationService).notifyMatchingFactories(any(JobListing.class));
    }

    @Test
    void createJob_success_enterprise() {
        User enterprise = createUser(UserRole.ENTERPRISE);
        authenticate(enterprise);

        JobRequest.CreateJobRequest request = createJobRequest();
        JobListing job = JobListing.builder().id(UUID.randomUUID()).sme(enterprise).status(JobStatus.OPEN).build();

        when(userRepository.findByIdAndDeletedAtIsNull(enterprise.getId())).thenReturn(Optional.of(enterprise));
        when(jobRepository.save(any(JobListing.class))).thenReturn(job);

        JobResponse.JobDetailResponse expectedResponse = JobResponse.JobDetailResponse.builder().build();
        when(mapper.toJobResponse(job)).thenReturn(expectedResponse);

        JobResponse.JobDetailResponse response = jobService.createJob(request);

        assertThat(response).isNotNull();
        verify(jobRepository).save(any(JobListing.class));
        verify(jobNotificationService).notifyMatchingFactories(any(JobListing.class));
    }

    @Test
    void createJob_throwsUnauthorizedException_whenNotSmeOrEnterprise() {
        User factoryUser = createUser(UserRole.FACTORY_OWNER);
        authenticate(factoryUser);

        JobRequest.CreateJobRequest request = createJobRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only SMEs can post jobs");

        verifyNoInteractions(jobRepository, auditLogger, notificationService);
    }

    @Test
    void createJob_throwsBusinessException_whenDeadlineInPast() {
        User sme = createUser(UserRole.SME_OWNER);
        authenticate(sme);

        JobRequest.CreateJobRequest request = createJobRequest();
        request.setDeadline(LocalDate.now().minusDays(1));

        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Job deadline must be in the future");

        verifyNoInteractions(jobRepository, auditLogger, notificationService);
    }

    @Test
    void listOpenJobs_success() {
        Pageable pageable = PageRequest.of(0, 10);
        JobListing job = JobListing.builder().id(UUID.randomUUID()).build();
        Page<JobListing> page = new PageImpl<>(List.of(job));

        when(jobRepository.findOpenJobs(eq("wood"), any(BigDecimal.class), any(BigDecimal.class), eq(pageable)))
                .thenReturn(page);

        JobResponse.JobDetailResponse responseDetail = JobResponse.JobDetailResponse.builder().build();
        when(mapper.toJobResponse(job)).thenReturn(responseDetail);

        Page<JobResponse.JobDetailResponse> result = jobService.listOpenJobs("wood", "100.00", "5000.00", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getJob_success() {
        UUID jobId = UUID.randomUUID();
        JobListing job = JobListing.builder().id(jobId).build();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));

        JobResponse.JobDetailResponse expectedResponse = JobResponse.JobDetailResponse.builder().build();
        when(mapper.toJobResponse(job)).thenReturn(expectedResponse);

        JobResponse.JobDetailResponse response = jobService.getJob(jobId);

        assertThat(response).isNotNull();
    }

    @Test
    void getJob_throwsResourceNotFoundException_whenNotFound() {
        UUID jobId = UUID.randomUUID();

        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob(jobId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(jobId.toString());
    }

    private User createUser(UserRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("+233241234567")
                .fullName("Test User")
                .role(role)
                .isVerified(true)
                .isActive(true)
                .passwordHash("password_hash")
                .build();
    }

    private JobRequest.CreateJobRequest createJobRequest() {
        JobRequest.CreateJobRequest request = new JobRequest.CreateJobRequest();
        request.setTitle("Wooden Chairs");
        request.setProductType("Furniture");
        request.setSectorTag("wood");
        request.setQuantity(50);
        request.setSpecifications("High quality mahogany wood chairs.");
        request.setBudgetMinGhs(BigDecimal.valueOf(100));
        request.setBudgetMaxGhs(BigDecimal.valueOf(150));
        request.setDeadline(LocalDate.now().plusDays(30));
        request.setDeliveryAddress("Accra High Street");
        return request;
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
