package com.gamematcher.controller;

import com.gamematcher.constant.MileagePurchaseType;
import com.gamematcher.constant.PaymentOrderKind;
import com.gamematcher.constant.Role;
import com.gamematcher.constant.StreamStatus;
import com.gamematcher.constant.StreamerTier;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.constant.community.BoardCategory;
import com.gamematcher.constant.community.PostStatus;
import com.gamematcher.entity.AdminAuditLog;
import com.gamematcher.entity.Donation;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.MileagePurchase;
import com.gamematcher.entity.PangWithdrawal;
import com.gamematcher.entity.PaymentOrder;
import com.gamematcher.entity.RecruitPost;
import com.gamematcher.entity.User;
import com.gamematcher.entity.community.Post;
import com.gamematcher.repository.AdminAuditLogRepository;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.MileagePurchaseRepository;
import com.gamematcher.repository.PangWithdrawalRepository;
import com.gamematcher.repository.PaymentOrderRepository;
import com.gamematcher.repository.RecruitPostRepository;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.repository.community.PostRepository;
import com.gamematcher.service.NotificationService;
import com.gamematcher.service.PangService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String SESSION_USER_ID = "userId";

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final MileagePurchaseRepository mileagePurchaseRepository;
    private final PangWithdrawalRepository pangWithdrawalRepository;
    private final PostRepository postRepository;
    private final RecruitPostRepository recruitPostRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PangService pangService;
    private final NotificationService notificationService;

    private boolean isAdmin(HttpSession session) {
        Long userId = currentAdminId(session);
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }

    private Long currentAdminId(HttpSession session) {
        return (Long) session.getAttribute(SESSION_USER_ID);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String displayName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return Objects.toString(user.getUsername(), "");
    }

    private List<Map<String, Object>> buildDailyAmountSeries(
            LocalDate startDate,
            LocalDate endDate,
            java.util.function.Function<LocalDate, Map<String, Object>> mapper
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            rows.add(mapper.apply(cursor));
            cursor = cursor.plusDays(1);
        }
        return rows;
    }

    private <T> Map<String, Object> paginate(List<T> source, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int totalElements = source.size();
        int from = Math.min(safePage * safeSize, totalElements);
        int to = Math.min(from + safeSize, totalElements);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        return Map.of(
                "content", source.subList(from, to),
                "page", safePage,
                "size", safeSize,
                "totalElements", totalElements,
                "totalPages", totalPages,
                "first", safePage == 0,
                "last", to >= totalElements
        );
    }

    private void logAction(Long adminUserId, String targetType, Long targetId, String actionType, String summary, String detail) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(adminUserId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActionType(actionType);
        log.setSummary(summary);
        log.setDetail(detail);
        adminAuditLogRepository.save(log);
    }

    @GetMapping("/streamers")
    public ResponseEntity<?> listStreamers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String tier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String normalizedQuery = normalize(query);
        String normalizedTier = normalize(tier);

        List<Map<String, Object>> rows = userRepository.findAll().stream()
                .map(user -> {
                    long totalReceived = donationRepository.sumAmountByToUserId(user.getId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", user.getId());
                    row.put("loginId", Objects.toString(user.getLoginId(), ""));
                    row.put("displayName", displayName(user));
                    row.put("streamerTier", user.getStreamerTier() != null ? user.getStreamerTier().name() : "GENERAL");
                    row.put("totalReceivedPang", totalReceived);
                    row.put("createdAt", user.getCreatedAt());
                    return row;
                })
                .filter(row -> normalizedQuery.isBlank()
                        || normalize((String) row.get("loginId")).contains(normalizedQuery)
                        || normalize((String) row.get("displayName")).contains(normalizedQuery))
                .filter(row -> normalizedTier.isBlank()
                        || normalize((String) row.get("streamerTier")).equals(normalizedTier))
                .sorted(Comparator.comparingLong(row -> -((Number) row.get("totalReceivedPang")).longValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(paginate(rows, page, size));
    }

    @GetMapping("/broadcasts")
    public ResponseEntity<?> listBroadcasts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "沅뚰븳???놁뒿?덈떎."));
        }

        String normalizedQuery = normalize(query);
        String normalizedStatus = normalize(status);
        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Map<String, Object>> rows = liveStreamRepository.findAll().stream()
                .sorted(Comparator.comparing(LiveStream::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(stream -> {
                    User streamer = userMap.get(stream.getUserId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", stream.getId());
                    row.put("title", Objects.toString(stream.getTitle(), ""));
                    row.put("game", stream.getGame() != null ? stream.getGame().name() : "");
                    row.put("status", stream.getStatus() != null ? stream.getStatus().name() : StreamStatus.CREATED.name());
                    row.put("statusLabel", switch (stream.getStatus() != null ? stream.getStatus() : StreamStatus.CREATED) {
                        case LIVE -> "방송 중";
                        case ENDED -> "종료됨";
                        case CREATED -> "준비 중";
                    });
                    row.put("userId", stream.getUserId());
                    row.put("loginId", streamer != null ? Objects.toString(streamer.getLoginId(), "") : "");
                    row.put("displayName", streamer != null ? displayName(streamer) : "");
                    row.put("createdAt", stream.getCreatedAt());
                    row.put("startedAt", stream.getStartedAt());
                    row.put("endedAt", stream.getEndedAt());
                    row.put("visibleInRecent", stream.getVisibleInRecent() == null || stream.getVisibleInRecent());
                    row.put("warningCount", stream.getAdminWarningCount() != null ? stream.getAdminWarningCount() : 0);
                    row.put("lastWarningAt", stream.getLastAdminWarningAt());
                    row.put("lastWarningMessage", Objects.toString(stream.getLastAdminWarningMessage(), ""));
                    return row;
                })
                .filter(row -> normalizedQuery.isBlank()
                        || normalize((String) row.get("title")).contains(normalizedQuery)
                        || normalize((String) row.get("loginId")).contains(normalizedQuery)
                        || normalize((String) row.get("displayName")).contains(normalizedQuery)
                        || normalize((String) row.get("game")).contains(normalizedQuery))
                .filter(row -> {
                    if (normalizedStatus.isBlank() || "all".equals(normalizedStatus)) {
                        return true;
                    }
                    if ("hidden_recent".equals(normalizedStatus)) {
                        return !Boolean.TRUE.equals(row.get("visibleInRecent"));
                    }
                    return normalize((String) row.get("status")).equals(normalizedStatus);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(paginate(rows, page, size));
    }

    @PatchMapping("/broadcasts/{streamId}")
    public ResponseEntity<?> updateBroadcast(
            @PathVariable Long streamId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        Long adminUserId = currentAdminId(session);
        if (adminUserId == null || !isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "沅뚰븳???놁뒿?덈떎."));
        }

        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        User streamer = userRepository.findById(stream.getUserId()).orElse(null);
        String action = normalize(body.get("action"));
        String message = Objects.toString(body.get("message"), "").trim();

        if ("warn".equals(action)) {
            int nextCount = (stream.getAdminWarningCount() != null ? stream.getAdminWarningCount() : 0) + 1;
            String warningMessage = message.isBlank()
                    ? "운영자 경고가 접수되었습니다. 방송 정책을 확인해 주세요."
                    : message;
            stream.setAdminWarningCount(nextCount);
            stream.setLastAdminWarningAt(LocalDateTime.now());
            stream.setLastAdminWarningMessage(warningMessage);
            stream.setLastAdminId(adminUserId);
            liveStreamRepository.save(stream);
            if (streamer != null) {
                notificationService.createAdminStreamNotice(streamer.getId(), adminUserId, warningMessage);
            }
            logAction(adminUserId, "LIVE_STREAM", streamId, "STREAM_WARNED", "방송 경고", warningMessage);
            return ResponseEntity.ok(Map.of(
                    "message", "방송 경고를 전달했습니다.",
                    "warningCount", nextCount,
                    "lastWarningMessage", warningMessage
            ));
        }

        if ("force_end".equals(action)) {
            stream.setStatus(StreamStatus.ENDED);
            stream.setEndedAt(LocalDateTime.now());
            stream.setLastAdminId(adminUserId);
            liveStreamRepository.save(stream);
            if (streamer != null) {
                notificationService.createAdminStreamNotice(
                        streamer.getId(),
                        adminUserId,
                        message.isBlank() ? "운영자에 의해 방송이 강제 종료되었습니다." : message
                );
            }
            logAction(adminUserId, "LIVE_STREAM", streamId, "STREAM_FORCE_ENDED", "방송 강제 종료", stream.getTitle());
            return ResponseEntity.ok(Map.of("message", "방송을 강제 종료했습니다.", "status", stream.getStatus().name()));
        }

        if ("hide_recent".equals(action)) {
            stream.setVisibleInRecent(false);
            stream.setLastAdminId(adminUserId);
            liveStreamRepository.save(stream);
            logAction(adminUserId, "LIVE_STREAM", streamId, "STREAM_HIDDEN_FROM_RECENT", "최근 방송 숨김", stream.getTitle());
            return ResponseEntity.ok(Map.of("message", "최근 방송 목록에서 숨겼습니다.", "visibleInRecent", false));
        }

        if ("restore_recent".equals(action)) {
            stream.setVisibleInRecent(true);
            stream.setLastAdminId(adminUserId);
            liveStreamRepository.save(stream);
            logAction(adminUserId, "LIVE_STREAM", streamId, "STREAM_RESTORED_TO_RECENT", "최근 방송 복구", stream.getTitle());
            return ResponseEntity.ok(Map.of("message", "최근 방송 목록에 다시 노출합니다.", "visibleInRecent", true));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 방송 관리 액션입니다."));
    }

    @GetMapping("/community/posts")
    public ResponseEntity<?> listCommunityPosts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String normalizedQuery = normalize(query);
        String normalizedCategory = normalize(category);
        String normalizedStatus = normalize(status);

        List<Map<String, Object>> rows = postRepository.findAll().stream()
                .sorted(Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(post -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", post.getId());
                    row.put("title", Objects.toString(post.getTitle(), ""));
                    row.put("category", post.getBoardCategory() != null ? post.getBoardCategory().name() : "FREE");
                    row.put("categoryLabel", post.getBoardCategory() != null ? post.getBoardCategory().getDisplayName() : "자유");
                    row.put("status", post.getStatus() != null ? post.getStatus().name() : PostStatus.ACTIVE.name());
                    row.put("statusLabel", post.getStatus() != null ? post.getStatus().getDisplayName() : "활성");
                    row.put("authorId", post.getAuthor() != null ? post.getAuthor().getId() : null);
                    row.put("authorLoginId", post.getAuthor() != null ? Objects.toString(post.getAuthor().getLoginId(), "") : "");
                    row.put("authorName", post.getAuthor() != null ? displayName(post.getAuthor()) : "");
                    row.put("viewCount", post.getViewCount());
                    row.put("likeCount", post.getLikeCount());
                    row.put("commentCount", post.getCommentCount());
                    row.put("reportCount", post.getReportCount());
                    row.put("isNotice", post.isNotice());
                    row.put("createdAt", post.getCreatedAt());
                    return row;
                })
                .filter(row -> normalizedQuery.isBlank()
                        || normalize((String) row.get("title")).contains(normalizedQuery)
                        || normalize((String) row.get("authorLoginId")).contains(normalizedQuery)
                        || normalize((String) row.get("authorName")).contains(normalizedQuery))
                .filter(row -> normalizedCategory.isBlank()
                        || "all".equals(normalizedCategory)
                        || normalize((String) row.get("category")).equals(normalizedCategory))
                .filter(row -> normalizedStatus.isBlank()
                        || "all".equals(normalizedStatus)
                        || normalize((String) row.get("status")).equals(normalizedStatus))
                .collect(Collectors.toList());

        return ResponseEntity.ok(paginate(rows, page, size));
    }

    @PatchMapping("/community/posts/{postId}")
    public ResponseEntity<?> updateCommunityPostStatus(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        Long adminUserId = currentAdminId(session);
        if (adminUserId == null || !isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        String action = normalize(body.get("action"));
        PostStatus nextStatus;
        String summary;
        if ("blind".equals(action)) {
            nextStatus = PostStatus.BLIND;
            summary = "게시글 블라인드";
        } else if ("restore".equals(action)) {
            nextStatus = PostStatus.ACTIVE;
            summary = "게시글 복구";
        } else if ("delete".equals(action)) {
            nextStatus = PostStatus.DELETED_BY_ADMIN;
            summary = "게시글 삭제";
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 작업입니다."));
        }

        post.setStatus(nextStatus);
        postRepository.save(post);
        logAction(adminUserId, "COMMUNITY_POST", post.getId(), nextStatus.name(), summary, post.getTitle());

        return ResponseEntity.ok(Map.of(
                "message", "게시글 상태가 변경되었습니다.",
                "status", post.getStatus().name(),
                "statusLabel", post.getStatus().getDisplayName()
        ));
    }

    @GetMapping("/match-rooms")
    public ResponseEntity<?> listMatchRooms(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String game,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String normalizedQuery = normalize(query);
        String normalizedGame = normalize(game);

        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Map<String, Object>> rows = recruitPostRepository.findAll().stream()
                .sorted(Comparator.comparing(RecruitPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(post -> {
                    User owner = userMap.get(post.getUserId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", post.getId());
                    row.put("game", Objects.toString(post.getGame(), ""));
                    row.put("summonerName", Objects.toString(post.getSummonerName(), ""));
                    row.put("mainPosition", Objects.toString(post.getMainPosition(), ""));
                    row.put("findPosition", Objects.toString(post.getFindPosition(), ""));
                    row.put("tier", Objects.toString(post.getTier(), ""));
                    row.put("region", Objects.toString(post.getRegion(), ""));
                    row.put("mode", Objects.toString(post.getMode(), ""));
                    row.put("memo", Objects.toString(post.getMemo(), ""));
                    row.put("createdAt", post.getCreatedAt());
                    row.put("ownerId", post.getUserId());
                    row.put("ownerLoginId", owner != null ? Objects.toString(owner.getLoginId(), "") : "");
                    row.put("ownerName", owner != null ? displayName(owner) : "");
                    return row;
                })
                .filter(row -> normalizedQuery.isBlank()
                        || normalize((String) row.get("summonerName")).contains(normalizedQuery)
                        || normalize((String) row.get("ownerLoginId")).contains(normalizedQuery)
                        || normalize((String) row.get("ownerName")).contains(normalizedQuery)
                        || normalize((String) row.get("memo")).contains(normalizedQuery))
                .filter(row -> normalizedGame.isBlank()
                        || "all".equals(normalizedGame)
                        || normalize((String) row.get("game")).equals(normalizedGame))
                .collect(Collectors.toList());

        return ResponseEntity.ok(paginate(rows, page, size));
    }

    @DeleteMapping("/match-rooms/{roomId}")
    public ResponseEntity<?> deleteMatchRoom(@PathVariable Long roomId, HttpSession session) {
        Long adminUserId = currentAdminId(session);
        if (adminUserId == null || !isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        RecruitPost post = recruitPostRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("매칭방을 찾을 수 없습니다."));

        recruitPostRepository.delete(post);
        logAction(adminUserId, "MATCH_ROOM", post.getId(), "DELETE", "매칭방 삭제", post.getSummonerName());

        return ResponseEntity.ok(Map.of("message", "매칭방이 삭제되었습니다."));
    }

    @PatchMapping("/streamers/{userId}/tier")
    public ResponseEntity<?> updateStreamerTier(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String tierValue = Objects.toString(body.get("tier"), "").trim();
        if (tierValue.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "등급을 입력해 주세요."));
        }

        StreamerTier tier;
        try {
            tier = StreamerTier.valueOf(tierValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 스트리머 등급입니다."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }

        user.setStreamerTier(tier);
        userRepository.save(user);
        logAction(currentAdminId(session), "USER", userId, "STREAMER_TIER_CHANGED", "스트리머 등급 변경", "등급을 " + tier.name() + "로 변경");

        return ResponseEntity.ok(Map.of("message", "스트리머 등급이 변경되었습니다.", "tier", tier.name()));
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String normalizedQuery = normalize(query);
        String normalizedRole = normalize(role);
        String normalizedStatus = normalize(status);

        List<Map<String, Object>> rows = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", user.getId());
                    row.put("loginId", Objects.toString(user.getLoginId(), ""));
                    row.put("username", Objects.toString(user.getUsername(), ""));
                    row.put("nickname", Objects.toString(user.getNickname(), ""));
                    row.put("email", Objects.toString(user.getEmail(), ""));
                    row.put("role", user.getRole().name());
                    row.put("status", user.getStatus().name());
                    row.put("provider", user.getProvider() != null ? user.getProvider().name() : "LOCAL");
                    row.put("pangBalance", user.getPangBalance() != null ? user.getPangBalance() : 0L);
                    row.put("streamerTier", user.getStreamerTier() != null ? user.getStreamerTier().name() : "GENERAL");
                    row.put("createdAt", user.getCreatedAt());
                    row.put("lastLoginAt", user.getLastLoginAt());
                    row.put("suspendedUntil", user.getSuspendedUntil());
                    row.put("suspensionReason", user.getSuspensionReason());
                    return row;
                })
                .filter(row -> normalizedQuery.isBlank()
                        || normalize((String) row.get("loginId")).contains(normalizedQuery)
                        || normalize((String) row.get("username")).contains(normalizedQuery)
                        || normalize((String) row.get("nickname")).contains(normalizedQuery)
                        || normalize((String) row.get("email")).contains(normalizedQuery))
                .filter(row -> normalizedRole.isBlank() || normalize((String) row.get("role")).equals(normalizedRole))
                .filter(row -> normalizedStatus.isBlank() || normalize((String) row.get("status")).equals(normalizedStatus))
                .sorted(Comparator.comparing(
                        row -> (LocalDateTime) row.get("createdAt"),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(paginate(rows, page, size));
    }

    @GetMapping("/users/{userId}/history")
    public ResponseEntity<?> getUserHistory(@PathVariable Long userId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        List<Map<String, Object>> rows = adminAuditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("USER", userId)
                .stream()
                .map(log -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", log.getId());
                    row.put("targetType", log.getTargetType());
                    row.put("actionType", log.getActionType());
                    row.put("summary", log.getSummary());
                    row.put("detail", log.getDetail());
                    row.put("adminUserId", log.getAdminUserId());
                    row.put("createdAt", log.getCreatedAt());
                    return row;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(rows);
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<?> updateUserState(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        Long sessionUserId = currentAdminId(session);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }

        Object statusValue = body.get("status");
        Object roleValue = body.get("role");
        String nicknameValue = Objects.toString(body.get("nickname"), "").trim();
        Object suspensionDaysValue = body.get("suspensionDays");
        boolean permanentSuspension = Boolean.parseBoolean(Objects.toString(body.get("permanentSuspension"), "false"));
        String suspensionReason = Objects.toString(body.get("suspensionReason"), "").trim();

        if (statusValue != null && !Objects.toString(statusValue, "").isBlank()) {
            UserStatus nextStatus;
            try {
                nextStatus = UserStatus.valueOf(Objects.toString(statusValue).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 회원 상태입니다."));
            }

            if (nextStatus == UserStatus.SUSPENDED) {
                Integer suspensionDays = null;
                if (suspensionDaysValue != null && !Objects.toString(suspensionDaysValue, "").isBlank()) {
                    try {
                        suspensionDays = Integer.parseInt(Objects.toString(suspensionDaysValue));
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body(Map.of("message", "정지 기간 값이 올바르지 않습니다."));
                    }
                }
                if (!permanentSuspension && (suspensionDays == null || suspensionDays <= 0)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "정지 기간을 선택해 주세요."));
                }

                LocalDateTime suspendedUntil = permanentSuspension ? null : LocalDateTime.now().plusDays(suspensionDays);
                user.setSuspendedUntil(suspendedUntil);
                user.setSuspensionReason(suspensionReason.isBlank() ? null : suspensionReason);
                logAction(
                        sessionUserId,
                        "USER",
                        userId,
                        "USER_SUSPENDED",
                        "회원 정지",
                        permanentSuspension
                                ? "영구 정지 처리"
                                : suspensionDays + "일 정지 처리, 해제 예정 " + suspendedUntil
                );
            } else {
                user.setSuspendedUntil(null);
                user.setSuspensionReason(null);
                logAction(sessionUserId, "USER", userId, "USER_STATUS_CHANGED", "회원 상태 변경", "상태를 " + nextStatus.name() + "로 변경");
            }
            user.setStatus(nextStatus);
        }

        if (roleValue != null && !Objects.toString(roleValue, "").isBlank()) {
            try {
                Role nextRole = Role.valueOf(Objects.toString(roleValue).toUpperCase(Locale.ROOT));
                if (Objects.equals(sessionUserId, userId) && nextRole != Role.ADMIN) {
                    return ResponseEntity.badRequest().body(Map.of("message", "본인 관리자 권한은 해제할 수 없습니다."));
                }
                user.setRole(nextRole);
                logAction(sessionUserId, "USER", userId, "USER_ROLE_CHANGED", "회원 권한 변경", "권한을 " + nextRole.name() + "로 변경");
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 회원 권한입니다."));
            }
        }

        if (body.containsKey("nickname")) {
            if (nicknameValue.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "닉네임을 입력해 주세요."));
            }
            if (userRepository.existsByNicknameAndIdNot(nicknameValue, userId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 닉네임입니다."));
            }
            user.setNickname(nicknameValue);
            logAction(sessionUserId, "USER", userId, "USER_NICKNAME_CHANGED", "회원 닉네임 변경", "닉네임을 '" + nicknameValue + "'로 변경");
        }

        userRepository.save(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "회원 정보가 변경되었습니다.");
        response.put("status", user.getStatus().name());
        response.put("role", user.getRole().name());
        response.put("nickname", user.getNickname());
        response.put("suspendedUntil", user.getSuspendedUntil());
        response.put("suspensionReason", user.getSuspensionReason());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pang/gift")
    public ResponseEntity<?> giftPang(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String loginId = Objects.toString(body.get("loginId"), "").trim();
        String customMessage = Objects.toString(body.get("message"), "").trim();
        if (loginId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "아이디를 입력해 주세요."));
        }

        int pangAmount;
        try {
            pangAmount = Integer.parseInt(Objects.toString(body.get("pangAmount"), "0"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "지급할 팡 수량이 올바르지 않습니다."));
        }
        if (pangAmount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "지급할 팡 수량은 1 이상이어야 합니다."));
        }

        Long adminUserId = currentAdminId(session);

        if ("/all".equalsIgnoreCase(loginId)) {
            List<User> targetUsers = userRepository.findAll().stream()
                    .filter(user -> user.getStatus() != UserStatus.DELETED)
                    .collect(Collectors.toList());

            if (targetUsers.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "吏湲됲븷 ?뚯썝???놁뒿?덈떎."));
            }

            for (User targetUser : targetUsers) {
                pangService.grantFromMileage(targetUser.getId(), pangAmount);
                notificationService.createForAdminPangGift(targetUser.getId(), adminUserId, pangAmount, customMessage);
            }

            logAction(
                    adminUserId,
                    "SYSTEM",
                    0L,
                    "ADMIN_PANG_GIFTED_ALL",
                    "?댁쁺?????껑泥?吏湲?",
                    "紐⑤뱺 ?뚯썝??" + pangAmount + "??吏湲? (" + targetUsers.size() + "紐?)"
            );

            return ResponseEntity.ok(Map.of(
                    "message", "?몃? ?뚯썝?먭쾶 ?≪쓣 吏湲됲뻽?듬땲??",
                    "recipientCount", targetUsers.size()
            ));
        }

        User targetUser = userRepository.findByLoginId(loginId).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.status(404).body(Map.of("message", "대상 회원을 찾을 수 없습니다."));
        }

        long currentBalance = pangService.grantFromMileage(targetUser.getId(), pangAmount);
        notificationService.createForAdminPangGift(targetUser.getId(), adminUserId, pangAmount, customMessage);
        logAction(adminUserId, "USER", targetUser.getId(), "ADMIN_PANG_GIFTED", "운영자 팡 지급", loginId + " 계정에 " + pangAmount + "팡 지급");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "운영자 팡 지급이 완료되었습니다.");
        response.put("loginId", targetUser.getLoginId());
        response.put("pangBalance", currentBalance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenueSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        List<PaymentOrder> orders = paymentOrderRepository.findAll();
        List<MileagePurchase> mileagePurchases = mileagePurchaseRepository.findAll();
        List<Donation> donations = donationRepository.findAll();
        List<PangWithdrawal> withdrawals = pangWithdrawalRepository.findAll();
        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        LocalDate today = LocalDate.now();
        int targetYear = year != null ? year : today.getYear();
        int targetMonth = month != null ? month : today.getMonthValue();
        LocalDate targetMonthStart = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate targetMonthEndExclusive = targetMonthStart.plusMonths(1);
        LocalDate firstDayOfCurrentMonth = today.withDayOfMonth(1);

        long totalRequestedWon = orders.stream().mapToLong(PaymentOrder::getAmountWon).sum();
        long completedPang = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .mapToLong(order -> order.getPangAmount() == null ? 0 : order.getPangAmount())
                .sum();
        long totalDonationPang = donations.stream()
                .mapToLong(donation -> donation.getAmount() == null ? 0 : donation.getAmount())
                .sum();
        long completedSettlementCommissionPang = withdrawals.stream()
                .mapToLong(withdrawal -> withdrawal.getCommissionPang() == null ? 0 : withdrawal.getCommissionPang())
                .sum();
        long completedSettlementPang = withdrawals.stream()
                .mapToLong(withdrawal -> withdrawal.getNetPang() == null ? 0 : withdrawal.getNetPang())
                .sum();
        long monthUsedPang = donations.stream()
                .filter(donation -> donation.getCreatedAt() != null
                        && !donation.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && donation.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(donation -> donation.getAmount() == null ? 0 : donation.getAmount())
                .sum();
        long monthCompletedSettlementCommissionPang = withdrawals.stream()
                .filter(withdrawal -> withdrawal.getCreatedAt() != null
                        && !withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(withdrawal -> withdrawal.getCommissionPang() == null ? 0 : withdrawal.getCommissionPang())
                .sum();
        long monthCompletedSettlementPang = withdrawals.stream()
                .filter(withdrawal -> withdrawal.getCreatedAt() != null
                        && !withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(withdrawal -> withdrawal.getNetPang() == null ? 0 : withdrawal.getNetPang())
                .sum();
        long remainingPang = userMap.values().stream()
                .mapToLong(user -> user.getPangBalance() == null ? 0 : user.getPangBalance())
                .sum();
        long totalIssuedPang = totalDonationPang + remainingPang;

        long completedSalesWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getCreatedAt() != null
                        && !order.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && order.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long todayCompletedSalesWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().isEqual(today))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long pendingSalesWon = 0L;
        long failedSalesWon = orders.stream()
                .filter(order -> "FAILED".equals(order.getStatus()))
                .filter(order -> order.getCreatedAt() != null
                        && !order.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && order.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long cancelledSalesWon = orders.stream()
                .filter(order -> "CANCELLED".equals(order.getStatus()))
                .filter(order -> order.getCreatedAt() != null
                        && !order.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && order.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();

        long mileageSalesWon = mileagePurchases.stream()
                .filter(purchase -> purchase.getType() == MileagePurchaseType.PANG
                        || purchase.getType() == MileagePurchaseType.SUBSCRIPTION_TICKET
                        || purchase.getType() == MileagePurchaseType.AD_FREE_30_DAYS)
                .filter(purchase -> purchase.getCreatedAt() != null
                        && !purchase.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && purchase.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                .sum();
        long subscriptionSalesWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.SUBSCRIPTION)
                .filter(order -> order.getCreatedAt() != null
                        && !order.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && order.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long usedMileage = mileagePurchases.stream()
                .filter(purchase -> purchase.getType() == MileagePurchaseType.PANG
                        || purchase.getType() == MileagePurchaseType.SUBSCRIPTION_TICKET
                        || purchase.getType() == MileagePurchaseType.AD_FREE_30_DAYS)
                .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                .sum();
        long remainingMileage = userMap.values().stream()
                .mapToLong(user -> user.getMileage() == null ? 0 : user.getMileage())
                .sum();
        long grantedMileage = usedMileage + remainingMileage;
        long adminGrantedMileage = mileagePurchases.stream()
                .filter(purchase -> purchase.getType() == MileagePurchaseType.ADMIN_GIFT)
                .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                .sum();
        long monthAdminGrantedMileage = mileagePurchases.stream()
                .filter(purchase -> purchase.getType() == MileagePurchaseType.ADMIN_GIFT)
                .filter(purchase -> purchase.getCreatedAt() != null
                        && !purchase.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && purchase.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                .sum();
        long adFreeSalesWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.AD_FREE)
                .filter(order -> order.getCreatedAt() != null
                        && !order.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && order.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();

        long todaySettlementCommissionWon = Math.round(withdrawals.stream()
                .filter(withdrawal -> withdrawal.getCreatedAt() != null && withdrawal.getCreatedAt().toLocalDate().isEqual(today))
                .mapToLong(withdrawal -> withdrawal.getCommissionPang() == null ? 0 : withdrawal.getCommissionPang())
                .sum() * 1.2d);
        long todayAdFreeSalesWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.AD_FREE)
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().isEqual(today))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long todayMileageSalesWon = mileagePurchases.stream()
                .filter(purchase -> purchase.getType() == MileagePurchaseType.PANG
                        || purchase.getType() == MileagePurchaseType.SUBSCRIPTION_TICKET
                        || purchase.getType() == MileagePurchaseType.AD_FREE_30_DAYS)
                .filter(purchase -> purchase.getCreatedAt() != null && purchase.getCreatedAt().toLocalDate().isEqual(today))
                .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                .sum();
        long todayPlatformRevenueWon = Math.max(0L, todaySettlementCommissionWon + todayAdFreeSalesWon - todayMileageSalesWon);

        long monthSettlementCommissionWon = Math.round(withdrawals.stream()
                .filter(withdrawal -> withdrawal.getCreatedAt() != null
                        && !withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                        && withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                .mapToLong(withdrawal -> withdrawal.getCommissionPang() == null ? 0 : withdrawal.getCommissionPang())
                .sum() * 1.2d);
        long monthPlatformRevenueWon = Math.max(0L, monthSettlementCommissionWon + adFreeSalesWon - mileageSalesWon);

        long platformRevenueWon = Math.max(
                0L,
                Math.round(withdrawals.stream()
                        .filter(withdrawal -> withdrawal.getCreatedAt() != null
                                && !withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                                && withdrawal.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                        .mapToLong(withdrawal -> withdrawal.getCommissionPang() == null ? 0 : withdrawal.getCommissionPang())
                        .sum() * 1.2d)
                        + adFreeSalesWon
                        - mileageSalesWon
        );

        List<Map<String, Object>> recentOrders = java.util.stream.Stream.concat(
                        orders.stream()
                                .filter(order -> "COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus()))
                                .filter(order -> order.getCreatedAt() != null
                                        && !order.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                                        && order.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                                .map(order -> {
                                    User orderUser = userMap.get(order.getUserId());
                                    Map<String, Object> row = new LinkedHashMap<>();
                                    row.put("id", order.getId());
                                    row.put("orderId", order.getOrderId());
                                    row.put("loginId", orderUser != null ? Objects.toString(orderUser.getLoginId(), "") : "");
                                    row.put("displayName", displayName(orderUser));
                                    row.put("kind", order.getKind().name());
                                    row.put("amountWon", order.getAmountWon());
                                    row.put("pangAmount", order.getPangAmount());
                                    row.put("status", order.getStatus());
                                    row.put("createdAt", order.getCreatedAt());
                                    return row;
                                }),
                        mileagePurchases.stream()
                                .filter(purchase -> purchase.getType() == MileagePurchaseType.PANG
                                        || purchase.getType() == MileagePurchaseType.SUBSCRIPTION_TICKET
                                        || purchase.getType() == MileagePurchaseType.AD_FREE_30_DAYS)
                                .filter(purchase -> purchase.getCreatedAt() != null
                                        && !purchase.getCreatedAt().toLocalDate().isBefore(targetMonthStart)
                                        && purchase.getCreatedAt().toLocalDate().isBefore(targetMonthEndExclusive))
                                .map(purchase -> {
                                    User orderUser = userMap.get(purchase.getUserId());
                                    Map<String, Object> row = new LinkedHashMap<>();
                                    row.put("id", purchase.getId());
                                    row.put("orderId", "MILEAGE-" + purchase.getId());
                                    row.put("loginId", orderUser != null ? Objects.toString(orderUser.getLoginId(), "") : "");
                                    row.put("displayName", displayName(orderUser));
                                    row.put("kind", purchase.getType().name());
                                    row.put("amountWon", purchase.getMileageCost() == null ? 0L : purchase.getMileageCost());
                                    row.put("pangAmount", purchase.getPangAmount());
                                    row.put("status", "COMPLETED");
                                    row.put("createdAt", purchase.getCreatedAt());
                                    return row;
                                }))
                .sorted(Comparator.comparing(
                        row -> (LocalDateTime) row.get("createdAt"),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(8)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRequestedWon", totalRequestedWon);
        response.put("completedSalesWon", completedSalesWon);
        response.put("pendingSalesWon", pendingSalesWon);
        response.put("failedSalesWon", failedSalesWon);
        response.put("cancelledSalesWon", cancelledSalesWon);
        response.put("completedPang", completedPang);
        response.put("todayCompletedWon", todayCompletedSalesWon);
        response.put("monthCompletedWon", completedSalesWon);
        response.put("mileageSalesWon", mileageSalesWon);
        response.put("subscriptionSalesWon", subscriptionSalesWon);
        response.put("adFreeSalesWon", adFreeSalesWon);
        response.put("platformRevenueWon", platformRevenueWon);
        response.put("todayPlatformRevenueWon", todayPlatformRevenueWon);
        response.put("monthPlatformRevenueWon", monthPlatformRevenueWon);
        response.put("totalDonationPang", totalDonationPang);
        response.put("completedSettlementCommissionPang", completedSettlementCommissionPang);
        response.put("completedSettlementPang", completedSettlementPang);
        response.put("monthUsedPang", monthUsedPang);
        response.put("monthCompletedSettlementCommissionPang", monthCompletedSettlementCommissionPang);
        response.put("monthCompletedSettlementPang", monthCompletedSettlementPang);
        response.put("totalIssuedPang", totalIssuedPang);
        response.put("usedPang", totalDonationPang);
        response.put("remainingPang", remainingPang);
        response.put("grantedMileage", grantedMileage);
        response.put("usedMileage", usedMileage);
        response.put("remainingMileage", remainingMileage);
        response.put("adminGrantedMileage", adminGrantedMileage);
        response.put("monthAdminGrantedMileage", monthAdminGrantedMileage);
        response.put("selectedYear", targetYear);
        response.put("selectedMonth", targetMonth);
        response.put("currentYear", today.getYear());
        response.put("currentMonth", today.getMonthValue());
        response.put("recentOrders", recentOrders);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue-v2")
    public ResponseEntity<?> getRevenueSummaryV2(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        List<PaymentOrder> orders = paymentOrderRepository.findAll();
        List<MileagePurchase> mileagePurchases = mileagePurchaseRepository.findAll();
        List<Donation> donations = donationRepository.findAll();
        List<PangWithdrawal> withdrawals = pangWithdrawalRepository.findAll();
        List<User> users = userRepository.findAll();
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate trendStart = today.minusDays(6);

        long totalChargeWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.PANG_CHARGE)
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long todayChargeWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.PANG_CHARGE)
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().isEqual(today))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long monthChargeWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.PANG_CHARGE)
                .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().toLocalDate().isBefore(firstDayOfMonth))
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long successChargeWon = totalChargeWon;
        long cancelledChargeWon = orders.stream()
                .filter(order -> "CANCELLED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.PANG_CHARGE)
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long failedChargeWon = orders.stream()
                .filter(order -> "FAILED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.PANG_CHARGE)
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();

        long usedPang = donations.stream()
                .mapToLong(donation -> donation.getAmount() == null ? 0 : donation.getAmount())
                .sum();
        long remainingPang = users.stream()
                .mapToLong(user -> user.getPangBalance() == null ? 0 : user.getPangBalance())
                .sum();
        long totalIssuedPang = usedPang + remainingPang;

        long usedMileage = mileagePurchases.stream()
                .filter(purchase -> purchase.getType() == MileagePurchaseType.PANG
                        || purchase.getType() == MileagePurchaseType.SUBSCRIPTION_TICKET
                        || purchase.getType() == MileagePurchaseType.AD_FREE_30_DAYS)
                .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                .sum();
        long remainingMileage = users.stream()
                .mapToLong(user -> user.getMileage() == null ? 0 : user.getMileage())
                .sum();
        long grantedMileage = usedMileage + remainingMileage;
        long adminGrantedMileage = mileagePurchases.stream()
                .filter(purchase -> purchase.getType() == MileagePurchaseType.ADMIN_GIFT)
                .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                .sum();

        long commissionWon = Math.round(withdrawals.stream()
                .mapToLong(withdrawal -> withdrawal.getCommissionPang() == null ? 0 : withdrawal.getCommissionPang())
                .sum() * 1.2d);
        long streamerSettlementWon = Math.round(withdrawals.stream()
                .mapToLong(withdrawal -> withdrawal.getNetPang() == null ? 0 : withdrawal.getNetPang())
                .sum() * 1.2d);
        long adFreeCashWon = orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .filter(order -> order.getKind() == PaymentOrderKind.AD_FREE)
                .mapToLong(PaymentOrder::getAmountWon)
                .sum();
        long platformRevenueWon = Math.max(0L, commissionWon + adFreeCashWon - usedMileage);

        List<Map<String, Object>> cashTrend = buildDailyAmountSeries(trendStart, today, date -> {
            long amountWon = orders.stream()
                    .filter(order -> "COMPLETED".equals(order.getStatus()))
                    .filter(order -> order.getKind() == PaymentOrderKind.PANG_CHARGE)
                    .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().isEqual(date))
                    .mapToLong(PaymentOrder::getAmountWon)
                    .sum();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", date.toString());
            row.put("amountWon", amountWon);
            return row;
        });

        List<Map<String, Object>> pangTrend = buildDailyAmountSeries(trendStart, today, date -> {
            long dayUsedPang = donations.stream()
                    .filter(donation -> donation.getCreatedAt() != null && donation.getCreatedAt().toLocalDate().isEqual(date))
                    .mapToLong(donation -> donation.getAmount() == null ? 0 : donation.getAmount())
                    .sum();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", date.toString());
            row.put("usedPang", dayUsedPang);
            return row;
        });

        List<Map<String, Object>> mileageTrend = buildDailyAmountSeries(trendStart, today, date -> {
            long dayGrantedMileage = mileagePurchases.stream()
                    .filter(purchase -> purchase.getCreatedAt() != null && purchase.getCreatedAt().toLocalDate().isEqual(date))
                    .filter(purchase -> purchase.getType() == MileagePurchaseType.ADMIN_GIFT)
                    .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                    .sum();
            long dayUsedMileage = mileagePurchases.stream()
                    .filter(purchase -> purchase.getCreatedAt() != null && purchase.getCreatedAt().toLocalDate().isEqual(date))
                    .filter(purchase -> purchase.getType() == MileagePurchaseType.PANG
                            || purchase.getType() == MileagePurchaseType.SUBSCRIPTION_TICKET
                            || purchase.getType() == MileagePurchaseType.AD_FREE_30_DAYS)
                    .mapToLong(purchase -> purchase.getMileageCost() == null ? 0 : purchase.getMileageCost())
                    .sum();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", date.toString());
            row.put("grantedMileage", dayGrantedMileage);
            row.put("usedMileage", dayUsedMileage);
            return row;
        });

        List<Map<String, Object>> topDonationStreamers = donationRepository
                .findTopUserIdsByTotalDonation(org.springframework.data.domain.PageRequest.of(0, 5))
                .stream()
                .map(row -> {
                    Long userId = row[0] instanceof Number ? ((Number) row[0]).longValue() : null;
                    long donationPang = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                    User targetUser = userId != null ? userMap.get(userId) : null;
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("userId", userId);
                    item.put("displayName", displayName(targetUser));
                    item.put("loginId", targetUser != null ? Objects.toString(targetUser.getLoginId(), "") : "");
                    item.put("donationPang", donationPang);
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("topKpis", Map.of(
                "totalChargeWon", totalChargeWon,
                "platformRevenueWon", platformRevenueWon,
                "remainingPang", remainingPang,
                "remainingMileage", remainingMileage
        ));
        response.put("cashFlow", Map.of(
                "totalChargeWon", totalChargeWon,
                "todayChargeWon", todayChargeWon,
                "monthChargeWon", monthChargeWon,
                "successChargeWon", successChargeWon,
                "cancelledChargeWon", cancelledChargeWon,
                "refundedChargeWon", cancelledChargeWon,
                "failedChargeWon", failedChargeWon,
                "trend", cashTrend
        ));
        response.put("pangFlow", Map.of(
                "totalIssuedPang", totalIssuedPang,
                "usedPang", usedPang,
                "remainingPang", remainingPang,
                "trend", pangTrend,
                "topDonationStreamers", topDonationStreamers
        ));
        response.put("mileageFlow", Map.of(
                "grantedMileage", grantedMileage,
                "usedMileage", usedMileage,
                "remainingMileage", remainingMileage,
                "adminGrantedMileage", adminGrantedMileage,
                "trend", mileageTrend
        ));
        response.put("profitFlow", Map.of(
                "platformRevenueWon", platformRevenueWon,
                "streamerSettlementWon", streamerSettlementWon,
                "commissionWon", commissionWon,
                "adFreeCashWon", adFreeCashWon
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settlements")
    public ResponseEntity<?> listSettlements(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String normalizedQuery = normalize(query);

        List<Map<String, Object>> rows = pangWithdrawalRepository.findAll().stream()
                .map(withdrawal -> {
                    User orderUser = userRepository.findById(withdrawal.getUserId()).orElse(null);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", withdrawal.getId());
                    row.put("orderId", "WD-" + withdrawal.getId());
                    row.put("loginId", orderUser != null ? Objects.toString(orderUser.getLoginId(), "") : "");
                    row.put("displayName", displayName(orderUser));
                    row.put("kind", "WITHDRAWAL");
                    row.put("pangAmount", withdrawal.getAmountPang());
                    row.put("amountWon", withdrawal.getNetPang());
                    row.put("status", "REQUESTED");
                    row.put("createdAt", withdrawal.getCreatedAt());
                    row.put("completedAt", null);
                    row.put("commissionPercent", withdrawal.getCommissionPercent());
                    row.put("commissionPang", withdrawal.getCommissionPang());
                    row.put("netPang", withdrawal.getNetPang());
                    return row;
                })
                .filter(row -> normalizedQuery.isBlank()
                        || normalize((String) row.get("orderId")).contains(normalizedQuery)
                        || normalize((String) row.get("loginId")).contains(normalizedQuery)
                        || normalize((String) row.get("displayName")).contains(normalizedQuery))
                .sorted(Comparator.comparing(
                        row -> (LocalDateTime) row.get("createdAt"),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(paginate(rows, page, size));
    }

    @PatchMapping("/settlements/{settlementId}")
    public ResponseEntity<?> updateSettlementStatus(
            @PathVariable Long settlementId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "권한이 없습니다."));
        }

        String status = Objects.toString(body.get("status"), "").trim();
        if (status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "정산 상태를 입력해 주세요."));
        }

        String nextStatus = status.toUpperCase(Locale.ROOT);
        List<String> allowed = List.of("PENDING", "COMPLETED", "FAILED", "CANCELLED");
        if (!allowed.contains(nextStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 정산 상태입니다."));
        }

        PaymentOrder order = paymentOrderRepository.findById(settlementId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of("message", "정산 대상을 찾을 수 없습니다."));
        }

        order.setStatus(nextStatus);
        order.setCompletedAt("COMPLETED".equals(nextStatus) ? LocalDateTime.now() : null);
        paymentOrderRepository.save(order);
        logAction(currentAdminId(session), "SETTLEMENT", settlementId, "SETTLEMENT_STATUS_CHANGED", "정산 상태 변경", "상태를 " + nextStatus + "로 변경");

        return ResponseEntity.ok(Map.of(
                "message", "정산 상태가 변경되었습니다.",
                "status", order.getStatus()
        ));
    }
}
