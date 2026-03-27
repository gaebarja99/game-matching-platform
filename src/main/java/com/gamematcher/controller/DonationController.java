package com.gamematcher.controller;

import com.gamematcher.dto.donation.DonationResponse;
import com.gamematcher.dto.stream.StreamResponse;
import com.gamematcher.entity.Donation;
import com.gamematcher.service.DonationService;
import com.gamematcher.service.LiveStreamService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donate")
@RequiredArgsConstructor
public class DonationController {

    private static final String SESSION_USER_ID = "userId";

    private final DonationService donationService;
    private final LiveStreamService liveStreamService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<?> donate(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Object streamIdObj = body.get("streamId");
        if (streamIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "방송 정보가 필요합니다."));
        }
        Long streamId = null;
        if (streamIdObj instanceof Number) {
            streamId = ((Number) streamIdObj).longValue();
        } else if (streamIdObj instanceof String) {
            try {
                streamId = Long.parseLong((String) streamIdObj);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "올바른 방송 번호가 아닙니다."));
            }
        }
        if (streamId == null || streamId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "올바른 방송 번호가 아닙니다."));
        }
        Object amountObj = body.get("amount");
        if (amountObj == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "팡 개수를 입력해 주세요."));
        }
        int amount = 0;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).intValue();
        } else if (amountObj instanceof String) {
            try {
                amount = Integer.parseInt((String) amountObj);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "팡 개수는 숫자로 입력해 주세요."));
            }
        }
        String message = body.containsKey("message") && body.get("message") != null
                ? body.get("message").toString().trim()
                : null;
        String videoUrl = body.containsKey("videoUrl") && body.get("videoUrl") != null
                ? body.get("videoUrl").toString().trim()
                : null;
        boolean hasVideoUrl = videoUrl != null && !videoUrl.isEmpty() && (videoUrl.startsWith("http://") || videoUrl.startsWith("https://"));
        boolean hasMessage = message != null && !message.isBlank();

        try {
            StreamResponse stream = liveStreamService.getById(streamId);
            int minVideoPang = stream.getMinVideoPang() != null ? stream.getMinVideoPang() : 0;
            int minTtsPang = stream.getMinTtsPang() != null ? stream.getMinTtsPang() : 0;
            if (hasVideoUrl && minVideoPang > 0 && amount < minVideoPang) {
                return ResponseEntity.badRequest().body(Map.of("message", "영상 후원은 " + minVideoPang + "팡 이상부터 가능합니다."));
            }
            if (hasMessage && minTtsPang > 0 && amount < minTtsPang) {
                return ResponseEntity.badRequest().body(Map.of("message", "TTS(후원 메시지)는 " + minTtsPang + "팡 이상부터 가능합니다."));
            }
        } catch (Exception ignored) {
            // stream not found will be caught in donate()
        }

        try {
            DonationResponse result = donationService.donate(userId, streamId, amount, message);
            // 해당 방송 시청 중인 모든 클라이언트(스트리머·다른 시청자)에게 후원 알림 브로드캐스트
            Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                    "type", "donation",
                    "donorName", result.getDonorName() != null ? result.getDonorName() : "",
                    "amount", result.getAmount() != null ? result.getAmount() : 0,
                    "tier", result.getTier() != null ? result.getTier() : "팡",
                    "donorMessage", result.getDonorMessage() != null ? result.getDonorMessage() : ""
            ));
            if (result.getDonorProfileImageUrl() != null) payload.put("donorProfileImageUrl", result.getDonorProfileImageUrl());
            if (result.getConsecutiveDonationDays() != null && result.getConsecutiveDonationDays() >= 1)
                payload.put("consecutiveDonationDays", result.getConsecutiveDonationDays());
            payload.put("donorUserId", userId);
            if (hasVideoUrl) {
                payload.put("videoUrl", videoUrl.length() > 512 ? videoUrl.substring(0, 512) : videoUrl);
            }
            messagingTemplate.convertAndSend("/topic/stream/" + streamId, payload);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 내 후원(사용) 내역 */
    @GetMapping("/me")
    public ResponseEntity<?> myDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Donation> list = donationService.getMyDonations(userId, page, size);
        long total = donationService.getMyDonationsCount(userId);
        List<Map<String, Object>> items = list.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("streamId", d.getStreamId());
            m.put("amount", d.getAmount());
            m.put("message", d.getMessage());
            m.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().format(ISO_FORMAT) : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("items", items, "total", total));
    }
}
