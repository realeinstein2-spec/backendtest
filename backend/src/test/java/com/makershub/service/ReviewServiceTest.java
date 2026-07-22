package com.makershub.service;

import com.makershub.dto.request.ReviewRequest;
import com.makershub.dto.response.ReviewResponse;
import com.makershub.entity.Order;
import com.makershub.entity.Review;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.OrderStatus;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.mapper.DtoMapper;
import com.makershub.notification.NotificationService;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.ReviewRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DtoMapper mapper;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitReview_success_smeReviewingFactory() {
        User sme = createUser(UserRole.SME_OWNER);
        User factory = createUser(UserRole.FACTORY_OWNER);
        authenticate(sme);

        Order order = createOrder(sme, factory, OrderStatus.COMPLETED);
        ReviewRequest.CreateReviewRequest request = createReviewRequest(order.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(reviewRepository.findByOrderIdAndReviewerId(order.getId(), sme.getId())).thenReturn(Optional.empty());

        Review review = Review.builder().id(UUID.randomUUID()).build();
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageRatingByReviewedId(factory.getId())).thenReturn(4.5);
        when(reviewRepository.countByReviewedId(factory.getId())).thenReturn(1L);

        ReviewResponse.ReviewDetailResponse expectedResponse = ReviewResponse.ReviewDetailResponse.builder().build();
        when(mapper.toReviewResponse(any(Review.class))).thenReturn(expectedResponse);

        ReviewResponse.ReviewDetailResponse response = reviewService.submitReview(request);

        assertThat(response).isNotNull();
        verify(reviewRepository).save(any(Review.class));
        verify(userRepository).save(factory);
        assertThat(factory.getRatingAvg()).isEqualTo(4.5);
        assertThat(factory.getReviewCount()).isEqualTo(1);
        verify(auditLogger).log(eq(AuditAction.CREATE), eq("REVIEW"), eq(review.getId()), any(), any());
        verify(notificationService).sendNotification(any());
    }

    @Test
    void submitReview_success_factoryReviewingSme() {
        User sme = createUser(UserRole.SME_OWNER);
        User factory = createUser(UserRole.FACTORY_OWNER);
        authenticate(factory);

        Order order = createOrder(sme, factory, OrderStatus.COMPLETED);
        ReviewRequest.CreateReviewRequest request = createReviewRequest(order.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(factory.getId())).thenReturn(Optional.of(factory));
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(reviewRepository.findByOrderIdAndReviewerId(order.getId(), factory.getId())).thenReturn(Optional.empty());

        Review review = Review.builder().id(UUID.randomUUID()).build();
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageRatingByReviewedId(sme.getId())).thenReturn(4.8);
        when(reviewRepository.countByReviewedId(sme.getId())).thenReturn(3L);

        ReviewResponse.ReviewDetailResponse expectedResponse = ReviewResponse.ReviewDetailResponse.builder().build();
        when(mapper.toReviewResponse(any(Review.class))).thenReturn(expectedResponse);

        ReviewResponse.ReviewDetailResponse response = reviewService.submitReview(request);

        assertThat(response).isNotNull();
        verify(reviewRepository).save(any(Review.class));
        verify(userRepository).save(sme);
        assertThat(sme.getRatingAvg()).isEqualTo(4.8);
        assertThat(sme.getReviewCount()).isEqualTo(3);
        verify(auditLogger).log(eq(AuditAction.CREATE), eq("REVIEW"), eq(review.getId()), any(), any());
        verify(notificationService).sendNotification(any());
    }

    @Test
    void submitReview_throwsResourceNotFoundException_whenOrderNotFound() {
        User sme = createUser(UserRole.SME_OWNER);
        authenticate(sme);

        UUID orderId = UUID.randomUUID();
        ReviewRequest.CreateReviewRequest request = createReviewRequest(orderId);

        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));
        when(orderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submitReview(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(orderId.toString());

        verifyNoInteractions(reviewRepository);
    }

    @Test
    void submitReview_throwsBusinessException_whenOrderNotCompleted() {
        User sme = createUser(UserRole.SME_OWNER);
        User factory = createUser(UserRole.FACTORY_OWNER);
        authenticate(sme);

        Order order = createOrder(sme, factory, OrderStatus.PAYMENT_PENDING);
        ReviewRequest.CreateReviewRequest request = createReviewRequest(order.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewService.submitReview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order must be completed");

        verifyNoInteractions(reviewRepository);
    }

    @Test
    void submitReview_throwsUnauthorizedException_whenUserNotPartyToOrder() {
        User sme = createUser(UserRole.SME_OWNER);
        User factory = createUser(UserRole.FACTORY_OWNER);
        User stranger = createUser(UserRole.SME_OWNER);
        authenticate(stranger);

        Order order = createOrder(sme, factory, OrderStatus.COMPLETED);
        ReviewRequest.CreateReviewRequest request = createReviewRequest(order.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(stranger.getId())).thenReturn(Optional.of(stranger));
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewService.submitReview(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not a party to this order");

        verifyNoInteractions(reviewRepository);
    }

    @Test
    void submitReview_throwsBusinessException_whenReviewAlreadyExists() {
        User sme = createUser(UserRole.SME_OWNER);
        User factory = createUser(UserRole.FACTORY_OWNER);
        authenticate(sme);

        Order order = createOrder(sme, factory, OrderStatus.COMPLETED);
        ReviewRequest.CreateReviewRequest request = createReviewRequest(order.getId());

        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(reviewRepository.findByOrderIdAndReviewerId(order.getId(), sme.getId())).thenReturn(Optional.of(new Review()));

        assertThatThrownBy(() -> reviewService.submitReview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Review already submitted");

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void getReviewsForUser_returnsPageOfReviews() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Review review = Review.builder().id(UUID.randomUUID()).build();
        Page<Review> reviewPage = new PageImpl<>(List.of(review));

        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(reviewPage);

        ReviewResponse.ReviewDetailResponse responseDetail = ReviewResponse.ReviewDetailResponse.builder().build();
        when(mapper.toReviewResponse(review)).thenReturn(responseDetail);

        Page<ReviewResponse.ReviewDetailResponse> result = reviewService.getReviewsForUser(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(responseDetail);
    }

    @Test
    void getReviewsForOrder_returnsListOfReviews() {
        UUID orderId = UUID.randomUUID();
        Review review = Review.builder().id(UUID.randomUUID()).build();

        when(reviewRepository.findByOrderId(orderId)).thenReturn(List.of(review));

        ReviewResponse.ReviewDetailResponse responseDetail = ReviewResponse.ReviewDetailResponse.builder().build();
        when(mapper.toReviewResponse(review)).thenReturn(responseDetail);

        List<ReviewResponse.ReviewDetailResponse> result = reviewService.getReviewsForOrder(orderId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(responseDetail);
    }

    private User createUser(UserRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("+233" + (int)(Math.random() * 1000000000))
                .fullName("Test User")
                .role(role)
                .isVerified(true)
                .isActive(true)
                .passwordHash("password_hash")
                .build();
    }

    private Order createOrder(User sme, User factory, OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .sme(sme)
                .factory(factory)
                .status(status)
                .build();
    }

    private ReviewRequest.CreateReviewRequest createReviewRequest(UUID orderId) {
        ReviewRequest.CreateReviewRequest request = new ReviewRequest.CreateReviewRequest();
        request.setOrderId(orderId.toString());
        request.setOverallRating(5);
        request.setQualityRating(5);
        request.setTimelinessRating(5);
        request.setCommunicationRating(5);
        request.setComment("Excellent work!");
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
