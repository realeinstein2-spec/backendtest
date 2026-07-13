package com.makershub.repository;

import com.makershub.entity.Bid;
import com.makershub.enums.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findByJobListingIdAndDeletedAtIsNullOrderByTotalPriceGhsAsc(UUID jobId);

    List<Bid> findByJobListingIdAndStatusAndDeletedAtIsNull(UUID jobId, BidStatus status);

    Optional<Bid> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByJobListingIdAndFactoryIdAndDeletedAtIsNull(UUID jobId, UUID factoryId);

    long countByFactoryIdAndStatus(UUID factoryId, BidStatus status);
}
