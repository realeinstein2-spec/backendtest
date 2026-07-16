package com.makershub.repository;

import com.makershub.entity.JobListing;
import com.makershub.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, UUID> {
    Page<JobListing> findBySmeIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID smeId, Pageable pageable);

    Page<JobListing> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(JobStatus status, Pageable pageable);

    @Query("SELECT j FROM JobListing j WHERE j.status = com.makershub.enums.JobStatus.OPEN AND j.deletedAt IS NULL " +
           "AND (:sectorTag IS NULL OR j.sectorTag = :sectorTag) " +
           "AND (:minBudget IS NULL OR j.budgetMaxGhs >= :minBudget) " +
           "AND (:maxBudget IS NULL OR j.budgetMinGhs <= :maxBudget) " +
           "ORDER BY j.createdAt DESC")
    Page<JobListing> findOpenJobs(@Param("sectorTag") String sectorTag,
                                  @Param("minBudget") java.math.BigDecimal minBudget,
                                  @Param("maxBudget") java.math.BigDecimal maxBudget,
                                  Pageable pageable);

    Optional<JobListing> findByIdAndDeletedAtIsNull(UUID id);
}
