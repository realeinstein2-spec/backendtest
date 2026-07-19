package com.makershub.entity;

import com.makershub.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_job", columnList = "job_id", unique = true),
        @Index(name = "idx_orders_bid", columnList = "bid_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_sme", columnList = "sme_id"),
        @Index(name = "idx_orders_factory", columnList = "factory_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_job"))
    private JobListing jobListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_bid"))
    private Bid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sme_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_sme"))
    private User sme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_factory"))
    private User factory;

    @Column(name = "agreed_amount_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal agreedAmountGhs;

    @Column(name = "platform_fee_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal platformFeeGhs;

    @Column(name = "factory_payout_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal factoryPayoutGhs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PAYMENT_PENDING;

    @Column(name = "current_progress_percentage")
    @Builder.Default
    private Integer currentProgressPercentage = 0;

    @Column(name = "current_production_stage")
    @Builder.Default
    private String currentProductionStage = "Payment Pending";

    @Column(name = "quality_check_deadline")
    private Instant qualityCheckDeadline;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
