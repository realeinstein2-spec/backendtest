package com.makershub.entity;

import com.makershub.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "factories", indexes = {
        @Index(name = "idx_factories_user", columnList = "user_id", unique = true),
        @Index(name = "idx_factories_status", columnList = "verification_status"),
        @Index(name = "idx_factories_featured", columnList = "is_featured"),
        @Index(name = "idx_factories_location", columnList = "gps_coordinates")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factory {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_factories_user"))
    private User user;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "description", length = 2000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "factory_sectors", joinColumns = @JoinColumn(name = "factory_id"),
            foreignKey = @ForeignKey(name = "fk_factory_sectors_factory"))
    @Column(name = "sector_tag", length = 50)
    private List<String> sectorTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "machinery_list", columnDefinition = "jsonb")
    private String machineryList;

    @Column(name = "payout_account_type", length = 50)
    private String payoutAccountType;

    @Column(name = "payout_account_name", length = 255)
    private String payoutAccountName;

    @Column(name = "payout_account_number", length = 100)
    private String payoutAccountNumber;

    @Column(name = "payout_bank_code", length = 50)
    private String payoutBankCode;

    @Column(name = "min_order_quantity")
    private Integer minOrderQuantity;

    @Column(name = "max_order_quantity")
    private Integer maxOrderQuantity;

    @JsonIgnore
    @Column(name = "gps_coordinates", columnDefinition = "geometry(Point, 4326)")
    private Point gpsCoordinates;

    public Double getLatitude() {
        return gpsCoordinates != null ? gpsCoordinates.getY() : null;
    }

    public Double getLongitude() {
        return gpsCoordinates != null ? gpsCoordinates.getX() : null;
    }

    public UUID getOwnerId() {
        return user != null ? user.getId() : null;
    }

    public String getOwnerName() {
        return user != null ? user.getFullName() : null;
    }

    public String getOwnerPhoneNumber() {
        return user != null ? user.getPhoneNumber() : null;
    }

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verification_notes", length = 1000)
    private String verificationNotes;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "featured_until")
    private Instant featuredUntil;

    @Column(name = "response_time_hours")
    private Double responseTimeHours;

    @Column(name = "completion_rate")
    private Double completionRate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
