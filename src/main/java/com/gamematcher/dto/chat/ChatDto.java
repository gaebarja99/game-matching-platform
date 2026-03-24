package com.gamematcher.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ChatDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ChatRequest {
        @NotBlank(message = "message is required")
        private String message;
    }

    @Getter
    @AllArgsConstructor
    public static class ChatResponse {
        private String reply;
        private boolean aiEnabled;
    }
}
