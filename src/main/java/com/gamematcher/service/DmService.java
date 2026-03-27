package com.gamematcher.service;

import com.gamematcher.entity.DmMessage;
import com.gamematcher.entity.User;
import com.gamematcher.repository.DmMessageRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class DmService {

    private static final int MAX_TEXT_LENGTH = 2000;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DmMessageRepository dmMessageRepository;
    private final FriendRequestService friendRequestService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ProfanityFilterService profanityFilterService;

    /** 친구인지 확인 */
    public boolean areFriends(Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null || userId.equals(otherUserId)) return false;
        List<Long> friendIds = friendRequestService.getFriendUserIds(userId);
        return friendIds.contains(otherUserId);
    }

    @Transactional
    public DmMessage send(Long fromUserId, Long toUserId, String text) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId))
            throw new IllegalArgumentException("대상을 지정해 주세요.");
        if (!areFriends(fromUserId, toUserId))
            throw new IllegalArgumentException("친구에게만 메시지를 보낼 수 있습니다.");
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) throw new IllegalArgumentException("메시지를 입력해 주세요.");
        if (trimmed.length() > MAX_TEXT_LENGTH) trimmed = trimmed.substring(0, MAX_TEXT_LENGTH);
        ProfanityFilterService.ModerationResult moderation = profanityFilterService.moderateChat(fromUserId, trimmed);
        DmMessage msg = new DmMessage();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setText(moderation.getSanitizedText());
        msg = dmMessageRepository.save(msg);
        notificationService.createForNewDm(toUserId, fromUserId);
        Map<String, Object> payload = Map.of(
                "id", msg.getId(),
                "fromUserId", msg.getFromUserId(),
                "toUserId", msg.getToUserId(),
                "text", msg.getText(),
                "createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().format(ISO) : "");
        messagingTemplate.convertAndSend("/topic/user/" + toUserId, payload);
        return msg;
    }

    /** 대화 목록 (최신순, 친구만) */
    public List<DmMessage> getConversation(Long userId, Long otherUserId, int limit) {
        if (userId == null || otherUserId == null) return List.of();
        if (!areFriends(userId, otherUserId)) return List.of();
        int size = Math.min(Math.max(1, limit), 100);
        return dmMessageRepository.findConversation(userId, otherUserId, PageRequest.of(0, size));
    }

    /** 채팅 목록 (대화 상대별 최근 메시지 1건, 최신 대화순) */
    public List<Map<String, Object>> getConversationList(Long userId, int limit) {
        if (userId == null) return List.of();
        int size = Math.min(Math.max(1, limit), 50);
        List<DmMessage> recent = dmMessageRepository.findRecentByUserId(userId, PageRequest.of(0, 200));
        Set<Long> seen = new HashSet<>();
        List<Map<String, Object>> list = new ArrayList<>();
        for (DmMessage m : recent) {
            Long other = m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId();
            if (other.equals(userId)) continue;
            if (seen.contains(other)) continue;
            seen.add(other);
            User otherUser = userRepository.findById(other).orElse(null);
            String nickname = otherUser != null && otherUser.getNickname() != null && !otherUser.getNickname().isBlank()
                    ? otherUser.getNickname() : (otherUser != null ? otherUser.getUsername() : "알 수 없음");
            String loginId = otherUser != null ? otherUser.getLoginId() : "";
            String profileImageUrl = otherUser != null ? otherUser.getProfileImageUrl() : null;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("otherUserId", other);
            map.put("nickname", nickname);
            map.put("loginId", loginId != null ? loginId : "");
            map.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
            map.put("lastMessage", m.getText() != null ? m.getText() : "");
            map.put("lastMessageAt", m.getCreatedAt() != null ? m.getCreatedAt().format(ISO) : "");
            long unreadCount = notificationService.getUnreadDmCountByActor(userId, other);
            map.put("unreadCount", unreadCount);
            list.add(map);
            if (list.size() >= size) break;
        }
        return list;
    }
}
