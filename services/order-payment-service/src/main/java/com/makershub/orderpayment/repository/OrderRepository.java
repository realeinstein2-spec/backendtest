package com.makershub.orderpayment.repository;

import com.makershub.orderpayment.entity.Order;
import com.makershub.orderpayment.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findBySmeOwnerId(UUID smeOwnerId, Pageable pageable);

    Page<Order> findByFactoryOwnerId(UUID factoryOwnerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
