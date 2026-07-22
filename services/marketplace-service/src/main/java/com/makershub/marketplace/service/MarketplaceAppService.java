package com.makershub.marketplace.service;

import com.makershub.marketplace.dto.BidDto;
import com.makershub.marketplace.dto.JobDto;
import com.makershub.marketplace.entity.Bid;
import com.makershub.marketplace.entity.JobListing;
import com.makershub.marketplace.enums.BidStatus;
import com.makershub.marketplace.enums.JobStatus;
import com.makershub.marketplace.repository.BidRepository;
import com.makershub.marketplace.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceAppService {

    private final JobListingRepository jobListingRepository;
    private final BidRepository bidRepository;

    @Transactional(readOnly = true)
    public Page<JobDto.JobDetailResponse> getOpenJobs(Pageable pageable) {
        return jobListingRepository.findByStatus(JobStatus.OPEN, pageable)
                .map(this::toJobResponse);
    }

    @Transactional(readOnly = true)
    public JobDto.JobDetailResponse getJobById(UUID jobId) {
        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        return toJobResponse(job);
    }

    @Transactional
    public JobDto.JobDetailResponse createJob(UUID smeOwnerId, JobDto.CreateJobRequest request) {
        JobListing job = JobListing.builder()
                .smeOwnerId(smeOwnerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .targetPrice(request.getTargetPrice())
                .deadline(request.getDeadline())
                .status(JobStatus.OPEN)
                .build();

        JobListing saved = jobListingRepository.save(job);
        return toJobResponse(saved);
    }

    @Transactional
    public BidDto.BidDetailResponse submitBid(UUID factoryOwnerId, BidDto.CreateBidRequest request) {
        JobListing job = jobListingRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job is not accepting bids");
        }

        Bid bid = Bid.builder()
                .jobId(request.getJobId())
                .factoryOwnerId(factoryOwnerId)
                .proposedPrice(request.getProposedPrice())
                .estimatedDays(request.getEstimatedDays())
                .proposalNotes(request.getProposalNotes())
                .status(BidStatus.PENDING)
                .build();

        Bid saved = bidRepository.save(bid);
        return toBidResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BidDto.BidDetailResponse> getBidsForJob(UUID jobId) {
        return bidRepository.findByJobId(jobId).stream()
                .map(this::toBidResponse)
                .collect(Collectors.toList());
    }

    private JobDto.JobDetailResponse toJobResponse(JobListing j) {
        return JobDto.JobDetailResponse.builder()
                .id(j.getId())
                .smeOwnerId(j.getSmeOwnerId())
                .title(j.getTitle())
                .description(j.getDescription())
                .category(j.getCategory())
                .quantity(j.getQuantity())
                .targetPrice(j.getTargetPrice())
                .currency(j.getCurrency())
                .deadline(j.getDeadline())
                .status(j.getStatus())
                .createdAt(j.getCreatedAt())
                .build();
    }

    private BidDto.BidDetailResponse toBidResponse(Bid b) {
        return BidDto.BidDetailResponse.builder()
                .id(b.getId())
                .jobId(b.getJobId())
                .factoryOwnerId(b.getFactoryOwnerId())
                .proposedPrice(b.getProposedPrice())
                .estimatedDays(b.getEstimatedDays())
                .proposalNotes(b.getProposalNotes())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
