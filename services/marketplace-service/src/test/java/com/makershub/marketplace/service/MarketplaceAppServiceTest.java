package com.makershub.marketplace.service;

import com.makershub.marketplace.dto.BidDto;
import com.makershub.marketplace.dto.JobDto;
import com.makershub.marketplace.entity.Bid;
import com.makershub.marketplace.entity.JobListing;
import com.makershub.marketplace.enums.BidStatus;
import com.makershub.marketplace.enums.JobStatus;
import com.makershub.marketplace.repository.BidRepository;
import com.makershub.marketplace.repository.JobListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceAppServiceTest {

    @Mock
    private JobListingRepository jobListingRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private MarketplaceAppService marketplaceAppService;

    @Test
    void createJob_success() {
        UUID smeId = UUID.randomUUID();
        JobDto.CreateJobRequest request = new JobDto.CreateJobRequest();
        request.setTitle("Metal Fabrication");
        request.setDescription("100 steel brackets");
        request.setCategory("FABRICATION");
        request.setQuantity(100);
        request.setTargetPrice(BigDecimal.valueOf(5000));

        when(jobListingRepository.save(any(JobListing.class))).thenAnswer(i -> {
            JobListing j = i.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        JobDto.JobDetailResponse response = marketplaceAppService.createJob(smeId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Metal Fabrication");
        assertThat(response.getQuantity()).isEqualTo(100);
        assertThat(response.getStatus()).isEqualTo(JobStatus.OPEN);
    }

    @Test
    void submitBid_success() {
        UUID factoryId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobListing job = JobListing.builder()
                .id(jobId)
                .status(JobStatus.OPEN)
                .build();

        BidDto.CreateBidRequest request = new BidDto.CreateBidRequest();
        request.setJobId(jobId);
        request.setProposedPrice(BigDecimal.valueOf(4500));
        request.setEstimatedDays(7);
        request.setProposalNotes("Can complete in 1 week");

        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> {
            Bid b = i.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        BidDto.BidDetailResponse response = marketplaceAppService.submitBid(factoryId, request);

        assertThat(response).isNotNull();
        assertThat(response.getProposedPrice()).isEqualTo(BigDecimal.valueOf(4500));
        assertThat(response.getStatus()).isEqualTo(BidStatus.PENDING);
    }
}
