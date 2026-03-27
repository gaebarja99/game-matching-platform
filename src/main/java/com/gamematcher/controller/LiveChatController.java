package com.gamematcher.controller;

import com.gamematcher.dto.chat.ChatMessageDto;
import com.gamematcher.entity.User;
import com.gamematcher.service.ChatService;
import com.gamematcher.service.LevelService;
import com.gamematcher.service.ProfanityFilterService;
import com.gamematcher.service.StreamChatSettingsService;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 라이브 방송 채팅. STOMP /app/chat/{streamId} 로 전송 시 /topic/stream/{streamId} 로 브로드캐스트.
 * 로그인한 사용자만 발송 가능.
 */
@Controller
@RequiredArgsConstructor
public class LiveChatController {

    private static final String SESSION_USER_ID = "userId";
    private static final int MAX_TEXT_LENGTH = 500;

    private final UserRepository userRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final ChatService chatService;
    private final StreamChatSettingsService streamChatSettingsService;
    private final ProfanityFilterService profanityFilterService;

    @MessageMapping("/chat/{streamId}")
    @SendTo("/topic/stream/{streamId}")
    public ChatMessageDto send(@DestinationVariable Long streamId, String text, SimpMessageHeaderAccessor accessor) {
        Object uidObj = accessor != null && accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get(SESSION_USER_ID)
                : null;
        if (uidObj == null || !(uidObj instanceof Long)) {
            return null;
        }
        Long userId = (Long) uidObj;
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty() || trimmed.length() > MAX_TEXT_LENGTH) {
            return null;
        }
        ProfanityFilterService.ModerationResult moderation;
        try {
            moderation = profanityFilterService.moderateChat(userId, trimmed);
        } catch (IllegalArgumentException e) {
            return ChatMessageDto.builder()
                    .userId(0L)
                    .displayName("SYSTEM")
                    .text(e.getMessage())
                    .build();
        }
        boolean isStreamer = liveStreamRepository.findById(streamId)
                .map(stream -> userId.equals(stream.getUserId()))
                .orElse(false);
        boolean isManager = streamChatSettingsService.isManager(streamId, userId);
        if (streamChatSettingsService.isBanned(streamId, userId)) {
            return null;
        }
        if (liveStreamRepository.findById(streamId)
                .map(s -> Boolean.TRUE.equals(s.getChatFrozen()) && !isStreamer && !isManager)
                .orElse(false)) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        String displayName = user != null ? (user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername()) : "알 수 없음";
        String profileImageUrl = user != null ? user.getProfileImageUrl() : null;
        String loginId = user != null ? user.getLoginId() : null;
        chatService.saveMessage(streamId, userId, moderation.getSanitizedText());
        int level = user != null && user.getTotalExperienceTenths() != null
                ? LevelService.getLevel(user.getTotalExperienceTenths())
                : 1;
        return ChatMessageDto.builder()
                .userId(userId)
                .loginId(loginId)
                .displayName(displayName)
                .profileImageUrl(profileImageUrl)
                .text(moderation.getSanitizedText())
                .streamer(isStreamer)
                .level(level)
                .build();
    }
}
