package com.gamematcher.controller;

import com.gamematcher.dto.chat.ChatMessageDto;
import com.gamematcher.dto.stream.CreateStreamRequest;
import com.gamematcher.dto.stream.StreamResponse;
import com.gamematcher.dto.stream.UpdateStreamRequest;
import com.gamematcher.service.ChatService;
import com.gamematcher.service.FollowService;
import com.gamematcher.service.LiveStreamService;
import com.gamematcher.service.RankService;
import com.gamematcher.service.StreamChatSettingsService;
import com.gamematcher.service.SubscriptionService;
import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 라이브 스트림 API.
 * - 스트림 생성 시 OBS에 넣을 서버/스트림 키 발급
 * - nginx-rtmp 콜백 (on_publish / on_publish_done) 처리
 * - 라이브 목록·상세·재생 URL 조회
 */
@RestController
@RequestMapping("/api/streams")
@RequiredArgsConstructor
public class LiveStreamController {

    private static final String SESSION_USER_ID = "userId";

    private final LiveStreamService liveStreamService;
    private final FollowService followService;
    private final RankService rankService;
    private final ChatService chatService;
    private final StreamChatSettingsService streamChatSettingsService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private Long getCurrentUserId(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        return id instanceof Long ? (Long) id : null;
    }

    /**
     * 새 스트림 생성. 로그인한 사용자로 등록됨.
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateStreamRequest request, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(liveStreamService.create(uid, request));
    }

    /**
     * 스트림 정보 수정 (제목, 카테고리). 로그인한 본인 스트림만 수정 가능.
     */
    @PatchMapping("/{streamId}")
    public ResponseEntity<?> update(@PathVariable Long streamId, @Valid @RequestBody UpdateStreamRequest request, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            return ResponseEntity.ok(liveStreamService.update(streamId, uid, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * OBS Studio 설정에 필요한 서버 URL과 스트림 키.
     */
    @GetMapping("/{streamId}/obs-setup")
    public ResponseEntity<?> getObsSetup(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(liveStreamService.getObsSetup(streamId, uid));
    }

    /**
     * 스트림 키 재발급. 기존 OBS 설정은 무효화되며 새 키를 입력해야 함.
     */
    @PostMapping("/{streamId}/regenerate-stream-key")
    public ResponseEntity<?> regenerateStreamKey(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            String newKey = liveStreamService.regenerateStreamKey(streamId, uid);
            return ResponseEntity.ok(Map.of("streamKey", newKey));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 최근 방송 목록 (스트리밍 메인 페이지용). ?limit=20 기본.
     */
    @GetMapping
    public ResponseEntity<List<StreamResponse>> listRecent(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(liveStreamService.listRecent(Math.min(limit, 50)));
    }

    /**
     * 로그인한 사용자의 방송 목록 (내 채널 페이지용).
     */
    @GetMapping("/by-user/me")
    public ResponseEntity<List<StreamResponse>> listMyStreams(HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(liveStreamService.listByUser(uid, uid));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<StreamResponse>> listUserStreams(@PathVariable Long userId, HttpSession session) {
        return ResponseEntity.ok(liveStreamService.listByUser(userId, getCurrentUserId(session)));
    }

    @PatchMapping("/{streamId}/channel-visibility")
    public ResponseEntity<?> updateChannelVisibility(@PathVariable Long streamId,
                                                     @RequestBody Map<String, Boolean> body,
                                                     HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            boolean visibleInRecent = body == null || !Boolean.FALSE.equals(body.get("visibleInRecent"));
            return ResponseEntity.ok(liveStreamService.updateVisibility(streamId, uid, visibleInRecent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{streamId}/channel")
    public ResponseEntity<?> deleteFromChannel(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            liveStreamService.deleteFromChannel(streamId, uid);
            return ResponseEntity.ok(Map.of("message", "채널에서 영상을 삭제했습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 현재 라이브 중인 스트림 목록.
     */
    @GetMapping("/live")
    public ResponseEntity<List<StreamResponse>> listLive() {
        return ResponseEntity.ok(liveStreamService.listLive());
    }

    /**
     * 스트림 상세 (재생 URL 포함). 시청자 수·방송 시작 시각 포함.
     */
    @GetMapping("/{streamId}")
    public ResponseEntity<StreamResponse> get(@PathVariable Long streamId) {
        return ResponseEntity.ok(liveStreamService.getById(streamId));
    }

    /**
     * 주간 후원 랭킹 (채팅창 상단용). 이번 주 월요일 0시 이후 해당 방송에 후원한 금액 순.
     */
    @GetMapping("/{streamId}/weekly-donor-rank")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyDonorRank(@PathVariable Long streamId) {
        return ResponseEntity.ok(rankService.getWeeklyDonationRankByStream(streamId));
    }

    /**
     * 해당 방송에 후원한 적 있는 사용자 ID 목록 (채팅창에서 후원자 닉네임 색상 표시용).
     */
    @GetMapping("/{streamId}/donors")
    public ResponseEntity<List<Long>> getStreamDonors(@PathVariable Long streamId) {
        return ResponseEntity.ok(rankService.getStreamDonorUserIds(streamId));
    }

    /**
     * 해당 방송의 스트리머를 구독한 사용자 ID 목록 (채팅창 구독 뱃지용).
     */
    @GetMapping("/{streamId}/subscriber-ids")
    public ResponseEntity<List<Long>> getStreamSubscriberIds(@PathVariable Long streamId) {
        Long streamerId = liveStreamService.getById(streamId).getUserId();
        if (streamerId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(subscriptionService.getSubscriberIds(streamerId));
    }

    /**
     * 해당 방송 최근 채팅 내역 (새로고침 시 복원용). ?limit=100 기본.
     */
    @GetMapping("/{streamId}/chat")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long streamId,
                                                               @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(chatService.getRecentMessages(streamId, limit));
    }

    /**
     * 채팅 + 후원 통합 타임라인 (새로고침 시 후원 내역도 복원).
     */
    @GetMapping("/{streamId}/chat-timeline")
    public ResponseEntity<List<Map<String, Object>>> getChatTimeline(@PathVariable Long streamId,
                                                                      @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(chatService.getRecentTimeline(streamId, limit));
    }

    /**
     * 채팅 설정 조회 (얼리기, BJ 공지).
     */
    @GetMapping("/{streamId}/chat-settings")
    public ResponseEntity<?> getChatSettings(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        StreamResponse stream = liveStreamService.getById(streamId);
        boolean isOwner = uid != null && stream.getUserId() != null && uid.equals(stream.getUserId());
        boolean isManager = !isOwner && uid != null && streamChatSettingsService.isManager(streamId, uid);
        return ResponseEntity.ok(Map.of(
                "chatFrozen", stream.getChatFrozen() != null ? stream.getChatFrozen() : false,
                "streamNotice", stream.getStreamNotice() != null ? stream.getStreamNotice() : "",
                "streamNoticeVisible", stream.getStreamNoticeVisible() != null ? stream.getStreamNoticeVisible() : false,
                "minVideoPang", stream.getMinVideoPang() != null ? stream.getMinVideoPang() : 0,
                "minTtsPang", stream.getMinTtsPang() != null ? stream.getMinTtsPang() : 0,
                "isOwner", isOwner,
                "isManager", isManager
        ));
    }

    /**
     * 채팅 얼리기 설정 (방장만).
     */
    @PutMapping("/{streamId}/chat-settings/freeze")
    public ResponseEntity<?> setChatFreeze(@PathVariable Long streamId, @RequestBody Map<String, Boolean> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            boolean frozen = body != null && Boolean.TRUE.equals(body.get("frozen"));
            streamChatSettingsService.setChatFrozen(streamId, uid, frozen);
            broadcastChatSettings(streamId);
            return ResponseEntity.ok(Map.of("chatFrozen", frozen));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * BJ 공지 등록/수정 (방장만).
     */
    @PutMapping("/{streamId}/chat-settings/notice")
    public ResponseEntity<?> setStreamNotice(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            String text = body != null && body.get("text") != null ? body.get("text").toString().trim() : null;
            Boolean visible = body != null && body.containsKey("visible") ? Boolean.TRUE.equals(body.get("visible")) : null;
            streamChatSettingsService.setStreamNotice(streamId, uid, text, visible);
            broadcastChatSettings(streamId);
            return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 영상 후원 / TTS 최소 팡 설정 (방장만).
     */
    @PutMapping("/{streamId}/chat-settings/donation-limits")
    public ResponseEntity<?> setDonationLimits(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            Integer minVideoPang = body != null && body.containsKey("minVideoPang")
                    ? parseInt(body.get("minVideoPang"), 0) : null;
            Integer minTtsPang = body != null && body.containsKey("minTtsPang")
                    ? parseInt(body.get("minTtsPang"), 0) : null;
            streamChatSettingsService.setDonationLimits(streamId, uid, minVideoPang, minTtsPang);
            StreamResponse updated = liveStreamService.getById(streamId);
            return ResponseEntity.ok(Map.of("message", "저장되었습니다.", "minVideoPang", updated.getMinVideoPang() != null ? updated.getMinVideoPang() : 0, "minTtsPang", updated.getMinTtsPang() != null ? updated.getMinTtsPang() : 0));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private static Integer parseInt(Object o, int defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void broadcastChatSettings(Long streamId) {
        try {
            StreamResponse stream = liveStreamService.getById(streamId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "chatSettings");
            payload.put("chatFrozen", stream.getChatFrozen() != null ? stream.getChatFrozen() : false);
            payload.put("streamNotice", stream.getStreamNotice() != null ? stream.getStreamNotice() : "");
            payload.put("streamNoticeVisible", stream.getStreamNoticeVisible() != null ? stream.getStreamNoticeVisible() : false);
            messagingTemplate.convertAndSend("/topic/stream/" + streamId, payload);
        } catch (Exception ignored) { }
    }

    /** 채팅창에 시스템 안내 문구 브로드캐스트 (매니저 임명, 채팅 금지/해제 등). */
    private void broadcastSystemChat(Long streamId, String text) {
        if (streamId == null || text == null || text.isBlank()) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "system");
            payload.put("text", text.trim());
            messagingTemplate.convertAndSend("/topic/stream/" + streamId, payload);
        } catch (Exception ignored) { }
    }

    private String getDisplayName(Long userId) {
        if (userId == null) return "알 수 없음";
        return userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("알 수 없음");
    }

    /**
     * 채팅 금지 사용자 목록 (방장만).
     */
    @GetMapping("/{streamId}/chat/bans")
    public ResponseEntity<?> getChatBans(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            return ResponseEntity.ok(streamChatSettingsService.getBannedList(streamId, uid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 채팅 금지 추가 (방장/매니저). body: userId 또는 loginId, durationMinutes(선택, null=영구, 5=5분).
     */
    @PostMapping("/{streamId}/chat/ban")
    public ResponseEntity<?> addChatBan(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (body == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 아이디를 입력해 주세요."));
        Integer durationMinutes = null;
        Object dm = body.get("durationMinutes");
        if (dm instanceof Number) durationMinutes = ((Number) dm).intValue();
        else if (dm != null) try { durationMinutes = Integer.parseInt(dm.toString()); } catch (NumberFormatException ignored) {}
        Object loginIdObj = body.get("loginId");
        String loginId = (loginIdObj != null && loginIdObj.toString().trim().length() > 0) ? loginIdObj.toString().trim() : null;
        if (loginId != null) {
            try {
                Long targetUserId = userRepository.findByLoginId(loginId.trim()).map(User::getId).orElse(null);
                streamChatSettingsService.addChatBanByLoginId(streamId, uid, loginId, durationMinutes);
                if (targetUserId != null) broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 채팅 금지되었습니다.");
                return ResponseEntity.ok(Map.of("message", durationMinutes != null ? durationMinutes + "분 채팅 금지 처리되었습니다." : "채팅 금지 처리되었습니다."));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
        }
        Object targetObj = body.get("userId");
        if (targetObj == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 아이디를 입력해 주세요."));
        Long targetUserId;
        try {
            targetUserId = targetObj instanceof Number ? ((Number) targetObj).longValue() : Long.parseLong(targetObj.toString().trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID는 숫자이거나, 로그인 아이디(문자)를 입력해 주세요."));
        }
        if (targetUserId < 1) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID는 1 이상이어야 합니다."));
        try {
            streamChatSettingsService.addChatBan(streamId, uid, targetUserId, durationMinutes);
            broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 채팅 금지되었습니다.");
            return ResponseEntity.ok(Map.of("message", durationMinutes != null ? durationMinutes + "분 채팅 금지 처리되었습니다." : "채팅 금지 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 채팅 금지 해제 (방장/매니저).
     */
    @DeleteMapping("/{streamId}/chat/ban/{targetUserId}")
    public ResponseEntity<?> removeChatBan(@PathVariable Long streamId, @PathVariable Long targetUserId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            String displayName = getDisplayName(targetUserId);
            streamChatSettingsService.removeChatBan(streamId, uid, targetUserId);
            broadcastSystemChat(streamId, displayName + "님의 채팅 금지가 풀렸습니다.");
            return ResponseEntity.ok(Map.of("message", "해제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 블랙리스트 목록 (방장/매니저).
     */
    @GetMapping("/{streamId}/chat/blacklist")
    public ResponseEntity<?> getBlacklist(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            return ResponseEntity.ok(streamChatSettingsService.getBlacklist(streamId, uid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 블랙리스트 추가 (방장/매니저). body: userId 또는 loginId.
     */
    @PostMapping("/{streamId}/chat/blacklist")
    public ResponseEntity<?> addBlacklist(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (body == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 아이디를 입력해 주세요."));
        Object loginIdObj = body.get("loginId");
        String loginId = (loginIdObj != null && loginIdObj.toString().trim().length() > 0) ? loginIdObj.toString().trim() : null;
        if (loginId != null) {
            Long targetUserId = userRepository.findByLoginId(loginId.trim())
                    .map(u -> u.getId())
                    .orElse(null);
            if (targetUserId == null) return ResponseEntity.badRequest().body(Map.of("message", "해당 로그인 아이디의 사용자를 찾을 수 없습니다."));
            try {
                streamChatSettingsService.addBlacklist(streamId, uid, targetUserId);
                return ResponseEntity.ok(Map.of("message", "블랙리스트에 추가되었습니다. 해당 사용자는 방송 시청이 불가합니다."));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
        }
        Object targetObj = body.get("userId");
        if (targetObj == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 아이디를 입력해 주세요."));
        Long targetUserId = parseLong(targetObj);
        if (targetUserId == null || targetUserId < 1) return ResponseEntity.badRequest().body(Map.of("message", "유효한 사용자 ID를 입력해 주세요."));
        try {
            streamChatSettingsService.addBlacklist(streamId, uid, targetUserId);
            return ResponseEntity.ok(Map.of("message", "블랙리스트에 추가되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{streamId}/chat/blacklist/{targetUserId}")
    public ResponseEntity<?> removeBlacklist(@PathVariable Long streamId, @PathVariable Long targetUserId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            streamChatSettingsService.removeBlacklist(streamId, uid, targetUserId);
            return ResponseEntity.ok(Map.of("message", "블랙리스트에서 제거되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 매니저 목록 (방장만).
     */
    @GetMapping("/{streamId}/chat/managers")
    public ResponseEntity<?> getManagers(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            return ResponseEntity.ok(streamChatSettingsService.getManagers(streamId, uid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 매니저 지정 (방장만). body: userId 또는 loginId.
     */
    @PostMapping("/{streamId}/chat/managers")
    public ResponseEntity<?> addManager(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (body == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 아이디를 입력해 주세요."));
        Object loginIdObj = body.get("loginId");
        String loginId = (loginIdObj != null && loginIdObj.toString().trim().length() > 0) ? loginIdObj.toString().trim() : null;
        if (loginId != null) {
            Long targetUserId = userRepository.findByLoginId(loginId.trim()).map(User::getId).orElse(null);
            if (targetUserId == null) return ResponseEntity.badRequest().body(Map.of("message", "해당 로그인 아이디의 사용자를 찾을 수 없습니다."));
            try {
                streamChatSettingsService.addManager(streamId, uid, targetUserId);
                broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 매니저가 되었습니다.");
                return ResponseEntity.ok(Map.of("message", "매니저로 지정되었습니다."));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
        }
        Object targetObj = body.get("userId");
        if (targetObj == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 아이디를 입력해 주세요."));
        Long targetUserId = parseLong(targetObj);
        if (targetUserId == null || targetUserId < 1) return ResponseEntity.badRequest().body(Map.of("message", "유효한 사용자 ID를 입력해 주세요."));
        try {
            streamChatSettingsService.addManager(streamId, uid, targetUserId);
            broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 매니저가 되었습니다.");
            return ResponseEntity.ok(Map.of("message", "매니저로 지정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{streamId}/chat/managers/{targetUserId}")
    public ResponseEntity<?> removeManager(@PathVariable Long streamId, @PathVariable Long targetUserId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            streamChatSettingsService.removeManager(streamId, uid, targetUserId);
            return ResponseEntity.ok(Map.of("message", "매니저가 해제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private static Long parseLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * 시청 페이지 진입 시 호출. 블랙리스트 사용자는 403. 로그인한 사용자만 집계.
     */
    @PostMapping("/{streamId}/viewer/join")
    public ResponseEntity<?> viewerJoin(@PathVariable Long streamId, HttpSession session) {
        if (!liveStreamService.existsAndLive(streamId)) {
            return ResponseEntity.notFound().build();
        }
        Long userId = getCurrentUserId(session);
        if (userId != null) {
            if (streamChatSettingsService.isBlacklisted(streamId, userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "이 방송 시청이 제한된 계정입니다."));
            }
            liveStreamService.viewerJoin(streamId, userId);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 시청 페이지 이탈 시 호출.
     */
    @PostMapping("/{streamId}/viewer/leave")
    public ResponseEntity<?> viewerLeave(@PathVariable Long streamId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId != null) liveStreamService.viewerLeave(streamId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 스트림 목록.
     */
    @GetMapping("/my")
    public ResponseEntity<?> listMy(HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(liveStreamService.listByUser(uid));
    }

    /**
     * 팔로잉한 채널의 방송 목록. type=live | recent
     */
    @GetMapping("/following")
    public ResponseEntity<?> listFollowing(@RequestParam(defaultValue = "all") String type, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<Long> followingIds = followService.getFollowingUserIds(uid);
        if (followingIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<?> list;
        if ("live".equalsIgnoreCase(type)) {
            list = liveStreamService.listLiveByUserIds(followingIds);
        } else if ("recent".equalsIgnoreCase(type)) {
            list = liveStreamService.listRecentByUserIds(followingIds, 20);
        } else {
            List<StreamResponse> live = liveStreamService.listLiveByUserIds(followingIds);
            List<StreamResponse> recent = liveStreamService.listRecentByUserIds(followingIds, 20);
            Set<Long> liveIds = new HashSet<>();
            for (StreamResponse s : live) liveIds.add(s.getId());
            List<StreamResponse> all = new ArrayList<>(live);
            for (StreamResponse s : recent) {
                if (!liveIds.contains(s.getId())) all.add(s);
            }
            list = all;
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 방송자가 수동으로 방송 종료 처리. OBS 종료 시 RTMP에서 notify/end가 호출되지 않았을 때 사용.
     */
    @PutMapping("/{streamId}/end")
    public ResponseEntity<?> endStream(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            liveStreamService.endStreamByOwner(streamId, uid);
            return ResponseEntity.ok(Map.of("message", "방송이 종료 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ----- nginx-rtmp 콜백 (application/x-www-form-urlencoded, name=스트림키) -----

    /**
     * nginx-rtmp on_publish 콜백.
     * 2xx 반환 시 퍼블리시 허용, 그 외 거부.
     */
    @PostMapping("/notify/live")
    public ResponseEntity<Void> notifyLive(@RequestParam("name") String streamKey) {
        if (!liveStreamService.isValidStreamKey(streamKey)) {
            return ResponseEntity.status(403).build();
        }
        liveStreamService.notifyLive(streamKey);
        return ResponseEntity.ok().build();
    }

    /**
     * nginx-rtmp on_publish_done 콜백.
     */
    @PostMapping("/notify/end")
    public ResponseEntity<Void> notifyEnd(@RequestParam("name") String streamKey) {
        liveStreamService.notifyEnd(streamKey);
        return ResponseEntity.ok().build();
    }
}
