package com.makershub.service;

import com.makershub.dto.request.BidRequest;
import com.makershub.dto.response.BidResponse;
import com.makershub.entity.Bid;
import com.makershub.entity.Factory;
import com.makershub.entity.JobListing;
import com.makershub.entity.User;
import com.makershub.enums.*;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.mapper.DtoMapper;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.BidRepository;
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
import org.springframework.http.HttpStatus;
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
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private JobListingRepository jobRepository;

    @Mock
    private FactoryRepository factoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DtoMapper mapper;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BidService bidService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitBid_success() {
        User factoryUser = createUser(UserRole.FACTORY_OWNER);
        authenticate(factoryUser);

        Factory factory = Factory.builder()
                .id(UUID.randomUUID())
                .user(factoryUser)
                .companyName("Accra Forge")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        User sme = createUser(UserRole.SME_OWNER);
        JobListing job = JobListing.builder()
                .id(UUID.randomUUID())
                .sme(sme)
                .title("Metal Brackets")
                .status(JobStatus.OPEN)
                .build();

        BidRequest.CreateBidRequest request = createBidRequest(job.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factory));
        when(jobRepository.findByIdAndDeletedAtIsNull(job.getId())).thenReturn(Optional.of(job));
        when(bidRepository.existsByJobListingIdAndFactoryIdAndDeletedAtIsNull(job.getId(), factory.getId())).thenReturn(false);

        Bid savedBid = Bid.builder().id(UUID.randomUUID()).build();
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

        BidResponse.BidDetailResponse expectedResponse = BidResponse.BidDetailResponse.builder().build();
        when(mapper.toBidResponse(any(Bid.class))).thenReturn(expectedResponse);

        BidResponse.BidDetailResponse response = bidService.submitBid(request);

        assertThat(response).isNotNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.BIDDING);

        verify(bidRepository).save(any(Bid.class));
        verify(jobRepository).save(job);
        verify(auditLogger).log(eq(AuditAction.CREATE), eq("BID"), eq(savedBid.getId()), any(), any());
        verify(notificationService).sendNotification(any(NotificationEvent.class));
    }

    @Test
    void submitBid_throwsUnauthorizedException_whenNotFactoryOwner() {
        User smeUser = createUser(UserRole.SME_OWNER);
        authenticate(smeUser);

        BidRequest.CreateBidRequest request = createBidRequest(UUID.randomUUID());

        when(userRepository.findByIdAndDeletedAtIsNull(smeUser.getId())).thenReturn(Optional.of(smeUser));

        assertThatThrownBy(() -> bidService.submitBid(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only factory owners");

        verifyNoInteractions(bidRepository, factoryRepository, jobRepository, auditLogger, notificationService);
    }

    @Test
    void submitBid_throwsBusinessException_whenFactoryProfileMissing() {
        User factoryUser = createUser(UserRole.FACTORY_OWNER);
        authenticate(factoryUser);

        BidRequest.CreateBidRequest request = createBidRequest(UUID.randomUUID());

        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.submitBid(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Factory profile required");

        verifyNoInteractions(bidRepository, jobRepository, auditLogger, notificationService);
    }

    @Test
    void submitBid_throwsUnauthorizedException_whenFactoryNotVerified() {
        User factoryUser = createUser(UserRole.FACTORY_OWNER);
        authenticate(factoryUser);

        Factory factory = Factory.builder()
                .id(UUID.randomUUID())
                .user(factoryUser)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        BidRequest.CreateBidRequest request = createBidRequest(UUID.randomUUID());

        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factory));

        assertThatThrownBy(() -> bidService.submitBid(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Factory must be verified");

        verifyNoInteractions(bidRepository, jobRepository, auditLogger, notificationService);
    }

    @Test
    void submitBid_throwsResourceNotFoundException_whenJobNotFound() {
        User factoryUser = createUser(UserRole.FACTORY_OWNER);
        authenticate(factoryUser);

        Factory factory = Factory.builder()
                .id(UUID.randomUUID())
                .user(factoryUser)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        UUID jobId = UUID.randomUUID();
        BidRequest.CreateBidRequest request = createBidRequest(jobId);

        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factory));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.submitBid(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(jobId.toString());

        verifyNoInteractions(bidRepository, auditLogger, notificationService);
    }

    @Test
    void submitBid_throwsBusinessException_whenJobNotOpen() {
        User factoryUser = createUser(UserRole.FACTORY_OWNER);
        authenticate(factoryUser);

        Factory factory = Factory.builder()
                .id(UUID.randomUUID())
                .user(factoryUser)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        JobListing job = JobListing.builder()
                .id(UUID.randomUUID())
                .status(JobStatus.COMPLETED)
                .build();

        BidRequest.CreateBidRequest request = createBidRequest(job.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factory));
        when(jobRepository.findByIdAndDeletedAtIsNull(job.getId())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> bidService.submitBid(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Job is not open");

        verifyNoInteractions(bidRepository, auditLogger, notificationService);
    }

    @Test
    void submitBid_throwsBusinessException_whenBidAlreadySubmitted() {
        User factoryUser = createUser(UserRole.FACTORY_OWNER);
        authenticate(factoryUser);

        Factory factory = Factory.builder()
                .id(UUID.randomUUID())
                .user(factoryUser)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        JobListing job = JobListing.builder()
                .id(UUID.randomUUID())
                .status(JobStatus.OPEN)
                .build();

        BidRequest.CreateBidRequest request = createBidRequest(job.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factory));
        when(jobRepository.findByIdAndDeletedAtIsNull(job.getId())).thenReturn(Optional.of(job));
        when(bidRepository.existsByJobListingIdAndFactoryIdAndDeletedAtIsNull(job.getId(), factory.getId())).thenReturn(true);

        assertThatThrownBy(() -> bidService.submitBid(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bid already submitted");

        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    void listBidsForJob_returnsBids() {
        UUID jobId = UUID.randomUUID();
        Bid bid = Bid.builder().id(UUID.randomUUID()).build();
        when(bidRepository.findByJobListingIdAndDeletedAtIsNullOrderByTotalPriceGhsAsc(jobId)).thenReturn(List.of(bid));

        BidResponse.BidDetailResponse responseDetail = BidResponse.BidDetailResponse.builder().build();
        when(mapper.toBidResponse(bid)).thenReturn(responseDetail);

        List<BidResponse.BidDetailResponse> result = bidService.listBidsForJob(jobId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(responseDetail);
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

    private BidRequest.CreateBidRequest createBidRequest(UUID jobId) {
        BidRequest.CreateBidRequest request = new BidRequest.CreateBidRequest();
        request.setJobId(jobId.toString());
        request.setPricePerUnitGhs(BigDecimal.valueOf(10));
        request.setTotalPriceGhs(BigDecimal.valueOf(1000));
        request.setProductionDays(5);
        request.setDeliveryDateEstimate(LocalDate.now().plusDays(10));
        request.setMessage("I can deliver high quality products fast.");
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
