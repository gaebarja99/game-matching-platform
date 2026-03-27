package com.gamematcher.controller;

import com.gamematcher.dto.mileage.MileagePangPurchaseRequest;
import com.gamematcher.dto.mileage.MileageSubscriptionPurchaseRequest;
import com.gamematcher.entity.MileagePurchase;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.service.MileageShopService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mileage-shop")
@RequiredArgsConstructor
public class MileageShopController {

    private static final String SESSION_USER_ID = "userId";
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MileageShopService mileageShopService;
    private final UserRepository userRepository;

    @GetMapping("/streamers/search")
    public ResponseEntity<?> searchStreamers(@RequestParam("q") String q, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(List.of());

        String keyword = q == null ? "" : q.trim();
        if (keyword.length() < 2) return ResponseEntity.ok(List.of());

        List<Map<String, Object>> items = userRepository.searchByLoginIdOrNickname(keyword, userId).stream()
                .limit(10)
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("nickname", u.getNickname());
                    m.put("profileImageUrl", u.getProfileImageUrl());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/prices")
    public ResponseEntity<?> prices() {
        return ResponseEntity.ok(Map.of(
                "pangMileageCostPerPang", mileageShopService.getPangMileageCostPerPang(),
                "subscriptionTicketCost", mileageShopService.getSubscriptionTicketCost(),
                "adFree30DaysCost", mileageShopService.getAdFree30DaysCost()
        ));
    }

    @PostMapping("/pang")
    public ResponseEntity<?> buyPang(@RequestBody MileagePangPurchaseRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (request.getPangAmount() == null) return ResponseEntity.badRequest().body(Map.of("message", "팡 개수를 입력해 주세요."));
        try {
            MileageShopService.Result result = mileageShopService.buyPang(userId, request.getPangAmount());
            return ResponseEntity.ok(Map.of(
                    "message", result.message(),
                    "mileageBalance", result.mileageBalance() != null ? result.mileageBalance() : 0L,
                    "pangBalance", result.pangBalance() != null ? result.pangBalance() : 0L
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/subscription")
    public ResponseEntity<?> buySubscription(@RequestBody MileageSubscriptionPurchaseRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        if (request.getUserId() == null) return ResponseEntity.badRequest().body(Map.of("message", "구독 대상이 필요합니다."));
        try {
            MileageShopService.Result result = mileageShopService.buySubscriptionTicket(userId, request.getUserId());
            return ResponseEntity.ok(Map.of(
                    "message", result.message(),
                    "mileageBalance", result.mileageBalance() != null ? result.mileageBalance() : 0L,
                    "subscribed", true
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/adfree")
    public ResponseEntity<?> buyAdFree(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        try {
            MileageShopService.Result result = mileageShopService.buyAdFree30Days(userId);
            return ResponseEntity.ok(Map.of(
                    "message", result.message(),
                    "mileageBalance", result.mileageBalance() != null ? result.mileageBalance() : 0L
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(401).build();
        List<MileagePurchase> list = mileageShopService.getHistory(userId, page, size);
        long total = mileageShopService.getHistoryCount(userId);
        List<Map<String, Object>> items = list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("type", p.getType() != null ? p.getType().name() : "");
            long displayMileageCost = p.getMileageCost() != null ? p.getMileageCost() : 0L;
            if (p.getType() != null) {
                switch (p.getType()) {
                    case ADMIN_GIFT -> displayMileageCost = p.getMileageCost() != null ? p.getMileageCost() : 0L;
                    case SUBSCRIPTION_TICKET -> displayMileageCost = mileageShopService.getSubscriptionTicketCost();
                    case AD_FREE_30_DAYS -> displayMileageCost = mileageShopService.getAdFree30DaysCost();
                    default -> {
                    }
                }
            }
            m.put("mileageCost", displayMileageCost);
            m.put("pangAmount", p.getPangAmount());
            m.put("targetUserId", p.getTargetUserId());
            m.put("targetUserNickname", p.getTargetUserId() == null
                    ? null
                    : userRepository.findById(p.getTargetUserId())
                            .map(u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getLoginId())
                            .orElse(null));
            m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().format(ISO_FORMAT) : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("items", items, "total", total));
    }
}
