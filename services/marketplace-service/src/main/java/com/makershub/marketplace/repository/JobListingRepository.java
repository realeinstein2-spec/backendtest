package com.makershub.marketplace.repository;

import com.makershub.marketplace.entity.JobListing;
import com.makershub.marketplace.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, UUID> {

    Page<JobListing> findByStatus(JobStatus status, Pageable pageable);

    Page<JobListing> findBySmeOwnerId(UUID smeOwnerId, Pageable pageable);
}
