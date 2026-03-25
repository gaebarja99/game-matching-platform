package com.gamematcher.controller;

import com.gamematcher.entity.GroupChatMessage;
import com.gamematcher.entity.GroupChatRoom;
import com.gamematcher.service.GroupChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-chat")
@RequiredArgsConstructor
public class GroupChatController {

    private static final String SESSION_USER_ID = "userId";

    private final GroupChatService groupChatService;

    /** 방 생성 */
    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String name = body != null && body.get("name") != null ? body.get("name").toString().trim() : null;
        try {
            GroupChatRoom room = groupChatService.createRoom(userId, name);
            return ResponseEntity.ok(Map.of("id", room.getId(), "name", room.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 내가 참여 중인 방 목록 */
    @GetMapping("/rooms")
    public ResponseEntity<?> myRooms(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        List<Map<String, Object>> list = groupChatService.getMyRooms(userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 방 멤버 목록 */
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<?> roomMembers(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        List<Map<String, Object>> list = groupChatService.getRoomMembers(roomId, userId);
        if (list.isEmpty() && !groupChatService.isMember(roomId, userId))
            return ResponseEntity.status(403).body(Map.of("message", "방에 참여할 수 없습니다."));
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 초대 */
    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<?> invite(@PathVariable Long roomId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        Long toUserId = null;
        Object to = body != null ? body.get("userId") : null;
        if (to instanceof Number) toUserId = ((Number) to).longValue();
        else if (to != null) try { toUserId = Long.parseLong(to.toString()); } catch (NumberFormatException ignored) {}
        if (toUserId == null) return ResponseEntity.badRequest().body(Map.of("message", "초대할 사용자를 선택해 주세요."));
        String result = groupChatService.invite(roomId, userId, toUserId);
        switch (result) {
            case "sent": return ResponseEntity.ok(Map.of("ok", true));
            case "not_member": return ResponseEntity.status(403).body(Map.of("message", "방 멤버만 초대할 수 있습니다."));
            case "not_friend": return ResponseEntity.badRequest().body(Map.of("message", "친구에게만 초대할 수 있습니다."));
            case "already_member": return ResponseEntity.badRequest().body(Map.of("message", "이미 참여 중인 사용자입니다."));
            case "already_pending": return ResponseEntity.badRequest().body(Map.of("message", "이미 초대한 사용자입니다."));
            default: return ResponseEntity.badRequest().body(Map.of("message", "초대에 실패했습니다."));
        }
    }

    /** 받은 초대 목록 */
    @GetMapping("/invitations/received")
    public ResponseEntity<?> receivedInvitations(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        List<Map<String, Object>> list = groupChatService.getReceivedInvitations(userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 초대 수락 */
    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable Long invitationId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        boolean ok = groupChatService.acceptInvitation(invitationId, userId);
        if (!ok) return ResponseEntity.badRequest().body(Map.of("message", "수락할 수 없습니다."));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 초대 거절 */
    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<?> rejectInvitation(@PathVariable Long invitationId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        boolean ok = groupChatService.rejectInvitation(invitationId, userId);
        if (!ok) return ResponseEntity.badRequest().body(Map.of("message", "거절할 수 없습니다."));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 메시지 목록 */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> messages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (!groupChatService.isMember(roomId, userId))
            return ResponseEntity.status(403).body(Map.of("message", "방에 참여할 수 없습니다."));
        List<Map<String, Object>> list = groupChatService.getMessages(roomId, userId, limit);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 메시지 전송 */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String text = body != null && body.get("text") != null ? body.get("text").toString() : null;
        try {
            GroupChatMessage msg = groupChatService.sendMessage(roomId, userId, text);
            return ResponseEntity.ok(Map.of("id", msg.getId(), "text", msg.getText(), "createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : ""));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 방장 강퇴 */
    @PostMapping("/rooms/{roomId}/kick")
    public ResponseEntity<?> kickMember(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        Long targetUserId = null;
        Object raw = body != null ? body.get("userId") : null;
        if (raw instanceof Number) targetUserId = ((Number) raw).longValue();
        else if (raw != null) try { targetUserId = Long.parseLong(raw.toString()); } catch (NumberFormatException ignored) {}
        if (targetUserId == null) return ResponseEntity.badRequest().body(Map.of("message", "강퇴할 사용자를 선택해 주세요."));

        String result = groupChatService.kickMember(roomId, userId, targetUserId);
        return switch (result) {
            case "ok" -> ResponseEntity.ok(Map.of("ok", true));
            case "not_host" -> ResponseEntity.status(403).body(Map.of("message", "방장만 강퇴할 수 있습니다."));
            case "cannot_kick_self" -> ResponseEntity.badRequest().body(Map.of("message", "본인은 강퇴할 수 없습니다."));
            case "not_member" -> ResponseEntity.badRequest().body(Map.of("message", "이미 방에 없는 사용자입니다."));
            case "not_found" -> ResponseEntity.status(404).body(Map.of("message", "채팅방을 찾을 수 없습니다."));
            default -> ResponseEntity.badRequest().body(Map.of("message", "강퇴에 실패했습니다."));
        };
    }
    @PostMapping("/rooms/{roomId}/presence/{action}")
    public ResponseEntity<?> recordPresence(
            @PathVariable Long roomId,
            @PathVariable String action,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        boolean ok = groupChatService.recordPresence(roomId, userId, action);
        if (!ok) return ResponseEntity.badRequest().body(Map.of("message", "입장/이탈 기록 저장에 실패했습니다."));
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
