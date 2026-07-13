package com.makershub.entity;

import com.makershub.enums.BidStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bids", indexes = {
        @Index(name = "idx_bids_job", columnList = "job_id"),
        @Index(name = "idx_bids_factory", columnList = "factory_id"),
        @Index(name = "idx_bids_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bids_job"))
    private JobListing jobListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bids_factory"))
    private Factory factory;

    @Column(name = "price_per_unit_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal pricePerUnitGhs;

    @Column(name = "total_price_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalPriceGhs;

    @Column(name = "production_days", nullable = false)
    private Integer productionDays;

    @Column(name = "delivery_date_estimate", nullable = false)
    private LocalDate deliveryDateEstimate;

    @Column(name = "message", length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BidStatus status = BidStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
