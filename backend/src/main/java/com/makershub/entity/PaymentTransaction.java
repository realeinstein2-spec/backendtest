package com.makershub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_transactions_ref", columnList = "reference", unique = true),
        @Index(name = "idx_payment_transactions_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, insertable = false, updatable = false)
    private Order order;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, insertable = false, updatable = false)
    private User customer;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "amount_pesewas", nullable = false)
    private Long amountPesewas;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "GHS";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING | PAID | FAILED

    @Column(name = "authorization_url", nullable = false, length = 1000)
    private String authorizationUrl;

    @Column(name = "paystack_transaction_id", length = 100)
    private String paystackTransactionId;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
