package com.makershub.repository;

import com.makershub.entity.EscrowTransaction;
import com.makershub.enums.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, UUID> {
    Optional<EscrowTransaction> findByPaystackReference(String paystackReference);

    Optional<EscrowTransaction> findByOrderId(UUID orderId);

    List<EscrowTransaction> findByEscrowStatusAndRetryCountLessThan(EscrowStatus status, Integer retryCount);

    long countByEscrowStatus(EscrowStatus status);
}
