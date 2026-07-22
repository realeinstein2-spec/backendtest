package com.makershub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class MessageRequest {

    private MessageRequest() {}

    @Data
    public static class CreateMessageRequest {
        @NotBlank
        private String orderId;

        @NotBlank
        @Size(max = 4000)
        private String content;

        @Size(max = 500)
        private String attachmentUrl;
    }
}
