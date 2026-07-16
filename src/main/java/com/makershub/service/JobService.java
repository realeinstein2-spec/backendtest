package com.makershub.service;

import com.makershub.dto.request.JobRequest;
import com.makershub.dto.response.JobResponse;
import com.makershub.entity.Factory;
import com.makershub.entity.JobListing;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.JobStatus;
import com.makershub.enums.NotificationType;
import com.makershub.enums.UserRole;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobListingRepository jobRepository;
    private final UserRepository userRepository;
    private final FactoryRepository factoryRepository;
    private final DtoMapper mapper;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    @Transactional
    public JobResponse.JobDetailResponse createJob(JobRequest.CreateJobRequest request) {
        User sme = getAuthenticatedUser();
        if (sme.getRole() != UserRole.SME_OWNER && sme.getRole() != UserRole.ENTERPRISE) {
            throw new UnauthorizedException("Only SMEs can post jobs");
        }
        // M-15: Validate deadline is in the future
        if (request.getDeadline() != null && request.getDeadline().isBefore(java.time.LocalDate.now())) {
            throw new com.makershub.exception.BusinessException("Job deadline must be in the future",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_DEADLINE");
        }
        JobListing job = JobListing.builder()
                .sme(sme)
                .title(request.getTitle())
                .productType(request.getProductType())
                .sectorTag(request.getSectorTag())
                .quantity(request.getQuantity())
                .specifications(request.getSpecifications())
                .budgetMinGhs(request.getBudgetMinGhs())
                .budgetMaxGhs(request.getBudgetMaxGhs())
                .deadline(request.getDeadline())
                .deliveryAddress(request.getDeliveryAddress())
                .attachmentUrls(request.getAttachmentUrls())
                .status(JobStatus.OPEN)
                .build();
        JobListing saved = jobRepository.save(job);
        auditLogger.log(AuditAction.CREATE, "JOB", saved.getId(), null, null);
        notifyMatchingFactories(saved);
        return mapper.toJobResponse(saved);
    }

    // C-12: Pass parsed budget values instead of null
    @Transactional(readOnly = true)
    public Page<JobResponse.JobDetailResponse> listOpenJobs(String sectorTag, String minBudget, String maxBudget, Pageable pageable) {
        java.math.BigDecimal minBud = minBudget != null && !minBudget.isBlank() ? new java.math.BigDecimal(minBudget) : null;
        java.math.BigDecimal maxBud = maxBudget != null && !maxBudget.isBlank() ? new java.math.BigDecimal(maxBudget) : null;
        return jobRepository.findOpenJobs(sectorTag, minBud, maxBud, pageable).map(mapper::toJobResponse);
    }

    @Transactional(readOnly = true)
    public JobResponse.JobDetailResponse getJob(UUID jobId) {
        JobListing job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId.toString()));
        return mapper.toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<Factory> findMatchingFactories(JobListing job) {
        // Default to Kumasi coordinates if no delivery address coordinates available.
        return factoryRepository.findMatchingFactories(
                job.getSectorTag(), job.getQuantity(), 6.6885, -1.6244, 50000.0);
    }

    // M-2: Run factory notifications asynchronously so job creation is not blocked
    @Async
    private void notifyMatchingFactories(JobListing job) {
        findMatchingFactories(job).forEach(factory ->
            notificationService.sendNotification(NotificationEvent.builder()
                    .recipientId(factory.getUser().getId())
                    .phoneNumber(factory.getUser().getPhoneNumber())
                    .type(NotificationType.ORDER_STATUS_UPDATE)
                    .title("New job match")
                    .body("A new " + job.getSectorTag() + " job is available for bidding.")
                    .data(Map.of("jobId", job.getId().toString()))
                    .build())
        );
    }

    @Transactional(readOnly = true)
    public Page<JobResponse.JobDetailResponse> listMyJobs(Pageable pageable) {
        User sme = getAuthenticatedUser();
        return jobRepository.findBySmeIdAndDeletedAtIsNullOrderByCreatedAtDesc(sme.getId(), pageable)
                .map(mapper::toJobResponse);
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
