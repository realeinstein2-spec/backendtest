package com.makershub.dto.response;

import com.makershub.enums.SupportTicketCategory;
import com.makershub.enums.SupportTicketPriority;
import com.makershub.enums.SupportTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SupportTicketResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportMessageDetailResponse {
        private UUID id;
        private UUID senderId;
        private String senderName;
        private String senderRole;
        private String message;
        private String attachmentUrl;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportTicketDetailResponse {
        private UUID id;
        private String ticketNumber;
        private UUID userId;
        private String userName;
        private String userEmail;
        private String userPhone;

        private String subject;
        private String description;
        private SupportTicketCategory category;
        private SupportTicketPriority priority;
        private SupportTicketStatus status;

        private Instant targetSlaDeadline;
        private boolean isSlaBreached;

        private UUID assignedAdminId;
        private String assignedAdminName;
        private String adminNotes;
        private String attachmentUrl;

        private Instant createdAt;
        private Instant updatedAt;
        private Instant resolvedAt;

        private List<SupportMessageDetailResponse> messages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportTicketSummaryResponse {
        private UUID id;
        private String ticketNumber;
        private UUID userId;
        private String userName;
        private String subject;
        private SupportTicketCategory category;
        private SupportTicketPriority priority;
        private SupportTicketStatus status;
        private Instant targetSlaDeadline;
        private boolean isSlaBreached;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportTicketStatsResponse {
        private long openTicketsCount;
        private long inProgressTicketsCount;
        private long waitingForUserTicketsCount;
        private long resolvedTicketsCount;
        private long closedTicketsCount;
        private long totalBreachedSlaCount;
        private int slaWindowHours;
    }
}
