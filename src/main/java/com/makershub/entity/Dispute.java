package com.makershub.entity;

import com.makershub.enums.DisputeReason;
import com.makershub.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "disputes", indexes = {
        @Index(name = "idx_disputes_order", columnList = "order_id", unique = true),
        @Index(name = "idx_disputes_status", columnList = "status"),
        @Index(name = "idx_disputes_assigned", columnList = "assigned_admin_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_disputes_order"))
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_id", nullable = false, foreignKey = @ForeignKey(name = "fk_disputes_raised_by"))
    private User raisedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private DisputeReason reason;

    @Column(name = "description", length = 4000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "dispute_evidence", joinColumns = @JoinColumn(name = "dispute_id"),
            foreignKey = @ForeignKey(name = "fk_dispute_evidence_dispute"))
    @Column(name = "evidence_url", length = 500)
    private List<String> evidenceUrls;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id", foreignKey = @ForeignKey(name = "fk_disputes_admin"))
    private User assignedAdmin;

    @Column(name = "admin_notes", length = 4000)
    private String adminNotes;

    @Column(name = "resolution_amount_ghs", precision = 14, scale = 2)
    private java.math.BigDecimal resolutionAmountGhs;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
