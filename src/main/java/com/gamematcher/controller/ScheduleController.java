package com.gamematcher.controller;

import com.gamematcher.dto.schedule.ScheduleItemDto;
import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.service.BroadcastScheduleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private static final String SESSION_USER_ID = "userId";
    private static final String OPERATOR_LOGIN_ID = "asd8219";

    private final BroadcastScheduleService broadcastScheduleService;
    private final UserRepository userRepository;

    private Long getCurrentUserId(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        return id instanceof Long ? (Long) id : null;
    }

    private boolean isOperator(HttpSession session) {
        Long uid = getCurrentUserId(session);
        if (uid == null) return false;
        return userRepository.findById(uid)
                .map(User::getLoginId)
                .filter(OPERATOR_LOGIN_ID::equals)
                .isPresent();
    }

    @GetMapping
    public ResponseEntity<List<ScheduleItemDto>> list() {
        return ResponseEntity.ok(broadcastScheduleService.list());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpSession session) {
        if (!isOperator(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "운영자만 일정을 등록할 수 있습니다."));
        }
        String title = (String) body.get("title");
        String scheduleAtStr = (String) body.get("scheduleAt");
        String link = (String) body.get("link");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "제목을 입력해 주세요."));
        }
        LocalDateTime scheduleAt = parseScheduleAt(scheduleAtStr);
        if (scheduleAt == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "일시 형식이 올바르지 않습니다. (예: 2025-03-10T12:40)"));
        }
        ScheduleItemDto created = broadcastScheduleService.create(title, scheduleAt, link);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        if (!isOperator(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "운영자만 일정을 수정할 수 있습니다."));
        }
        String title = (String) body.get("title");
        String scheduleAtStr = (String) body.get("scheduleAt");
        String link = (String) body.get("link");
        LocalDateTime scheduleAt = parseScheduleAt(scheduleAtStr);
        ScheduleItemDto updated = broadcastScheduleService.update(id, title, scheduleAt, link);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isOperator(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "운영자만 일정을 삭제할 수 있습니다."));
        }
        if (!broadcastScheduleService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    private static LocalDateTime parseScheduleAt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            s = s.trim().replace(" ", "T");
            if (s.length() >= 19) {
                return LocalDateTime.parse(s.substring(0, 19));
            }
            if (s.length() >= 16) {
                return LocalDateTime.parse(s.substring(0, 16) + ":00");
            }
            if (s.length() >= 10) {
                return LocalDateTime.parse(s.substring(0, 10) + "T00:00:00");
            }
        } catch (Exception ignored) {}
        return null;
    }
}
