package com.gamematcher.controller;

import com.gamematcher.service.GameRoomService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game-rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private static final String SESSION_USER_ID = "userId";

    private final GameRoomService gameRoomService;

    /** 방 만들기 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String title = body != null && body.get("title") != null ? body.get("title").toString().trim() : null;
        String memo = body != null && body.get("memo") != null ? body.get("memo").toString().trim() : null;
        String deletePassword = body != null && body.get("deletePassword") != null ? body.get("deletePassword").toString() : null;
        String game = body != null && body.get("game") != null ? body.get("game").toString() : "LEAGUE_OF_LEGENDS";
        String gameOptions = body != null && body.get("gameOptions") != null ? body.get("gameOptions").toString() : null;
        try {
            var room = gameRoomService.create(userId, title, memo, deletePassword, game, gameOptions);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", room.getId());
            out.put("title", room.getTitle());
            out.put("game", room.getGame());
            out.put("groupChatRoomId", room.getGroupChatRoomId());
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 방 목록 (게임·마감 필터. closed=true 시 마감된 방만) */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) Boolean closed,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        List<Map<String, Object>> list = gameRoomService.listRooms(game, closed, userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    /** 참가 */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> join(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String result = gameRoomService.join(roomId, userId);
        switch (result) {
            case "ok": return ResponseEntity.ok(Map.of("ok", true));
            case "not_found": return ResponseEntity.status(404).body(Map.of("message", "방을 찾을 수 없습니다."));
            case "closed": return ResponseEntity.badRequest().body(Map.of("message", "마감된 방입니다."));
            case "full": return ResponseEntity.badRequest().body(Map.of("message", "해당 방의 인원이 모두 찼습니다. 다른 방을 이용해 주세요."));
            case "already_member": return ResponseEntity.badRequest().body(Map.of("message", "이미 참가 중입니다."));
            default: return ResponseEntity.badRequest().body(Map.of("message", "참가에 실패했습니다."));
        }
    }

    /** 나가기 */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leave(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String result = gameRoomService.leave(roomId, userId);
        switch (result) {
            case "ok": return ResponseEntity.ok(Map.of("ok", true));
            case "not_found": return ResponseEntity.status(404).body(Map.of("message", "방을 찾을 수 없습니다."));
            case "host_cannot_leave": return ResponseEntity.badRequest().body(Map.of("message", "방장은 나갈 수 없습니다. 방을 삭제하거나 마감해 주세요."));
            case "not_member": return ResponseEntity.badRequest().body(Map.of("message", "참가 중인 방이 아닙니다."));
            default: return ResponseEntity.badRequest().body(Map.of("message", "나가기에 실패했습니다."));
        }
    }

    /** 방장: 마감 */
    @PostMapping("/{roomId}/close")
    public ResponseEntity<?> close(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String result = gameRoomService.close(roomId, userId);
        switch (result) {
            case "ok": return ResponseEntity.ok(Map.of("ok", true));
            case "not_found": return ResponseEntity.status(404).body(Map.of("message", "방을 찾을 수 없습니다."));
            case "not_host": return ResponseEntity.status(403).body(Map.of("message", "방장만 마감할 수 있습니다."));
            default: return ResponseEntity.badRequest().body(Map.of("message", "마감에 실패했습니다."));
        }
    }

    /** 방장: 삭제 (비밀번호 필수) */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> delete(@PathVariable Long roomId, @RequestBody Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String password = body != null ? body.get("deletePassword") : null;
        String result = gameRoomService.delete(roomId, userId, password);
        switch (result) {
            case "ok": return ResponseEntity.ok(Map.of("ok", true));
            case "not_found": return ResponseEntity.status(404).body(Map.of("message", "방을 찾을 수 없습니다."));
            case "not_host": return ResponseEntity.status(403).body(Map.of("message", "방장만 삭제할 수 있습니다."));
            case "wrong_password": return ResponseEntity.badRequest().body(Map.of("message", "삭제 비밀번호가 일치하지 않습니다."));
            default: return ResponseEntity.badRequest().body(Map.of("message", "삭제에 실패했습니다."));
        }
    }

    /** 방 채팅용 그룹 채팅방 ID 조회 (참가자만) */
    @GetMapping("/{roomId}/chat-room-id")
    public ResponseEntity<?> getChatRoomId(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        return gameRoomService.getGroupChatRoomId(roomId, userId)
                .map(id -> ResponseEntity.<Object>ok(Map.<String, Object>of("groupChatRoomId", id)))
                .orElse(ResponseEntity.status(403).body(Map.of("message", "참가한 방이 아닙니다.")));
    }
}
