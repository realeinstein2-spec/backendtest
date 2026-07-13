package com.makershub.repository;

import com.makershub.entity.Factory;
import com.makershub.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FactoryRepository extends JpaRepository<Factory, UUID> {
    Optional<Factory> findByUserId(UUID userId);

    Optional<Factory> findByUserIdAndDeletedAtIsNull(UUID userId);

    @Query(value = """
            SELECT f.* FROM factories f
            JOIN factory_sectors fs ON f.id = fs.factory_id
            WHERE f.verification_status = 'VERIFIED'
              AND f.deleted_at IS NULL
              AND fs.sector_tag = :sectorTag
              AND (f.min_order_quantity IS NULL OR f.min_order_quantity <= :quantity)
              AND (f.max_order_quantity IS NULL OR f.max_order_quantity >= :quantity)
              AND ST_DWithin(f.gps_coordinates::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :distanceMeters)
            """, nativeQuery = true)
    List<Factory> findMatchingFactories(@Param("sectorTag") String sectorTag,
                                        @Param("quantity") Integer quantity,
                                        @Param("lat") Double lat,
                                        @Param("lon") Double lon,
                                        @Param("distanceMeters") Double distanceMeters);

    List<Factory> findByVerificationStatus(VerificationStatus status);

    Page<Factory> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    long countByVerificationStatus(VerificationStatus status);
}
