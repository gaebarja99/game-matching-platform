package com.gamematcher.service.ai;

import com.gamematcher.dto.chat.ChatDto;

public interface ChatbotService {

    ChatDto.ChatResponse chat(String userMessage);
}
