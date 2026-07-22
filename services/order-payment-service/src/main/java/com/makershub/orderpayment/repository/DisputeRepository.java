package com.makershub.orderpayment.repository;

import com.makershub.orderpayment.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByOrderId(UUID orderId);

    Page<Dispute> findByRaisedByUserId(UUID raisedByUserId, Pageable pageable);
}
