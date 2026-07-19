package com.makershub.repository;

import com.makershub.entity.OrderProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderProgressLogRepository extends JpaRepository<OrderProgressLog, UUID> {
    List<OrderProgressLog> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
