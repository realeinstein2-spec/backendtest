package com.makershub.service;

import com.makershub.dto.request.BidRequest;
import com.makershub.dto.response.BidResponse;
import com.makershub.entity.Bid;
import com.makershub.entity.Factory;
import com.makershub.entity.JobListing;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.BidStatus;
import com.makershub.enums.JobStatus;
import com.makershub.enums.NotificationType;
import com.makershub.enums.UserRole;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final JobListingRepository jobRepository;
    private final FactoryRepository factoryRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    @Transactional
    public BidResponse.BidDetailResponse submitBid(BidRequest.CreateBidRequest request) {
        User factoryUser = getAuthenticatedUser();
        if (factoryUser.getRole() != UserRole.FACTORY_OWNER) {
            throw new UnauthorizedException("Only factory owners can submit bids");
        }
        Factory factory = factoryRepository.findByUserIdAndDeletedAtIsNull(factoryUser.getId())
                .orElseThrow(() -> new BusinessException("Factory profile required", HttpStatus.PRECONDITION_FAILED, "FACTORY_PROFILE_MISSING"));
        if (factory.getVerificationStatus() != com.makershub.enums.VerificationStatus.VERIFIED) {
            throw new UnauthorizedException("Factory must be verified to bid");
        }
        UUID jobId = UUID.fromString(request.getJobId());
        JobListing job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", request.getJobId()));
        if (job.getStatus() != JobStatus.OPEN && job.getStatus() != JobStatus.BIDDING) {
            throw new BusinessException("Job is not open for bidding", HttpStatus.CONFLICT, "JOB_NOT_OPEN");
        }
        if (bidRepository.existsByJobListingIdAndFactoryIdAndDeletedAtIsNull(jobId, factory.getId())) {
            throw new BusinessException("Bid already submitted for this job", HttpStatus.CONFLICT, "BID_EXISTS");
        }
        Bid bid = Bid.builder()
                .jobListing(job)
                .factory(factory)
                .pricePerUnitGhs(request.getPricePerUnitGhs())
                .totalPriceGhs(request.getTotalPriceGhs())
                .productionDays(request.getProductionDays())
                .deliveryDateEstimate(request.getDeliveryDateEstimate())
                .message(request.getMessage())
                .status(BidStatus.PENDING)
                .build();
        Bid saved = bidRepository.save(bid);
        job.setStatus(JobStatus.BIDDING);
        jobRepository.save(job);
        auditLogger.log(AuditAction.CREATE, "BID", saved.getId(), null, null);
        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(job.getSme().getId())
                .phoneNumber(job.getSme().getPhoneNumber())
                .type(NotificationType.BID_RECEIVED)
                .title("New bid received")
                .body(factory.getCompanyName() + " submitted a bid for " + job.getTitle())
                .data(Map.of("jobId", jobId.toString(), "bidId", saved.getId().toString()))
                .build());
        return mapper.toBidResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BidResponse.BidDetailResponse> listBidsForJob(UUID jobId) {
        return bidRepository.findByJobListingIdAndDeletedAtIsNullOrderByTotalPriceGhsAsc(jobId).stream()
                .map(mapper::toBidResponse)
                .toList();
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
