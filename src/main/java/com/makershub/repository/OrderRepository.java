package com.makershub.repository;

import com.makershub.entity.Order;
import com.makershub.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    Page<Order> findBySmeIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID smeId, Pageable pageable);

    Page<Order> findByFactoryIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID factoryId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deletedAt IS NULL")
    List<Order> findByStatus(@Param("status") OrderStatus status);

    List<Order> findByStatusAndQualityCheckDeadlineBeforeAndDeletedAtIsNull(OrderStatus status, Instant deadline);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = com.makershub.enums.OrderStatus.COMPLETED AND o.completedAt >= :since")
    long countCompletedSince(@Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(o.agreedAmountGhs), 0) FROM Order o WHERE o.status = com.makershub.enums.OrderStatus.COMPLETED AND o.completedAt >= :since")
    java.math.BigDecimal sumGmvSince(@Param("since") Instant since);
}
