package com.makershub.repository;

import com.makershub.entity.FeaturedListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeaturedListingRepository extends JpaRepository<FeaturedListing, UUID> {
    List<FeaturedListing> findByIsActiveTrueAndEndsAtAfter(Instant now);

    boolean existsByFactoryIdAndIsActiveTrueAndEndsAtAfter(UUID factoryId, Instant now);
}
