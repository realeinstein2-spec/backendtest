package com.makershub.repository;

import com.makershub.entity.SupportTicket;
import com.makershub.enums.SupportTicketCategory;
import com.makershub.enums.SupportTicketStatus;
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
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Optional<SupportTicket> findByIdAndDeletedAtIsNull(UUID id);

    Page<SupportTicket> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Page<SupportTicket> findByStatusAndDeletedAtIsNull(SupportTicketStatus status, Pageable pageable);

    Page<SupportTicket> findByCategoryAndDeletedAtIsNull(SupportTicketCategory category, Pageable pageable);

    Page<SupportTicket> findAllByDeletedAtIsNull(Pageable pageable);

    long countByStatusAndDeletedAtIsNull(SupportTicketStatus status);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status IN :statuses AND t.targetSlaDeadline < :now AND t.deletedAt IS NULL")
    long countBreachedSla(@Param("statuses") List<SupportTicketStatus> statuses, @Param("now") Instant now);
}
