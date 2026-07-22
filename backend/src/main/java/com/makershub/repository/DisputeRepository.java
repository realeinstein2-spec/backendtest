package com.makershub.repository;

import com.makershub.entity.Dispute;
import com.makershub.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {
    Optional<Dispute> findByOrderId(UUID orderId);

    Page<Dispute> findByStatusInOrderByCreatedAtAsc(List<DisputeStatus> statuses, Pageable pageable);

    List<Dispute> findByStatusAndAssignedAdminId(DisputeStatus status, UUID adminId);

    long countByStatus(DisputeStatus status);
}
