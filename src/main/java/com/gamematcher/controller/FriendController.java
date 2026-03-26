package com.gamematcher.controller;

import com.gamematcher.dto.friend.FriendSearchResult;
import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.service.FriendRequestService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gamematcher.config.OnlineUserStore;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private static final String SESSION_USER_ID = "userId";

    private final UserRepository userRepository;
    private final FriendRequestService friendRequestService;

    /** 접속 중인 사용자 ID 목록 (친구 온라인/오프라인 표시용) */
    @GetMapping("/online-ids")
    public ResponseEntity<List<Long>> onlineIds(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(List.copyOf(OnlineUserStore.getOnlineUserIds()));
    }

    /** 친구 목록 (수락된 요청 기준) */
    @GetMapping("/list")
    public ResponseEntity<List<FriendSearchResult>> friendList(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        List<Long> friendIds = friendRequestService.getFriendUserIds(userId);
        if (friendIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        Map<Long, User> userMap = userRepository.findAllById(friendIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        List<FriendSearchResult> list = friendIds.stream()
                .filter(id -> !id.equals(userId))
                .map(userMap::get)
                .filter(u -> u != null)
                .map(FriendSearchResult::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * 친구 관계인 경우에만 상대의 공개 프로필(자기소개 포함) 조회.
     */
    @GetMapping("/{userId}/public-profile")
    public ResponseEntity<?> friendPublicProfile(@PathVariable Long userId, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (userId == null || userId.equals(myId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "잘못된 요청입니다."));
        }
        if (!friendRequestService.getFriendUserIds(myId).contains(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "친구만 프로필을 볼 수 있습니다."));
        }
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) {
            return ResponseEntity.status(404).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }
        String nick = u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername();
        String bio = u.getBio() != null ? u.getBio().trim() : "";
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "loginId", u.getLoginId() != null ? u.getLoginId() : "",
                "nickname", nick != null ? nick : "",
                "profileImageUrl", u.getProfileImageUrl() != null ? u.getProfileImageUrl() : "",
                "bio", bio));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FriendSearchResult>> search(
            @RequestParam("q") String q,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        String trimmed = q != null ? q.trim() : "";
        // 한 글자 검색도 허용 (닉네임/아이디 한 글자에도 대응)
        if (trimmed.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<User> users = userRepository.searchByLoginIdOrNickname(trimmed, userId);
        List<FriendSearchResult> list = users.stream()
                .map(FriendSearchResult::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** 친구 요청 보내기 */
    @PostMapping("/requests")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Object> body, HttpSession session) {
        Long fromUserId = (Long) session.getAttribute(SESSION_USER_ID);
        if (fromUserId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long toUserId = null;
        Object uid = body != null ? body.get("userId") : null;
        if (uid instanceof Number) toUserId = ((Number) uid).longValue();
        else if (uid != null) try { toUserId = Long.parseLong(uid.toString()); } catch (NumberFormatException ignored) {}
        if (toUserId == null || toUserId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "대상 사용자가 필요합니다."));
        }
        if (fromUserId.equals(toUserId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "본인에게 요청할 수 없습니다."));
        }
        String result = friendRequestService.sendRequest(fromUserId, toUserId);
        if ("already_friends".equals(result)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "이미 친구입니다."));
        }
        if ("already_pending".equals(result)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "이미 요청을 보냈습니다."));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 받은 친구 요청 목록 */
    @GetMapping("/requests/received")
    public ResponseEntity<?> receivedRequests(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<Map<String, Object>> list = friendRequestService.getReceivedPending(userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 친구 요청 수락 */
    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        friendRequestService.accept(id, userId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 친구 요청 거절 */
    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        friendRequestService.reject(id, userId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 친구 삭제(언프렌드) */
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<?> removeFriend(@PathVariable Long friendUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean removed = friendRequestService.removeFriend(userId, friendUserId);
        if (!removed) {
            return ResponseEntity.badRequest().body(Map.of("message", "친구 관계가 없거나 이미 삭제되었습니다."));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 차단 목록 */
    @GetMapping("/blocked")
    public ResponseEntity<List<FriendSearchResult>> blockedList(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(friendRequestService.getBlockedUsers(userId));
    }

    /** 차단 */
    @PostMapping("/block")
    public ResponseEntity<?> blockUser(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long targetUserId = null;
        Object uid = body != null ? body.get("userId") : null;
        if (uid instanceof Number) targetUserId = ((Number) uid).longValue();
        else if (uid != null) try { targetUserId = Long.parseLong(uid.toString()); } catch (NumberFormatException ignored) {}
        if (targetUserId == null || targetUserId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "대상 사용자를 선택해 주세요."));
        }
        friendRequestService.blockUser(userId, targetUserId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 차단 해제 */
    @DeleteMapping("/block/{blockedUserId}")
    public ResponseEntity<?> unblockUser(@PathVariable Long blockedUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean removed = friendRequestService.unblockUser(userId, blockedUserId);
        if (!removed) {
            return ResponseEntity.badRequest().body(Map.of("message", "차단 목록에 없습니다."));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
