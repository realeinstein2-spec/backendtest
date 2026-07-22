package com.makershub.entity;

import com.makershub.enums.EscrowStatus;
import com.makershub.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "escrow_transactions", indexes = {
        @Index(name = "idx_escrow_order", columnList = "order_id"),
        @Index(name = "idx_escrow_reference", columnList = "paystack_reference", unique = true),
        @Index(name = "idx_escrow_status", columnList = "escrow_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowTransaction {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_escrow_order"))
    private Order order;

    @Column(name = "paystack_reference", unique = true, length = 100)
    private String paystackReference;

    @Column(name = "paystack_authorization_code", length = 100)
    private String paystackAuthorizationCode;

    @Column(name = "amount_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal amountGhs;

    @Column(name = "fee_amount_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal feeAmountGhs;

    @Column(name = "factory_payout_ghs", precision = 14, scale = 2, nullable = false)
    private BigDecimal factoryPayoutGhs;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", nullable = false, length = 30)
    @Builder.Default
    private EscrowStatus escrowStatus = EscrowStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
