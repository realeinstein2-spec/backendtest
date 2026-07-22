package com.makershub.dto.request;

import com.makershub.enums.SupportTicketCategory;
import com.makershub.enums.SupportTicketPriority;
import com.makershub.enums.SupportTicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class SupportTicketRequest {

    @Data
    public static class CreateTicketRequest {

        @NotBlank(message = "Subject is required")
        @Size(max = 255, message = "Subject cannot exceed 255 characters")
        private String subject;

        @NotBlank(message = "Description is required")
        private String description;

        @NotNull(message = "Category is required")
        private SupportTicketCategory category;

        private SupportTicketPriority priority = SupportTicketPriority.MEDIUM;

        private String attachmentUrl;
    }

    @Data
    public static class AddMessageRequest {

        @NotBlank(message = "Message content is required")
        private String message;

        private String attachmentUrl;
    }

    @Data
    public static class UpdateTicketStatusRequest {

        @NotNull(message = "Status is required")
        private SupportTicketStatus status;

        private String adminNotes;

        private String assignedAdminId;
    }
}
