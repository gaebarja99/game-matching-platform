package com.gamematcher.service;

import com.gamematcher.dto.chat.ChatMessageDto;
import com.gamematcher.entity.Donation;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.LiveStreamChatMessage;
import com.gamematcher.entity.User;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.LiveStreamChatMessageRepository;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.service.FollowService;
import com.gamematcher.service.LevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final LiveStreamChatMessageRepository chatMessageRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final FollowService followService;

    @Transactional
    public void saveMessage(Long streamId, Long userId, String text) {
        LiveStreamChatMessage msg = new LiveStreamChatMessage();
        msg.setStreamId(streamId);
        msg.setUserId(userId);
        msg.setText(text);
        chatMessageRepository.save(msg);
        // 팔로우했거나 후원한 방에서 채팅 1개당 경험치 0.1 (1 tenth)
        Long streamOwnerId = liveStreamRepository.findById(streamId).map(LiveStream::getUserId).orElse(null);
        if (streamOwnerId != null && !streamOwnerId.equals(userId)) {
            boolean follow = followService.isFollowing(userId, streamOwnerId);
            boolean donated = donationRepository.existsByFromUserIdAndStreamId(userId, streamId);
            if (follow || donated) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    long tenths = user.getTotalExperienceTenths() != null ? user.getTotalExperienceTenths() : 0L;
                    user.setTotalExperienceTenths(tenths + 1L);
                    userRepository.save(user);
                }
            }
        }
    }

    /**
     * 해당 방송의 최근 채팅 내역 (과거순 = 오래된 것 먼저).
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRecentMessages(Long streamId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<LiveStreamChatMessage> list = chatMessageRepository.findByStreamIdOrderByCreatedAtDesc(
                streamId, PageRequest.of(0, safeLimit));
        if (list.isEmpty()) return List.of();
        Collections.reverse(list);
        Long streamOwnerId = liveStreamRepository.findById(streamId).map(LiveStream::getUserId).orElse(null);
        return list.stream()
                .map(m -> toDto(m, streamOwnerId))
                .collect(Collectors.toList());
    }

    private ChatMessageDto toDto(LiveStreamChatMessage m, Long streamOwnerId) {
        User u = userRepository.findById(m.getUserId()).orElse(null);
        String displayName = u != null ? (u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()) : "알 수 없음";
        String profileImageUrl = u != null ? u.getProfileImageUrl() : null;
        String loginId = u != null ? u.getLoginId() : null;
        boolean streamer = streamOwnerId != null && streamOwnerId.equals(m.getUserId());
        int level = u != null && u.getTotalExperienceTenths() != null
                ? LevelService.getLevel(u.getTotalExperienceTenths())
                : 1;
        return ChatMessageDto.builder()
                .userId(m.getUserId())
                .loginId(loginId)
                .displayName(displayName)
                .profileImageUrl(profileImageUrl)
                .text(m.getText())
                .streamer(streamer)
                .level(level)
                .timestamp(m.getCreatedAt() != null ? m.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L)
                .build();
    }

    /**
     * 채팅 + 후원 통합 타임라인 (새로고침 시 후원 내역 복원용).
     * 최근 limit개까지 채팅과 후원을 createdAt 기준으로 합쳐서 과거순으로 반환.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentTimeline(Long streamId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        Long streamOwnerId = liveStreamRepository.findById(streamId).map(LiveStream::getUserId).orElse(null);

        List<LiveStreamChatMessage> chats = chatMessageRepository.findByStreamIdOrderByCreatedAtDesc(
                streamId, PageRequest.of(0, safeLimit));
        List<Donation> donations = donationRepository.findByStreamIdOrderByCreatedAtDesc(
                streamId, PageRequest.of(0, safeLimit));

        List<Map<String, Object>> out = new ArrayList<>();
        for (LiveStreamChatMessage m : chats) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "chat");
            map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().format(ISO) : null);
            User chatUser = userRepository.findById(m.getUserId()).orElse(null);
            String displayName = chatUser != null ? (chatUser.getNickname() != null && !chatUser.getNickname().isBlank() ? chatUser.getNickname() : chatUser.getUsername()) : "알 수 없음";
            map.put("displayName", displayName);
            map.put("userId", m.getUserId());
            map.put("loginId", chatUser != null ? chatUser.getLoginId() : null);
            if (chatUser != null && chatUser.getProfileImageUrl() != null) map.put("profileImageUrl", chatUser.getProfileImageUrl());
            map.put("text", m.getText());
            map.put("streamer", streamOwnerId != null && streamOwnerId.equals(m.getUserId()));
            int level = chatUser != null && chatUser.getTotalExperienceTenths() != null
                    ? LevelService.getLevel(chatUser.getTotalExperienceTenths())
                    : 1;
            map.put("level", level);
            out.add(map);
        }
        for (Donation d : donations) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "donation");
            map.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().format(ISO) : null);
            User donorUser = userRepository.findById(d.getFromUserId()).orElse(null);
            String donorName = donorUser != null ? (donorUser.getNickname() != null && !donorUser.getNickname().isBlank() ? donorUser.getNickname() : donorUser.getUsername()) : "후원자";
            map.put("donorName", donorName);
            if (donorUser != null && donorUser.getProfileImageUrl() != null) map.put("donorProfileImageUrl", donorUser.getProfileImageUrl());
            map.put("amount", d.getAmount());
            map.put("tier", DonationService.getPangTier(d.getAmount() != null ? d.getAmount() : 0));
            map.put("donorMessage", d.getMessage());
            out.add(map);
        }
        out.sort(Comparator.comparing(m -> {
            String at = (String) m.get("createdAt");
            return at != null ? at : "";
        }));
        if (out.size() > safeLimit) {
            out = out.subList(out.size() - safeLimit, out.size());
        }
        return out;
    }
}
