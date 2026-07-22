package com.makershub.orderpayment.repository;

import com.makershub.orderpayment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByPaymentReference(String paymentReference);

    Optional<PaymentTransaction> findByOrderId(UUID orderId);
}
