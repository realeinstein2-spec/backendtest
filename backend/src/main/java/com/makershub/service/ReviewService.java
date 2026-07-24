package com.makershub.service;

import com.makershub.dto.request.ReviewRequest;
import com.makershub.dto.response.ReviewResponse;
import com.makershub.entity.Order;
import com.makershub.entity.Review;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.NotificationType;
import com.makershub.enums.OrderStatus;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.mapper.DtoMapper;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.ReviewRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    @Transactional
    public ReviewResponse.ReviewDetailResponse submitReview(ReviewRequest.CreateReviewRequest request) {
        User reviewer = getAuthenticatedUser();
        UUID orderId = UUID.fromString(request.getOrderId());
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.REFUNDED) {
            throw new BusinessException("Order must be completed before reviewing", HttpStatus.CONFLICT, "ORDER_NOT_COMPLETED");
        }
        User reviewed;
        if (order.getSme().getId().equals(reviewer.getId())) {
            reviewed = order.getFactory();
        } else if (order.getFactory().getId().equals(reviewer.getId())) {
            reviewed = order.getSme();
        } else {
            throw new UnauthorizedException("You are not a party to this order");
        }
        reviewRepository.findByOrderIdAndReviewerId(orderId, reviewer.getId()).ifPresent(r -> {
            throw new BusinessException("Review already submitted", HttpStatus.CONFLICT, "REVIEW_EXISTS");
        });
        Review review = Review.builder()
                .order(order)
                .reviewer(reviewer)
                .reviewed(reviewed)
                .overallRating(request.getOverallRating())
                .qualityRating(request.getQualityRating())
                .timelinessRating(request.getTimelinessRating())
                .communicationRating(request.getCommunicationRating())
                .comment(request.getComment())
                .build();
        Review saved = reviewRepository.save(review);
        if (saved == null) {
            saved = reviewRepository.saveAndFlush(review);
        }
        updateUserRating(reviewed);
        auditLogger.log(AuditAction.CREATE, "REVIEW", saved.getId(), null, null);

        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(reviewed.getId())
                .phoneNumber(reviewed.getPhoneNumber())
                .type(NotificationType.REVIEW_RECEIVED)
                .title("New Review Received")
                .body(reviewer.getFullName() + " left a " + request.getOverallRating() + "-star review for your order")
                .data(Map.of("orderId", orderId.toString(), "reviewId", saved.getId().toString()))
                .build());

        return mapper.toReviewResponse(saved);
    }

    @Transactional
    protected void updateUserRating(User user) {
        Double avg = reviewRepository.calculateAverageRatingByReviewedId(user.getId());
        long count = reviewRepository.countByReviewedId(user.getId());
        user.setRatingAvg(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        user.setReviewCount((int) count);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse.ReviewDetailResponse> getReviewsForUser(UUID userId, Pageable pageable) {
        return reviewRepository.findByReviewedIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toReviewResponse);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse.ReviewDetailResponse> getReviewsForOrder(UUID orderId) {
        return reviewRepository.findByOrderId(orderId).stream()
                .map(mapper::toReviewResponse)
                .toList();
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
