package com.gamematcher.controller;

import com.gamematcher.service.GameRoomService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game-rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private static final String SESSION_USER_ID = "userId";

    private final GameRoomService gameRoomService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."));
        }
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

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) Boolean closed,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        List<Map<String, Object>> list = gameRoomService.listRooms(game, closed, userId);
        return ResponseEntity.ok(Map.of("list", list));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> join(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."));
        }
        String result = gameRoomService.join(roomId, userId);
        return switch (result) {
            case "ok" -> ResponseEntity.ok(Map.of("ok", true));
            case "not_found" -> ResponseEntity.status(404).body(Map.of("message", "\uBC29\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
            case "closed" -> ResponseEntity.badRequest().body(Map.of("message", "\uB9C8\uAC10\uB41C \uBC29\uC785\uB2C8\uB2E4."));
            case "already_member" -> ResponseEntity.badRequest().body(Map.of("message", "\uC774\uBBF8 \uCC38\uAC00 \uC911\uC785\uB2C8\uB2E4."));
            case "full" -> ResponseEntity.badRequest().body(Map.of("message", "\uC774\uBBF8 \uC815\uC6D0\uC774 \uAC00\uB4DD \uCC2C \uBC29\uC785\uB2C8\uB2E4."));
            default -> ResponseEntity.badRequest().body(Map.of("message", "\uCC38\uAC00\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."));
        };
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leave(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."));
        }
        String result = gameRoomService.leave(roomId, userId);
        return switch (result) {
            case "ok" -> ResponseEntity.ok(Map.of("ok", true));
            case "not_found" -> ResponseEntity.status(404).body(Map.of("message", "\uBC29\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
            case "not_member" -> ResponseEntity.badRequest().body(Map.of("message", "\uCC38\uAC00 \uC911\uC778 \uBC29\uC774 \uC544\uB2D9\uB2C8\uB2E4."));
            default -> ResponseEntity.badRequest().body(Map.of("message", "\uB098\uAC00\uAE30\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."));
        };
    }

    @PostMapping("/{roomId}/close")
    public ResponseEntity<?> close(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."));
        }
        String result = gameRoomService.close(roomId, userId);
        return switch (result) {
            case "ok" -> ResponseEntity.ok(Map.of("ok", true));
            case "not_found" -> ResponseEntity.status(404).body(Map.of("message", "\uBC29\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
            case "not_host" -> ResponseEntity.status(403).body(Map.of("message", "\uBC29\uC7A5\uB9CC \uB9C8\uAC10\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."));
            default -> ResponseEntity.badRequest().body(Map.of("message", "\uB9C8\uAC10\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."));
        };
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> delete(@PathVariable Long roomId, @RequestBody Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."));
        }
        String password = body != null ? body.get("deletePassword") : null;
        String result = gameRoomService.delete(roomId, userId, password);
        return switch (result) {
            case "ok" -> ResponseEntity.ok(Map.of("ok", true));
            case "not_found" -> ResponseEntity.status(404).body(Map.of("message", "\uBC29\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
            case "not_host" -> ResponseEntity.status(403).body(Map.of("message", "\uBC29\uC7A5\uB9CC \uC0AD\uC81C\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."));
            case "wrong_password" -> ResponseEntity.badRequest().body(Map.of("message", "\uC0AD\uC81C \uBE44\uBC00\uBC88\uD638\uAC00 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."));
            default -> ResponseEntity.badRequest().body(Map.of("message", "\uC0AD\uC81C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."));
        };
    }

    @GetMapping("/{roomId}/chat-room-id")
    public ResponseEntity<?> getChatRoomId(@PathVariable Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."));
        }
        return gameRoomService.getGroupChatRoomId(roomId, userId)
                .map(id -> ResponseEntity.<Object>ok(Map.<String, Object>of("groupChatRoomId", id)))
                .orElse(ResponseEntity.status(403).body(Map.of("message", "\uCC38\uAC00\uD55C \uBC29\uC774 \uC544\uB2D9\uB2C8\uB2E4.")));
    }
}
