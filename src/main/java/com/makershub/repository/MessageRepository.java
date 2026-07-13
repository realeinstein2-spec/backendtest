package com.makershub.repository;

import com.makershub.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByOrderIdOrderByCreatedAtAsc(UUID orderId, Pageable pageable);

    long countByOrderIdAndSenderIdNotAndIsReadFalse(UUID orderId, UUID senderId);
}
