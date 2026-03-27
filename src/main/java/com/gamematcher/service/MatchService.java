package com.gamematcher.service;

import com.gamematcher.entity.*;
import com.gamematcher.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int DEFAULT_MATCH_GROUP_SIZE = 2;
    private static final int VALORANT_MATCH_GROUP_SIZE = 5;
    private static final int MAX_TEXT_LENGTH = 2000;

    private final MatchQueueEntryRepository queueRepository;
    private final MatchSessionRepository sessionRepository;
    private final MatchSessionMemberRepository sessionMemberRepository;
    private final MatchChatMessageRepository matchChatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProfanityFilterService profanityFilterService;
    private final NotificationService notificationService;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\p{L}\\p{N}_.-]+)");

    /** 대기열 참가 → 매칭 시도 */
    @Transactional
    public Map<String, Object> joinQueue(Long userId, String game, String tier, String position, Integer maxPlayers) {
        if (userId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (game == null || game.isBlank()) game = "LEAGUE_OF_LEGENDS";

        queueRepository.findByUserId(userId).ifPresent(queueRepository::delete);

        MatchQueueEntry entry = new MatchQueueEntry();
        entry.setUserId(userId);
        entry.setGame(game);
        entry.setTier(tier != null && !tier.isBlank() ? tier : null);
        entry.setPosition(position != null && !position.isBlank() ? position : null);
        entry.setMaxPlayers(maxPlayers);
        queueRepository.save(entry);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inQueue", true);
        result.put("game", game);

        tryMatch(game, entry.getMaxPlayers());
        return result;
    }

    /** 같은 게임(+정원) 대기열에서 인원 충족 시 매칭 생성 후 알림 */
    @Transactional
    protected void tryMatch(String game, Integer maxPlayers) {
        List<MatchQueueEntry> entries = maxPlayers != null
                ? queueRepository.findByGameAndMaxPlayersOrderByJoinedAtAsc(game, maxPlayers)
                : queueRepository.findByGameOrderByJoinedAtAsc(game);
        int groupSize = maxPlayers != null ? maxPlayers : groupSizeForGame(game);
        if (entries.size() < groupSize) return;

        List<MatchQueueEntry> toMatch = entries.subList(0, groupSize);
        MatchSession session = new MatchSession();
        session.setGame(game);
        session = sessionRepository.save(session);

        List<Long> userIds = new ArrayList<>();
        for (MatchQueueEntry e : toMatch) {
            queueRepository.delete(e);
            MatchSessionMember m = new MatchSessionMember();
            m.setSessionId(session.getId());
            m.setUserId(e.getUserId());
            sessionMemberRepository.save(m);
            userIds.add(e.getUserId());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "MATCH_COMPLETE");
        payload.put("sessionId", session.getId());
        payload.put("game", session.getGame());
        payload.put("memberUserIds", userIds);

        List<Long> userIdsCopy = new ArrayList<>(userIds);
        Map<String, Object> payloadCopy = new LinkedHashMap<>(payload);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Long uid : userIdsCopy) {
                        messagingTemplate.convertAndSend("/topic/user/" + uid, payloadCopy);
                    }
                }
            });
        } else {
            for (Long uid : userIdsCopy) {
                messagingTemplate.convertAndSend("/topic/user/" + uid, payloadCopy);
            }
        }
    }

    private int groupSizeForGame(String game) {
        if ("VALORANT".equalsIgnoreCase(game)) return VALORANT_MATCH_GROUP_SIZE;
        if ("OVERWATCH".equalsIgnoreCase(game)) return VALORANT_MATCH_GROUP_SIZE;
        return DEFAULT_MATCH_GROUP_SIZE;
    }

    public Map<String, Object> queueStatusDetail(Long userId) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (userId == null) {
            m.put("inQueue", false);
            m.put("currentParticipants", 0);
            m.put("maxParticipants", DEFAULT_MATCH_GROUP_SIZE);
            return m;
        }
        Optional<MatchQueueEntry> opt = queueRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            m.put("inQueue", false);
            m.put("currentParticipants", 0);
            m.put("maxParticipants", DEFAULT_MATCH_GROUP_SIZE);
            return m;
        }
        MatchQueueEntry e = opt.get();
        String game = e.getGame();
        int max = e.getMaxPlayers() != null ? e.getMaxPlayers() : groupSizeForGame(game);
        long cnt = e.getMaxPlayers() != null
                ? queueRepository.countByGameAndMaxPlayers(game, e.getMaxPlayers())
                : queueRepository.countByGame(game);
        m.put("inQueue", true);
        m.put("game", game);
        m.put("currentParticipants", (int) Math.min(Integer.MAX_VALUE, cnt));
        m.put("maxParticipants", max);
        return m;
    }

    /** 대기열 나가기 */
    @Transactional
    public boolean leaveQueue(Long userId) {
        if (userId == null) return false;
        Optional<MatchQueueEntry> opt = queueRepository.findByUserId(userId);
        if (opt.isEmpty()) return false;
        queueRepository.delete(opt.get());
        return true;
    }

    /** 대기열 상태 */
    public boolean isInQueue(Long userId) {
        return userId != null && queueRepository.existsByUserId(userId);
    }

    /** 매칭 세션 조회 (참가자만) */
    public Optional<Map<String, Object>> getSession(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) return Optional.empty();
        if (!sessionMemberRepository.existsBySessionIdAndUserId(sessionId, userId)) return Optional.empty();
        return sessionRepository.findById(sessionId).map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("game", s.getGame());
            map.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().format(ISO) : "");
            List<Map<String, Object>> members = sessionMemberRepository.findBySessionId(sessionId).stream()
                    .map(m -> {
                        Map<String, Object> mm = new LinkedHashMap<>();
                        mm.put("userId", m.getUserId());
                        if (m.getAssignedLane() != null) {
                            mm.put("assignedLane", m.getAssignedLane());
                        }
                        userRepository.findById(m.getUserId()).ifPresent(u -> {
                            mm.put("nickname", (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername());
                            mm.put("profileImageUrl", u.getProfileImageUrl());
                        });
                        return mm;
                    })
                    .collect(Collectors.toList());
            map.put("members", members);
            return map;
        });
    }

    /** 내 매칭 세션 목록 (랜덤 채팅 내역) */
    public List<Map<String, Object>> getMySessions(Long userId) {
        if (userId == null) return List.of();
        return sessionMemberRepository.findByUserIdOrderByJoinedAtDesc(userId).stream()
                .map(m -> sessionRepository.findById(m.getSessionId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", s.getId());
                    map.put("game", s.getGame());
                    map.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().format(ISO) : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    /** 매칭 채팅 메시지 목록 */
    public List<Map<String, Object>> getMatchChatMessages(Long sessionId, Long userId, int limit) {
        if (sessionId == null || userId == null || !sessionMemberRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            return List.of();
        }
        int size = Math.min(Math.max(1, limit), 100);
        List<MatchChatMessage> list = matchChatMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, size));
        Collections.reverse(list);
        return list.stream().map(msg -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", msg.getId());
            map.put("sessionId", msg.getSessionId());
            map.put("fromUserId", msg.getFromUserId());
            map.put("text", msg.getText());
            map.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().format(ISO) : "");
            userRepository.findById(msg.getFromUserId()).ifPresent(u -> {
                map.put("fromNickname", (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername());
                map.put("fromProfileImageUrl", u.getProfileImageUrl());
            });
            return map;
        }).collect(Collectors.toList());
    }

    /** 매칭 채팅 전송 + STOMP 브로드캐스트 */
    @Transactional
    public MatchChatMessage sendMatchChat(Long sessionId, Long userId, String text) {
        if (sessionId == null || userId == null || !sessionMemberRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) throw new IllegalArgumentException("메시지를 입력해 주세요.");
        if (trimmed.length() > MAX_TEXT_LENGTH) trimmed = trimmed.substring(0, MAX_TEXT_LENGTH);
        ProfanityFilterService.ModerationResult moderation = profanityFilterService.moderateChat(userId, trimmed);

        MatchChatMessage msg = new MatchChatMessage();
        msg.setSessionId(sessionId);
        msg.setFromUserId(userId);
        msg.setText(moderation.getSanitizedText());
        msg = matchChatMessageRepository.save(msg);

        String fromNickname = userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "MESSAGE");
        payload.put("id", msg.getId());
        payload.put("sessionId", msg.getSessionId());
        payload.put("fromUserId", msg.getFromUserId());
        payload.put("fromNickname", fromNickname);
        payload.put("text", msg.getText());
        payload.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().format(ISO) : "");
        messagingTemplate.convertAndSend("/topic/match/" + sessionId, payload);

        /* 상대방에게 채팅 알림 (채팅/알림 드롭다운·토스트 실시간 표시용) */
        String gameName = sessionRepository.findById(sessionId).map(MatchSession::getGame).orElse(null);
        Map<String, Object> notifyPayload = new LinkedHashMap<>();
        notifyPayload.put("type", "MATCH_CHAT_MESSAGE");
        notifyPayload.put("sessionId", sessionId);
        notifyPayload.put("game", gameName);
        notifyPayload.put("fromUserId", userId);
        notifyPayload.put("fromNickname", fromNickname);
        notifyPayload.put("text", msg.getText());
        for (MatchSessionMember m : sessionMemberRepository.findBySessionId(sessionId)) {
            if (!userId.equals(m.getUserId())) {
                messagingTemplate.convertAndSend("/topic/user/" + m.getUserId(), notifyPayload);
            }
        }
        notifyMentionedMembers(sessionId, userId, msg.getText(), gameName);
        return msg;
    }

    public boolean isMatchMember(Long sessionId, Long userId) {
        return sessionId != null && userId != null && sessionMemberRepository.existsBySessionIdAndUserId(sessionId, userId);
    }

    /** 내역 삭제: 참가자만 삭제 가능. 세션·메시지·멤버 삭제 */
    @Transactional
    public boolean deleteSession(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) return false;
        if (!sessionMemberRepository.existsBySessionIdAndUserId(sessionId, userId)) return false;
        matchChatMessageRepository.deleteBySessionId(sessionId);
        sessionMemberRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        return true;
    }

    private void notifyMentionedMembers(Long sessionId, Long fromUserId, String text, String gameName) {
        if (sessionId == null || fromUserId == null || text == null || text.isBlank()) {
            return;
        }
        Map<String, Long> mentionMap = buildMentionMap(
                sessionMemberRepository.findBySessionId(sessionId).stream()
                        .map(MatchSessionMember::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .map(userRepository::findById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
        );
        for (Long mentionedUserId : extractMentionedUserIds(text, mentionMap, fromUserId)) {
            notificationService.createForMatchChatMention(mentionedUserId, fromUserId, sessionId, gameName, text);
        }
    }

    private Map<String, Long> buildMentionMap(List<User> users) {
        Map<String, Long> mentionMap = new LinkedHashMap<>();
        for (User user : users) {
            registerMentionKey(mentionMap, user.getNickname(), user.getId());
            registerMentionKey(mentionMap, user.getLoginId(), user.getId());
            registerMentionKey(mentionMap, user.getUsername(), user.getId());
        }
        return mentionMap;
    }

    private void registerMentionKey(Map<String, Long> mentionMap, String rawKey, Long userId) {
        if (rawKey == null || rawKey.isBlank() || userId == null) {
            return;
        }
        mentionMap.putIfAbsent(rawKey.trim().toLowerCase(Locale.ROOT), userId);
    }

    private Set<Long> extractMentionedUserIds(String text, Map<String, Long> mentionMap, Long fromUserId) {
        Set<Long> result = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            Long mentionedUserId = mentionMap.get(matcher.group(1).trim().toLowerCase(Locale.ROOT));
            if (mentionedUserId != null && !mentionedUserId.equals(fromUserId)) {
                result.add(mentionedUserId);
            }
        }
        return result;
    }
}
