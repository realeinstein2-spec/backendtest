package com.makershub.marketplace.repository;

import com.makershub.marketplace.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {

    List<Bid> findByJobId(UUID jobId);

    Page<Bid> findByFactoryOwnerId(UUID factoryOwnerId, Pageable pageable);
}
