package com.makershub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_order_reviewer", columnList = "order_id, reviewer_id", unique = true),
        @Index(name = "idx_reviews_reviewer", columnList = "reviewer_id"),
        @Index(name = "idx_reviews_reviewed", columnList = "reviewed_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_order"))
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_reviewer"))
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_reviewed"))
    private User reviewed;

    @Column(name = "overall_rating", nullable = false)
    private Integer overallRating;

    @Column(name = "quality_rating")
    private Integer qualityRating;

    @Column(name = "timeliness_rating")
    private Integer timelinessRating;

    @Column(name = "communication_rating")
    private Integer communicationRating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
