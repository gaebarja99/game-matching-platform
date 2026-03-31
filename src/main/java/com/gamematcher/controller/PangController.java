package com.gamematcher.controller;

import com.gamematcher.constant.MileagePurchaseType;
import com.gamematcher.constant.PangConstants;
import com.gamematcher.dto.pang.ChargeRequest;
import com.gamematcher.entity.MileagePurchase;
import com.gamematcher.entity.PangCharge;
import com.gamematcher.repository.MileagePurchaseRepository;
import com.gamematcher.service.PangService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pang")
@RequiredArgsConstructor
public class PangController {

    private static final String SESSION_USER_ID = "userId";
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PangService pangService;
    private final MileagePurchaseRepository mileagePurchaseRepository;

    @GetMapping("/balance")
    public ResponseEntity<?> balance(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        long balance = pangService.getBalance(userId);
        return ResponseEntity.ok(Map.of(
                "balance", balance,
                "pricePerPang", PangConstants.PRICE_WON_PER_PANG
        ));
    }

    @PostMapping("/charge")
    public ResponseEntity<?> charge(@RequestBody ChargeRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.badRequest().body(Map.of(
                "message", "팡 충전은 PortOne 결제 주문/승인 API를 통해서만 처리됩니다."
        ));
    }

    @GetMapping("/charges")
    public ResponseEntity<?> myCharges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<PangCharge> list = pangService.getChargeHistory(userId, page, size);
        long total = pangService.getChargeHistoryCount(userId);
        List<MileagePurchase> mileagePurchases = mileagePurchaseRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                userId,
                MileagePurchaseType.PANG,
                org.springframework.data.domain.PageRequest.of(0, Math.max(size * 3, 50))
        );

        Set<Long> matchedMileagePurchaseIds = new HashSet<>();
        List<Map<String, Object>> items = list.stream().map(charge ->
                toChargeItem(userId, charge, mileagePurchases, matchedMileagePurchaseIds)
        ).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("items", items, "total", total));
    }

    private Map<String, Object> toChargeItem(
            Long userId,
            PangCharge charge,
            List<MileagePurchase> mileagePurchases,
            Set<Long> matchedMileagePurchaseIds
    ) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", charge.getId());
        item.put("pangAmount", charge.getPangAmount());
        item.put("priceWon", charge.getPriceWon());
        item.put("orderId", charge.getOrderId());
        item.put("impUid", charge.getImpUid());

        boolean refunded = charge.getPangAmount() != null
                && charge.getPangAmount() > 0
                && pangService.isRefundedCharge(userId, charge.getOrderId(), charge.getImpUid());
        item.put("refunded", refunded);
        item.put("refundable", !refunded
                && charge.getPangAmount() != null
                && charge.getPangAmount() > 0
                && ((charge.getOrderId() != null && !charge.getOrderId().isBlank())
                || (charge.getImpUid() != null && !charge.getImpUid().isBlank())));
        item.put("detail", resolveChargeDetail(charge, mileagePurchases, matchedMileagePurchaseIds));
        item.put("createdAt", charge.getCreatedAt() != null ? charge.getCreatedAt().format(ISO_FORMAT) : null);
        return item;
    }

    private String resolveChargeDetail(
            PangCharge charge,
            List<MileagePurchase> mileagePurchases,
            Set<Long> matchedMileagePurchaseIds
    ) {
        if (charge == null || charge.getPangAmount() == null) {
            return "--";
        }
        if (charge.getPangAmount() < 0) {
            return "환불";
        }
        if ((charge.getOrderId() != null && !charge.getOrderId().isBlank())
                || (charge.getImpUid() != null && !charge.getImpUid().isBlank())) {
            return "결제";
        }

        MileagePurchase matched = findMatchingMileagePurchase(charge, mileagePurchases, matchedMileagePurchaseIds);
        if (matched != null) {
            matchedMileagePurchaseIds.add(matched.getId());
            return "마일리지";
        }
        return "이벤트";
    }

    private MileagePurchase findMatchingMileagePurchase(
            PangCharge charge,
            List<MileagePurchase> mileagePurchases,
            Set<Long> matchedMileagePurchaseIds
    ) {
        if (charge == null || mileagePurchases == null || mileagePurchases.isEmpty()) {
            return null;
        }

        Integer pangAmount = charge.getPangAmount();
        LocalDateTime createdAt = charge.getCreatedAt();
        if (pangAmount == null || createdAt == null) {
            return null;
        }

        List<MileagePurchase> candidates = new ArrayList<>();
        for (MileagePurchase purchase : mileagePurchases) {
            if (purchase == null || purchase.getId() == null || matchedMileagePurchaseIds.contains(purchase.getId())) {
                continue;
            }
            if (purchase.getPangAmount() == null || !pangAmount.equals(purchase.getPangAmount())) {
                continue;
            }
            if (purchase.getCreatedAt() == null) {
                continue;
            }

            long secondsGap = Math.abs(Duration.between(createdAt, purchase.getCreatedAt()).getSeconds());
            if (secondsGap <= 180) {
                candidates.add(purchase);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((left, right) -> Long.compare(
                Math.abs(Duration.between(createdAt, left.getCreatedAt()).getSeconds()),
                Math.abs(Duration.between(createdAt, right.getCreatedAt()).getSeconds())
        ));
        return candidates.get(0);
    }
}
