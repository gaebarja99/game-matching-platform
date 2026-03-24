package com.gamematcher.controller;

import com.gamematcher.dto.chat.ChatDto;
import com.gamematcher.service.ai.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatbotService chatbotService;

    @PostMapping
    public ChatDto.ChatResponse chat(@Valid @RequestBody ChatDto.ChatRequest request) {
        return chatbotService.chat(request.getMessage());
    }
}
