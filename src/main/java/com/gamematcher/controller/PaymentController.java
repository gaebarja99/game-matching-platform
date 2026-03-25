package com.gamematcher.controller;

import com.gamematcher.dto.payment.ConfirmPaymentRequest;
import com.gamematcher.dto.payment.CreateOrderRequest;
import com.gamematcher.dto.payment.RefundPaymentRequest;
import com.gamematcher.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private static final String SESSION_USER_ID = "userId";

    private final PaymentService paymentService;

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (request.getPangAmount() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "충전할 팡 개수를 입력해 주세요."));
        }
        try {
            PaymentService.CreateOrderResult result = paymentService.createPangOrder(userId, request.getPangAmount());
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

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody ConfirmPaymentRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String impUid = request.resolveImpUid();
        if (request.getOrderId() == null || request.getOrderId().isBlank() || impUid == null || impUid.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "orderId와 impUid(paymentId)가 필요합니다."));
        }

        try {
            PaymentService.ConfirmResult result = paymentService.confirmPangPayment(userId, request.getOrderId(), impUid);
            return ResponseEntity.ok(Map.of(
                    "success", result.success(),
                    "balance", result.balance(),
                    "message", result.message()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refund(@RequestBody RefundPaymentRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if ((request.getOrderId() == null || request.getOrderId().isBlank())
                && (request.getImpUid() == null || request.getImpUid().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "orderId 또는 impUid가 필요합니다."));
        }

        try {
            PaymentService.ConfirmResult result = paymentService.refundPangPayment(
                    userId,
                    request.getOrderId(),
                    request.getImpUid(),
                    request.getReason()
            );
            return ResponseEntity.ok(Map.of(
                    "success", result.success(),
                    "balance", result.balance(),
                    "message", result.message()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/webhook/portone")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        try {
            paymentService.handleWebhook(payload);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
