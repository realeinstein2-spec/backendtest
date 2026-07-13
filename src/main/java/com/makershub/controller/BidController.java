package com.makershub.controller;

import com.makershub.dto.request.BidRequest;
import com.makershub.dto.response.BidResponse;
import com.makershub.service.BidService;
import com.makershub.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bid System")
public class BidController {

    private final BidService bidService;
    private final OrderService orderService;

    @PostMapping("/jobs/{jobId}/bids")
    @PreAuthorize("hasRole('FACTORY_OWNER')")
    public ResponseEntity<BidResponse.BidDetailResponse> submitBid(@PathVariable UUID jobId, @Valid @RequestBody BidRequest.CreateBidRequest request) {
        request.setJobId(jobId.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(bidService.submitBid(request));
    }

    @GetMapping("/jobs/{jobId}/bids")
    @PreAuthorize("hasRole('SME_OWNER')")
    public ResponseEntity<List<BidResponse.BidDetailResponse>> listBids(@PathVariable UUID jobId) {
        return ResponseEntity.ok(bidService.listBidsForJob(jobId));
    }

    @PatchMapping("/bids/{bidId}/accept")
    @PreAuthorize("hasRole('SME_OWNER')")
    public ResponseEntity<?> acceptBid(@PathVariable UUID bidId) {
        return ResponseEntity.ok(orderService.acceptBid(bidId));
    }
}
