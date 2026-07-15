package com.makershub.repository;

import com.makershub.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByOrderIdAndReviewerId(UUID orderId, UUID reviewerId);

    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.reviewed.id = :userId")
    Double calculateAverageRatingByReviewedId(@Param("userId") UUID userId);

    Page<Review> findByReviewedIdOrderByCreatedAtDesc(UUID reviewedId, Pageable pageable);
}
