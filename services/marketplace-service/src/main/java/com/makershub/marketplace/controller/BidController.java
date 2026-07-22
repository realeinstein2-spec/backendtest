package com.makershub.marketplace.controller;

import com.makershub.marketplace.dto.BidDto;
import com.makershub.marketplace.service.MarketplaceAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BidController {

    private final MarketplaceAppService marketplaceAppService;

    @PostMapping
    public ResponseEntity<BidDto.BidDetailResponse> submitBid(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody BidDto.CreateBidRequest request) {
        UUID userId = parseUserId(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplaceAppService.submitBid(userId, request));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<BidDto.BidDetailResponse>> getBidsForJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(marketplaceAppService.getBidsForJob(jobId));
    }

    private UUID parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id format");
        }
    }
}
