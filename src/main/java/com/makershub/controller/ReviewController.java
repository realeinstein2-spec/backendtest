package com.makershub.controller;

import com.makershub.dto.request.ReviewRequest;
import com.makershub.dto.response.ApiResponse;
import com.makershub.dto.response.ReviewResponse;
import com.makershub.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit a review for a completed order")
    public ResponseEntity<ReviewResponse.ReviewDetailResponse> submitReview(@Valid @RequestBody ReviewRequest.CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.submitReview(request));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get paginated reviews received by a user/factory")
    public ResponseEntity<ApiResponse.PagedResponse<ReviewResponse.ReviewDetailResponse>> getReviewsForUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewResponse.ReviewDetailResponse> page = reviewService.getReviewsForUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.<ReviewResponse.ReviewDetailResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get reviews for a specific order")
    public ResponseEntity<List<ReviewResponse.ReviewDetailResponse>> getReviewsForOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(reviewService.getReviewsForOrder(orderId));
    }
}
