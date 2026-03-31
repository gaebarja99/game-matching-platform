package com.gamematcher.controller;

import com.gamematcher.dto.chat.ChatMessageDto;
import com.gamematcher.dto.stream.CreateStreamRequest;
import com.gamematcher.dto.stream.StreamResponse;
import com.gamematcher.dto.stream.UpdateStreamRequest;
import com.gamematcher.service.ChatService;
import com.gamematcher.service.FollowService;
import com.gamematcher.service.LiveStreamService;
import com.gamematcher.service.RankService;
import com.gamematcher.service.StreamViewerCountService;
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
 * ??????롮쾸?椰??????????濚밸Ŧ援???API.
 * - ??????濚밸Ŧ援?????????썹땟戮녹??????OBS???????????????饔낅떽???????鶯???????濚밸Ŧ援??????????諛몃마嶺뚮??????ル늉筌??
 * - nginx-rtmp ?????獄쏅챶留덌┼????????(on_publish / on_publish_done) ??遺얘턁????傭?끆???嶺뚮?猷볠꽴??
 * - ??????롮쾸?椰??????遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉??????????嶺뚮ㅎ?볠꽴???????URL ?????Β??????
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
    private final StreamViewerCountService streamViewerCountService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private Long getCurrentUserId(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        return id instanceof Long ? (Long) id : null;
    }

    /**
     * ????????濚밸Ŧ援?????????썹땟戮녹???? ????????耀붾굝????癲ル슢??㎖?밤뀋?????????????????濡?씀?濾????ㅼ굡????
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateStreamRequest request, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        }
        return ResponseEntity.ok(liveStreamService.create(uid, request));
    }

    /**
     * ??????濚밸Ŧ援?????耀붾굝??????????????怨뚮뼺?됰뗀???(???饔낅떽??????猷고ｇ땟??? ?????紐껊괘???????븍툖??????????. ????????耀붾굝????癲ル슢??㎖?밤뀋?????????怨뺤른?癲ル슢???????????濚밸Ŧ援??????롮쾸?椰?????類ㅻ쭓???????怨뚮뼺?됰뗀?????????ル뭽????
     */
    @PatchMapping("/{streamId}")
    public ResponseEntity<?> update(@PathVariable Long streamId, @Valid @RequestBody UpdateStreamRequest request, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        }
        try {
            return ResponseEntity.ok(liveStreamService.update(streamId, uid, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * OBS Studio ????關?쒎첎?嫄??????????꾩룆梨띰쭕??????饔낅떽???????鶯?URL????????濚밸Ŧ援?????
     */
    @GetMapping("/{streamId}/obs-setup")
    public ResponseEntity<?> getObsSetup(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        }
        return ResponseEntity.ok(liveStreamService.getObsSetup(streamId, uid));
    }

    /**
     * ??????濚밸Ŧ援????????????? ????????OBS ????關?쒎첎?嫄???? ????꿔꺂??틝???????濾?????룸ℓ????????? ????????⑤뜪輿??????繹먮굞彛????
     */
    @PostMapping("/{streamId}/regenerate-stream-key")
    public ResponseEntity<?> regenerateStreamKey(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        }
        try {
            String newKey = liveStreamService.regenerateStreamKey(streamId, uid);
            return ResponseEntity.ok(Map.of("streamKey", newKey));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * ??遺얘턁??????傭???????諛몃마??潁뺛깺苡????遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉???(??????濚밸Ŧ援???????좊틣??釉랁닕????????遺얘턁????????????怨뚮뼺?됰뗀?????????????. ?limit=20 ????????
     */
    @GetMapping
    public ResponseEntity<List<StreamResponse>> listRecent(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(liveStreamService.listRecent(Math.min(limit, 50)));
    }

    /**
     * ????????耀붾굝????癲ル슢??㎖?밤뀋?????????????????諛몃마??潁뺛깺苡????遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉???(?????????????怨뚮뼺?됰뗀?????????????.
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
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
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
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        }
        try {
            liveStreamService.deleteFromChannel(streamId, uid);
            return ResponseEntity.ok(Map.of("message", "???????????????嶺뚮ㅎ?볠꽴???????????????????怨몄）."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * ??????꾩룆梨띰쭕????????롮쾸?椰?????????熬곣벀嫄?????꾤뙴?????????濚밸Ŧ援?????遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉???
     */
    @GetMapping("/live")
    public ResponseEntity<List<StreamResponse>> listLive() {
        return ResponseEntity.ok(liveStreamService.listLive());
    }

    /**
     * ??????濚밸Ŧ援????????嶺뚮ㅎ?볠꽴??(????URL ????. ??????????饔낅떽????????룸챷援ｅㅇ?????饔낅떽????鶯ㅺ동???嫄???????????
     */
    @GetMapping("/{streamId}")
    public ResponseEntity<StreamResponse> get(@PathVariable Long streamId) {
        return ResponseEntity.ok(liveStreamService.getById(streamId));
    }

    /**
     * ??????諛몄カ?????댁뢿援????????꾩룆梨띰쭕??????(??????轅붽틓??섑떊???⑤짅嫄??????濚밸Ŧ援????. ???????????濾????0????????獄쏅챶留????????????諛몃마??潁뺛깺苡?????????꾩룆梨띰쭕????????援쏂굜??????????
     */
    @GetMapping("/{streamId}/weekly-donor-rank")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyDonorRank(@PathVariable Long streamId) {
        return ResponseEntity.ok(rankService.getWeeklyDonationRankByStream(streamId));
    }

    /**
     * ??????????諛몃마??潁뺛깺苡?????????꾩룆梨띰쭕????????????????????ID ??遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉???(??????轅붽틓??섑떊???⑤짅嫄????????????꾩룆梨띰쭕????????????????????롮쾸????怨뚮옩?戮녹춹????????.
     */
    @GetMapping("/{streamId}/donors")
    public ResponseEntity<List<Long>> getStreamDonors(@PathVariable Long streamId) {
        return ResponseEntity.ok(rankService.getStreamDonorUserIds(streamId));
    }

    /**
     * ??????????諛몃마??潁뺛깺苡?????????濚밸Ŧ援???????좊틣??釉랁닕?????????鶯ㅺ동????ル쭔???????ID ??遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉???(??????轅붽틓??섑떊???⑤짅嫄??????鶯ㅺ동????ル쭔???????.
     */
    @GetMapping("/{streamId}/subscriber-ids")
    public ResponseEntity<List<Long>> getStreamSubscriberIds(@PathVariable Long streamId) {
        Long streamerId = liveStreamService.getById(streamId).getUserId();
        if (streamerId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(subscriptionService.getSubscriberIds(streamerId));
    }

    /**
     * ??????????諛몃마??潁뺛깺苡????遺얘턁??????傭?????????????繹먮굞???(????????????????????怨뺤른?癲ル슢??????. ?limit=100 ????????
     */
    @GetMapping("/{streamId}/chat")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long streamId,
                                                               @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(chatService.getRecentMessages(streamId, limit));
    }

    /**
     * ?????+ ??????꾩룆梨띰쭕?????? ????????꾩룆梨띰쭕??影??맜???(?????????????????????꾩룆梨띰쭕????????繹먮굞?????????怨뺤른?癲ル슢?????.
     */
    @GetMapping("/{streamId}/chat-timeline")
    public ResponseEntity<List<Map<String, Object>>> getChatTimeline(@PathVariable Long streamId,
                                                                      @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(chatService.getRecentTimeline(streamId, limit));
    }

    /**
     * ?????????關?쒎첎?嫄????????Β??????(????棺堉?뤃??믡굦???? BJ ???????ㅿ폎??).
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
     * ?????????棺堉?뤃??믡굦????????關?쒎첎?嫄???(?????諛몃마??潁뺛깺苡?????????곌떽釉붾??.
     */
    @PutMapping("/{streamId}/chat-settings/freeze")
    public ResponseEntity<?> setChatFreeze(@PathVariable Long streamId, @RequestBody Map<String, Boolean> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        try {
            boolean frozen = body != null && Boolean.TRUE.equals(body.get("frozen"));
            streamChatSettingsService.setChatFrozen(streamId, uid, frozen);
            broadcastChatSettings(streamId);
            broadcastSystemChat(streamId, frozen ? "채팅창이 얼려졌습니다. 팬, 매니저, 방송자만 채팅할 수 있습니다." : "채팅창이 녹았습니다. 모두가 채팅할 수 있습니다.");
            return ResponseEntity.ok(Map.of("chatFrozen", frozen));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * BJ ???????ㅿ폎?? ?????濡?씀?濾????ㅼ굡????????怨뚮뼺?됰뗀???(?????諛몃마??潁뺛깺苡?????????곌떽釉붾??.
     */
    @PutMapping("/{streamId}/chat-settings/notice")
    public ResponseEntity<?> setStreamNotice(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        try {
            String text = body != null && body.get("text") != null ? body.get("text").toString().trim() : null;
            Boolean visible = body != null && body.containsKey("visible") ? Boolean.TRUE.equals(body.get("visible")) : null;
            streamChatSettingsService.setStreamNotice(streamId, uid, text, visible);
            broadcastChatSettings(streamId);
            return ResponseEntity.ok(Map.of("message", "???????볧뀮???됀???亦낃콛??????????????怨몄）."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * ?????嶺뚮ㅎ?볠꽴????????꾩룆梨띰쭕??/ TTS ??遺얘턁??????傭????????關?쒎첎?嫄???(?????諛몃마??潁뺛깺苡?????????곌떽釉붾??.
     */
    @PutMapping("/{streamId}/chat-settings/donation-limits")
    public ResponseEntity<?> setDonationLimits(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        try {
            Integer minVideoPang = body != null && body.containsKey("minVideoPang")
                    ? parseInt(body.get("minVideoPang"), 0) : null;
            Integer minTtsPang = body != null && body.containsKey("minTtsPang")
                    ? parseInt(body.get("minTtsPang"), 0) : null;
            streamChatSettingsService.setDonationLimits(streamId, uid, minVideoPang, minTtsPang);
            StreamResponse updated = liveStreamService.getById(streamId);
            return ResponseEntity.ok(Map.of("message", "???????볧뀮???됀???亦낃콛??????????????怨몄）.", "minVideoPang", updated.getMinVideoPang() != null ? updated.getMinVideoPang() : 0, "minTtsPang", updated.getMinTtsPang() != null ? updated.getMinTtsPang() : 0));
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

    /** ??????轅붽틓??섑떊???⑤짅嫄????????????????? ??????????????????????饔낅떽????????????濚밸Ŧ援??(??遺얘턁???????? ??????꾩룆梨띰쭕????蹂ｍ닧? ??????????援쏂굜????/????????살몝????. */
    private void broadcastSystemChat(Long streamId, String text) {
        if (streamId == null || text == null || text.isBlank()) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "system");
            payload.put("text", text.trim());
            messagingTemplate.convertAndSend("/topic/stream/" + streamId, payload);
        } catch (Exception ignored) { }
    }

    private void broadcastKickEvent(Long streamId, Long targetUserId, String message) {
        if (streamId == null || targetUserId == null) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "kick");
            payload.put("targetUserId", targetUserId);
            payload.put("message", message != null && !message.isBlank() ? message : "揶쏅벡???곸삢 ?諭곷릭??λ뮸??덈뼄. 5????덈툧 ????館釉?????곷뮸??덈뼄.");
            payload.put("redirectUrl", "/streams");
            messagingTemplate.convertAndSend("/topic/stream/" + streamId, payload);
        } catch (Exception ignored) { }
    }

    private String getDisplayName(Long userId) {
        if (userId == null) return "??????곸벉";
        return userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("??????곸벉");
    }

    @GetMapping("/{streamId}/chat/bans")
    public ResponseEntity<?> getChatBans(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        try {
            return ResponseEntity.ok(streamChatSettingsService.getBannedList(streamId, uid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{streamId}/chat/ban")
    public ResponseEntity<?> addChatBan(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (body == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 ID를 입력해 주세요."));
        Integer durationMinutes = null;
        Object dm = body.get("durationMinutes");
        if (dm instanceof Number) durationMinutes = ((Number) dm).intValue();
        else if (dm != null) try { durationMinutes = Integer.parseInt(dm.toString()); } catch (NumberFormatException ignored) {}
        Object loginIdObj = body.get("loginId");
        String loginId = (loginIdObj != null && !loginIdObj.toString().trim().isEmpty()) ? loginIdObj.toString().trim() : null;
        if (loginId != null) {
            try {
                Long targetUserId = userRepository.findByLoginId(loginId.trim())
                        .or(() -> userRepository.findByNickname(loginId.trim()))
                        .map(User::getId)
                        .orElse(null);
                streamChatSettingsService.addChatBanByLoginId(streamId, uid, loginId, durationMinutes);
                if (targetUserId != null) {
                    broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 1분 채팅금지되었습니다.");
                }
                return ResponseEntity.ok(Map.of("message", durationMinutes != null ? durationMinutes + "분 채팅금지 처리되었습니다." : "채팅금지 처리되었습니다."));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
        }
        Object targetObj = body.get("userId");
        if (targetObj == null) return ResponseEntity.badRequest().body(Map.of("message", "?????ID ??????癲??嶺????????諛몃마??維◈??? ??????ㅼ굣塋???????⑹름??????뭽??"));
        Long targetUserId = parseLong(targetObj);
        if (targetUserId == null || targetUserId < 1) return ResponseEntity.badRequest().body(Map.of("message", "??????????????ID????????ㅼ굣塋???????⑹름??????뭽??"));
        try {
            streamChatSettingsService.addChatBan(streamId, uid, targetUserId, durationMinutes);
            broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 1분 채팅금지되었습니다.");
            return ResponseEntity.ok(Map.of("message", durationMinutes != null ? durationMinutes + "분 채팅금지 처리되었습니다." : "채팅금지 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{streamId}/chat/ban/{targetUserId}")
    public ResponseEntity<?> removeChatBan(@PathVariable Long streamId, @PathVariable Long targetUserId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        try {
            String displayName = getDisplayName(targetUserId);
            streamChatSettingsService.removeChatBan(streamId, uid, targetUserId);
            broadcastSystemChat(streamId, displayName + "님의 채팅금지가 해제되었습니다.");
            return ResponseEntity.ok(Map.of("message", "채팅금지가 해제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{streamId}/chat/blacklist")
    public ResponseEntity<?> getBlacklist(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        try {
            return ResponseEntity.ok(streamChatSettingsService.getBlacklist(streamId, uid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{streamId}/chat/blacklist")
    public ResponseEntity<?> addBlacklist(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        if (body == null) return ResponseEntity.badRequest().body(Map.of("message", "?????ID ??????癲??嶺????????諛몃마??維◈??? ??????ㅼ굣塋???????⑹름??????뭽??"));
        Object loginIdObj = body.get("loginId");
        String loginId = (loginIdObj != null && !loginIdObj.toString().trim().isEmpty()) ? loginIdObj.toString().trim() : null;
        Long targetUserId = null;
        if (loginId != null) {
            targetUserId = userRepository.findByLoginId(loginId.trim())
                    .or(() -> userRepository.findByNickname(loginId.trim()))
                    .map(User::getId)
                    .orElse(null);
            if (targetUserId == null) return ResponseEntity.badRequest().body(Map.of("message", "입력한 닉네임 또는 로그인 ID에 해당하는 사용자를 찾을 수 없습니다."));
        } else {
            Object targetObj = body.get("userId");
            if (targetObj == null) return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID 또는 로그인 ID를 입력해 주세요."));
            targetUserId = parseLong(targetObj);
            if (targetUserId == null || targetUserId < 1) return ResponseEntity.badRequest().body(Map.of("message", "유효한 사용자 ID를 입력해 주세요."));
        }
        try {
            streamChatSettingsService.addBlacklist(streamId, uid, targetUserId);
            liveStreamService.viewerLeave(streamId, targetUserId);
            broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 블랙리스트에 추가되었습니다.");
            broadcastKickEvent(streamId, targetUserId, "블랙리스트에 등록되어 방송에 참여할 수 없습니다.");
            return ResponseEntity.ok(Map.of("message", "블랙리스트에 추가했습니다."));
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
            return ResponseEntity.ok(Map.of("message", "블랙리스트를 해제했습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{streamId}/chat/managers")
    public ResponseEntity<?> getManagers(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        try {
            return ResponseEntity.ok(streamChatSettingsService.getManagers(streamId, uid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{streamId}/chat/managers")
    public ResponseEntity<?> addManager(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        if (body == null) return ResponseEntity.badRequest().body(Map.of("message", "?????ID ??????癲??嶺????????諛몃마??維◈??? ??????ㅼ굣塋???????⑹름??????뭽??"));
        Object loginIdObj = body.get("loginId");
        String loginId = (loginIdObj != null && !loginIdObj.toString().trim().isEmpty()) ? loginIdObj.toString().trim() : null;
        Long targetUserId = null;
        if (loginId != null) {
            targetUserId = userRepository.findByLoginId(loginId.trim())
                    .or(() -> userRepository.findByNickname(loginId.trim()))
                    .map(User::getId)
                    .orElse(null);
            if (targetUserId == null) return ResponseEntity.badRequest().body(Map.of("message", "??????癲??嶺????????諛몃마??維◈?????????????곕?癲?????諛몃마???뀀탿??????? ?饔낅떽???????????????깅즽????????놁졄."));
        } else {
            Object targetObj = body.get("userId");
            if (targetObj == null) return ResponseEntity.badRequest().body(Map.of("message", "?????ID ??????癲??嶺????????諛몃마??維◈??? ??????ㅼ굣塋???????⑹름??????뭽??"));
            targetUserId = parseLong(targetObj);
            if (targetUserId == null || targetUserId < 1) return ResponseEntity.badRequest().body(Map.of("message", "??????????????ID????????ㅼ굣塋???????⑹름??????뭽??"));
        }
        try {
            streamChatSettingsService.addManager(streamId, uid, targetUserId);
            broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님이 매니저가 되었습니다.");
            return ResponseEntity.ok(Map.of("message", "매니저 권한이 부여되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{streamId}/chat/managers/{targetUserId}")
    public ResponseEntity<?> removeManager(@PathVariable Long streamId, @PathVariable Long targetUserId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        try {
            streamChatSettingsService.removeManager(streamId, uid, targetUserId);
            broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님의 매니저 권한이 해제되었습니다.");
            return ResponseEntity.ok(Map.of("message", "매니저 권한이 해제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{streamId}/chat/kick")
    public ResponseEntity<?> kickViewer(@PathVariable Long streamId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "?癲??嶺???轅붽틓?????됱깢???????諛몃마????꿔꺂??????"));
        Long targetUserId = parseLong(body != null ? body.get("userId") : null);
        if (targetUserId == null || targetUserId < 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "??????????????ID????????ㅼ굣塋???????⑹름??????뭽??"));
        }
        try {
            streamChatSettingsService.addChatBan(streamId, uid, targetUserId, 5);
            liveStreamService.viewerLeave(streamId, targetUserId);
            broadcastSystemChat(streamId, getDisplayName(targetUserId) + "님의 재입장이 5분 동안 제한되었습니다.");
            broadcastKickEvent(streamId, targetUserId, "강제퇴장 당하셨습니다. 5분 동안 재입장할 수 없습니다.");
            return ResponseEntity.ok(Map.of("message", "5분 강제퇴장 처리되었습니다.", "durationMinutes", 5));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{streamId}/viewers")
    public ResponseEntity<?> getViewers(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));

        StreamResponse stream = liveStreamService.getById(streamId);
        boolean isOwner = stream.getUserId() != null && stream.getUserId().equals(uid);
        boolean isManager = !isOwner && streamChatSettingsService.isManager(streamId, uid);
        if (!isOwner && !isManager) {
            return ResponseEntity.status(403).body(Map.of("message", "??????????????繹먮굞??????????怨몄）."));
        }

        Set<Long> donorSet = new HashSet<>(rankService.getStreamDonorUserIds(streamId));
        Set<Long> managerSet = streamChatSettingsService.getManagers(streamId, stream.getUserId()).stream()
                .map(row -> parseLong(row.get("userId")))
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        List<Map<String, Object>> viewers = streamViewerCountService.getViewerUserIds(streamId).stream()
                .map(viewerId -> userRepository.findById(viewerId)
                        .map(user -> {
                            Map<String, Object> row = new HashMap<>();
                            row.put("userId", viewerId);
                            row.put("profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "");
                            row.put("displayName", (user.getNickname() != null && !user.getNickname().isBlank()) ? user.getNickname() : user.getUsername());
                            row.put("loginId", user.getLoginId() != null ? user.getLoginId() : "");
                            row.put("streamer", stream.getUserId() != null && stream.getUserId().equals(viewerId));
                            row.put("manager", managerSet.contains(viewerId));
                            row.put("fan", donorSet.contains(viewerId));
                            return row;
                        })
                        .orElse(null))
                .filter(row -> row != null)
                .toList();

        return ResponseEntity.ok(viewers);
    }

    private static Long parseLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * ???????????怨뚮뼺?됰뗀??????????? ??遺얘턁???????????耀붾굝??????? ?????????쇰뮡????⑤슢堉??怨몄젶????????????403. ????????耀붾굝????癲ル슢??㎖?밤뀋??????????????遺얘턁??????濡ろ뜐????
     */
    @PostMapping("/{streamId}/viewer/join")
    public ResponseEntity<?> viewerJoin(@PathVariable Long streamId, HttpSession session) {
        if (!liveStreamService.existsAndLive(streamId)) {
            return ResponseEntity.notFound().build();
        }
        Long userId = getCurrentUserId(session);
        if (userId != null) {
            if (streamChatSettingsService.isBlacklisted(streamId, userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "블랙리스트에 등록되어 이 방송에 참여할 수 없습니다."));
            }
            if (streamChatSettingsService.hasActiveTimedBan(streamId, userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "강제퇴장 당하셨습니다. 5분 동안 재입장할 수 없습니다."));
            }
            liveStreamService.viewerJoin(streamId, userId);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * ???????????怨뚮뼺?됰뗀??????????? ??????ш낄?????????耀붾굝???????
     */
    @PostMapping("/{streamId}/viewer/leave")
    public ResponseEntity<?> viewerLeave(@PathVariable Long streamId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (userId != null) liveStreamService.viewerLeave(streamId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * ????????濚밸Ŧ援?????遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉???
     */
    @GetMapping("/my")
    public ResponseEntity<?> listMy(HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        }
        return ResponseEntity.ok(liveStreamService.listByUser(uid));
    }

    /**
     * ????濾?癲ル슢?ｆ쾮???????????????????諛몃마??潁뺛깺苡????遺얘턁?????????遺얜??熬곣뫖釉멧벧猿뗪섭鴉??? type=live | recent
     */
    @GetMapping("/following")
    public ResponseEntity<?> listFollowing(@RequestParam(defaultValue = "all") String type, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
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
     * ?????諛몃마??潁뺛깺苡???? ???癲됱빖?????釉띿뇢??⒱닪??????????????諛몃마??潁뺛깺苡??????????깅뼂????遺얘턁????傭?끆???嶺뚮?猷볠꽴?? OBS ????????깅뼂????RTMP?????notify/end??????ル뭽?? ??耀붾굝????????? ?????嚥싲갭큔??????????
     */
    @PutMapping("/{streamId}/end")
    public ResponseEntity<?> endStream(@PathVariable Long streamId, HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("message", "????????耀붾굝????????源껊쵂????????꾩룆梨띰쭕????饔낅떽???????"));
        try {
            liveStreamService.endStreamByOwner(streamId, uid);
            return ResponseEntity.ok(Map.of("message", "?????諛몃마??潁뺛깺苡???????????깅뼂????遺얘턁????傭?끆???嶺뚮?猷볠꽴???????????"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ----- nginx-rtmp ?????獄쏅챶留덌┼????????(application/x-www-form-urlencoded, name=??????濚밸Ŧ援??????롮쾸?椰??????⑥쥓六? -----

    /**
     * nginx-rtmp on_publish ?????獄쏅챶留덌┼????????
     * 2xx ?????諛몃마嶺뚮???????????????????????좊틣?????怨뚰맜??????關?쒎첎?嫄??怨룸쵂?? ??????遺얘턁筌?（?????.
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
     * nginx-rtmp on_publish_done ?????獄쏅챶留덌┼????????
     */
    @PostMapping("/notify/end")
    public ResponseEntity<Void> notifyEnd(@RequestParam("name") String streamKey) {
        liveStreamService.notifyEnd(streamKey);
        return ResponseEntity.ok().build();
    }
}
