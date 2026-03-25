package com.gamematcher.controller;

import com.gamematcher.service.SubscriptionService;
import com.gamematcher.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final String SESSION_USER_ID = "userId";

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    /** 현재 로그인 사용자가 해당 스트리머를 구독 중인지 조회 */
    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam Long userId, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        boolean subscribed = subscriptionService.isSubscribed(myId, userId);
        return ResponseEntity.ok(Map.of("subscribed", subscribed));
    }

    /** (구버전) 직접 구독 API는 비활성화. 결제 또는 마일리지 상점을 사용 */
    @PostMapping
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Long> body, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long streamerId = body != null ? body.get("userId") : null;
        if (streamerId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID(스트리머)가 필요합니다."));
        }
        if (myId.equals(streamerId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "본인 채널은 구독할 수 없습니다."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "구독은 결제 또는 마일리지 상점에서 구매해 주세요."));
    }

    /** 구독 결제 주문 생성 (포트원 결제창 호출용) */
    @PostMapping("/orders")
    public ResponseEntity<?> createSubscriptionOrder(@RequestBody Map<String, Long> body, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Long streamerId = body != null ? body.get("userId") : null;
        if (streamerId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID(스트리머)가 필요합니다."));
        }
        try {
            PaymentService.CreateOrderResult result = paymentService.createSubscriptionOrder(myId, streamerId);
            return ResponseEntity.ok(Map.of(
                    "orderId", result.orderId(),
                    "amount", result.amountWon(),
                    "orderName", result.orderName(),
                    "storeId", result.storeId() != null ? result.storeId() : "",
                    "pg", result.pg() != null ? result.pg() : "html5_inicis.INIpayTest",
                    "payMethod", result.payMethod() != null ? result.payMethod() : "card"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("message", e.getMessage()));
        }
    }

    /** 구독 결제 검증 및 구독 생성 */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSubscription(@RequestBody Map<String, String> body, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        String orderId = body != null ? body.get("orderId") : null;
        String impUid = body != null ? body.get("impUid") : null;
        if (orderId == null || orderId.isBlank() || impUid == null || impUid.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "orderId와 impUid가 필요합니다."));
        }
        try {
            PaymentService.ConfirmResult result = paymentService.confirmSubscriptionPayment(myId, orderId, impUid);
            return ResponseEntity.ok(Map.of("subscribed", true, "message", result.message()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("subscribed", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("subscribed", false, "message", e.getMessage()));
        }
    }

    /** 구독 취소 */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unsubscribe(@PathVariable Long userId, HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        subscriptionService.unsubscribe(myId, userId);
        return ResponseEntity.ok(Map.of("subscribed", false));
    }

    /** 내 채널 구독자 목록 (스트리머용) */
    @GetMapping("/subscribers")
    public ResponseEntity<?> subscribers(HttpSession session) {
        Long myId = (Long) session.getAttribute(SESSION_USER_ID);
        if (myId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<?> list = subscriptionService.getSubscriberList(myId).stream()
                .map(s -> Map.of(
                        "userId", s.getUserId(),
                        "nickname", s.getNickname() != null ? s.getNickname() : "",
                        "loginId", s.getLoginId() != null ? s.getLoginId() : "",
                        "subscribedAt", s.getSubscribedAt() != null ? s.getSubscribedAt().toString() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("list", list, "total", list.size()));
    }
}
