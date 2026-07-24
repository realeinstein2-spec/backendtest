package com.makershub.entity;

import com.makershub.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_listings", indexes = {
        @Index(name = "idx_jobs_sme", columnList = "sme_id"),
        @Index(name = "idx_jobs_status", columnList = "status"),
        @Index(name = "idx_jobs_sector", columnList = "sector_tag"),
        @Index(name = "idx_jobs_deadline", columnList = "deadline")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobListing {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sme_id", nullable = false, foreignKey = @ForeignKey(name = "fk_jobs_sme"))
    private User sme;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "product_type", nullable = false, length = 100)
    private String productType;

    @Column(name = "sector_tag", nullable = false, length = 50)
    private String sectorTag;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "specifications", length = 4000)
    private String specifications;

    @Column(name = "budget_min_ghs", precision = 14, scale = 2)
    private BigDecimal budgetMinGhs;

    @Column(name = "budget_max_ghs", precision = 14, scale = 2)
    private BigDecimal budgetMaxGhs;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @ElementCollection
    @CollectionTable(name = "job_attachments", joinColumns = @JoinColumn(name = "job_id"),
            foreignKey = @ForeignKey(name = "fk_job_attachments_job"))
    @Column(name = "attachment_url", length = 500)
    private List<String> attachmentUrls;

    public List<String> getProductImageUrls() {
        return attachmentUrls;
    }

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private JobStatus status = JobStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "jobListing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids;
}
