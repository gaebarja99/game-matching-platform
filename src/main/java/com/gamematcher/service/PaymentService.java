package com.gamematcher.service;

import com.gamematcher.config.PaymentProperties;
import com.gamematcher.constant.MileagePurchaseType;
import com.gamematcher.constant.PangConstants;
import com.gamematcher.constant.PaymentOrderKind;
import com.gamematcher.entity.MileagePurchase;
import com.gamematcher.entity.PaymentOrder;
import com.gamematcher.entity.User;
import com.gamematcher.repository.MileagePurchaseRepository;
import com.gamematcher.repository.PaymentOrderRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * PortOne V1 결제 연동 및 결제 검증/취소 처리를 담당한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int MIN_PANG = 100;
    private static final int MAX_PANG = 999_999_999;
    private static final long AD_FREE_PRICE_WON = 8_900L;

    private static final String IAMPORT_GET_TOKEN = "https://api.iamport.kr/users/getToken";
    private static final String IAMPORT_GET_PAYMENT = "https://api.iamport.kr/payments/";
    private static final String IAMPORT_CANCEL_PAYMENT = "https://api.iamport.kr/payments/cancel";

    private final PaymentOrderRepository paymentOrderRepository;
    private final PangService pangService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final PaymentProperties paymentProperties;
    private final UserRepository userRepository;
    private final MileagePurchaseRepository mileagePurchaseRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public CreateOrderResult createPangOrder(Long userId, int pangAmount) {
        if (pangAmount < MIN_PANG || pangAmount > MAX_PANG) {
            throw new IllegalArgumentException("충전 가능한 팡 수량은 100 이상 999,999,999 이하여야 합니다.");
        }
        if (paymentProperties.getApiKey() == null || paymentProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("결제 설정이 비어 있습니다. 관리자에게 문의해 주세요.");
        }

        long amountWon = Math.round(pangAmount * PangConstants.PRICE_WON_PER_PANG);
        String orderId = "GM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        PaymentOrder order = new PaymentOrder();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setKind(PaymentOrderKind.PANG_CHARGE);
        order.setPangAmount(pangAmount);
        order.setAmountWon(amountWon);
        order.setStatus("PENDING");
        paymentOrderRepository.save(order);

        String storeId = resolveClientStoreId();
        String pg = paymentProperties.getPg() != null ? paymentProperties.getPg() : "html5_inicis.INIpayTest";
        String payMethod = paymentProperties.getPayMethod() != null ? paymentProperties.getPayMethod() : "card";

        return new CreateOrderResult(orderId, amountWon, "GameMatcher 팡 " + pangAmount + "개 충전", storeId, pg, payMethod);
    }

    @Transactional
    public CreateOrderResult createSubscriptionOrder(Long subscriberId, Long streamerId) {
        if (subscriberId == null || streamerId == null) {
            throw new IllegalArgumentException("구독 결제를 진행할 대상 정보가 올바르지 않습니다.");
        }
        if (subscriberId.equals(streamerId)) {
            throw new IllegalArgumentException("본인 채널에는 구독할 수 없습니다.");
        }
        if (paymentProperties.getApiKey() == null || paymentProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("결제 설정이 비어 있습니다. 관리자에게 문의해 주세요.");
        }
        if (subscriptionService.isSubscribed(subscriberId, streamerId)) {
            throw new IllegalArgumentException("이미 구독 중인 스트리머입니다.");
        }

        long amountWon = subscriptionService.getSubscriptionPriceWon();
        String orderId = "GM-SUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        PaymentOrder order = new PaymentOrder();
        order.setOrderId(orderId);
        order.setUserId(subscriberId);
        order.setKind(PaymentOrderKind.SUBSCRIPTION);
        order.setTargetUserId(streamerId);
        order.setPangAmount(0);
        order.setAmountWon(amountWon);
        order.setStatus("PENDING");
        paymentOrderRepository.save(order);

        String storeId = resolveClientStoreId();
        String pg = paymentProperties.getPg() != null ? paymentProperties.getPg() : "html5_inicis.INIpayTest";
        String payMethod = paymentProperties.getPayMethod() != null ? paymentProperties.getPayMethod() : "card";

        return new CreateOrderResult(orderId, amountWon, "GameMatcher 스트리머 구독 결제", storeId, pg, payMethod);
    }

    @Transactional
    public CreateOrderResult createAdFreeOrder(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("결제를 진행할 사용자 정보가 올바르지 않습니다.");
        }
        if (paymentProperties.getApiKey() == null || paymentProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("결제 설정이 비어 있습니다. 관리자에게 문의해 주세요.");
        }

        String orderId = "GM-AD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        PaymentOrder order = new PaymentOrder();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setKind(PaymentOrderKind.AD_FREE);
        order.setPangAmount(0);
        order.setAmountWon(AD_FREE_PRICE_WON);
        order.setStatus("PENDING");
        paymentOrderRepository.save(order);

        String storeId = resolveClientStoreId();
        String pg = paymentProperties.getPg() != null ? paymentProperties.getPg() : "html5_inicis.INIpayTest";
        String payMethod = paymentProperties.getPayMethod() != null ? paymentProperties.getPayMethod() : "card";

        return new CreateOrderResult(orderId, AD_FREE_PRICE_WON, "GameMatcher 광고 제거 30일권", storeId, pg, payMethod);
    }

    @Transactional
    public ConfirmResult confirmPangPayment(Long userId, String orderId, String impUid) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문 정보를 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("현재 사용자와 주문 정보가 일치하지 않습니다.");
        }
        if (order.getKind() != PaymentOrderKind.PANG_CHARGE) {
            throw new IllegalArgumentException("팡 충전 주문이 아닙니다.");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            long balance = pangService.getBalance(userId);
            return new ConfirmResult(true, balance, "이미 결제가 완료된 주문입니다.");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("현재 상태에서는 결제 확인을 진행할 수 없습니다.");
        }
        if (paymentProperties.getApiKey() == null || paymentProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("결제 설정이 비어 있습니다.");
        }

        verifyAndMarkPayment(order, orderId, impUid);
        long newBalance = pangService.charge(userId, order.getPangAmount(), order.getOrderId(), impUid);
        saveMileageRewardHistory(userId, Math.round(order.getAmountWon() * 5.0 / 100.0), MileagePurchaseType.PANG_PAYMENT_REWARD);
        notificationService.createForPaymentCompleted(userId, order.getPangAmount(), order.getAmountWon());
        return new ConfirmResult(true, newBalance, "팡 충전이 완료되었습니다.");
    }

    @Transactional
    public ConfirmResult confirmSubscriptionPayment(Long subscriberId, String orderId, String impUid) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문 정보를 찾을 수 없습니다."));

        if (!order.getUserId().equals(subscriberId)) {
            throw new IllegalArgumentException("현재 사용자와 주문 정보가 일치하지 않습니다.");
        }
        if (order.getKind() != PaymentOrderKind.SUBSCRIPTION) {
            throw new IllegalArgumentException("구독 주문이 아닙니다.");
        }
        if (order.getTargetUserId() == null) {
            throw new IllegalArgumentException("구독 대상 정보가 누락되었습니다.");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            return new ConfirmResult(true, 0L, "이미 결제가 완료된 주문입니다.");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("현재 상태에서는 결제 확인을 진행할 수 없습니다.");
        }

        verifyAndMarkPayment(order, orderId, impUid);
        subscriptionService.grantSubscription(subscriberId, order.getTargetUserId(), true);
        saveMileageRewardHistory(subscriberId, Math.round(order.getAmountWon() * 10.0 / 100.0), MileagePurchaseType.SUBSCRIPTION_PAYMENT_REWARD);
        notificationService.createForNewSubscriber(order.getTargetUserId(), subscriberId);
        return new ConfirmResult(true, 0L, "구독 결제가 완료되었습니다.");
    }

    @Transactional
    public ConfirmResult confirmAdFreePayment(Long userId, String orderId, String impUid) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문 정보를 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("현재 사용자와 주문 정보가 일치하지 않습니다.");
        }
        if (order.getKind() != PaymentOrderKind.AD_FREE) {
            throw new IllegalArgumentException("광고 제거 주문이 아닙니다.");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            return new ConfirmResult(true, 0L, "이미 결제가 완료된 주문입니다.");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("현재 상태에서는 결제 확인을 진행할 수 없습니다.");
        }

        verifyAndMarkPayment(order, orderId, impUid);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        LocalDateTime base = user.getAdFreeUntil() != null && user.getAdFreeUntil().isAfter(LocalDateTime.now())
                ? user.getAdFreeUntil()
                : LocalDateTime.now();
        user.setAdFreeUntil(base.plusDays(30));
        long currentMileage = user.getMileage() != null ? user.getMileage() : 0L;
        long rewardMileage = Math.round(order.getAmountWon() * 5.0 / 100.0);
        user.setMileage(currentMileage + rewardMileage);
        userRepository.save(user);
        saveMileageRewardHistory(userId, rewardMileage, MileagePurchaseType.AD_FREE_PAYMENT_REWARD);

        return new ConfirmResult(true, 0L, "광고 제거 결제가 완료되었습니다.");
    }

    @Transactional
    public ConfirmResult refundPangPayment(Long userId, String orderId, String impUid, String reason) {
        PaymentOrder order = findOrderForRefund(orderId, impUid);
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("현재 사용자와 환불 대상 주문이 일치하지 않습니다.");
        }
        if (order.getKind() != PaymentOrderKind.PANG_CHARGE) {
            throw new IllegalArgumentException("팡 충전 주문만 환불할 수 있습니다.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            long balance = pangService.getBalance(userId);
            return new ConfirmResult(true, balance, "이미 환불된 주문입니다.");
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("결제가 완료된 주문만 환불할 수 있습니다.");
        }

        String targetImpUid = impUid != null && !impUid.isBlank() ? impUid : order.getImpUid();
        if (targetImpUid == null || targetImpUid.isBlank()) {
            throw new IllegalArgumentException("환불에 필요한 impUid 정보를 찾을 수 없습니다.");
        }

        cancelPaymentAtPortOne(targetImpUid, order.getOrderId(), reason);
        long balance = pangService.revokeCharge(order.getUserId(), order.getPangAmount(), order.getOrderId(), targetImpUid);
        order.setStatus("CANCELLED");
        paymentOrderRepository.save(order);
        notificationService.createForPaymentRefunded(order.getUserId(), order.getPangAmount(), order.getAmountWon());
        return new ConfirmResult(true, balance, "환불이 완료되었습니다.");
    }

    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        String impUid = getString(payload, "imp_uid", "impUid", "payment_id", "paymentId");
        String orderId = getString(payload, "merchant_uid", "merchantUid", "order_id", "orderId");
        String statusRaw = getString(payload, "status");
        String status = statusRaw != null ? statusRaw.trim().toLowerCase() : "";

        if ((impUid == null || impUid.isBlank()) && (orderId == null || orderId.isBlank())) {
            log.warn("Payment webhook ignored: missing imp_uid and merchant_uid. payload={}", payload);
            return;
        }

        Optional<PaymentOrder> orderOpt = Optional.empty();
        if (orderId != null && !orderId.isBlank()) {
            orderOpt = paymentOrderRepository.findByOrderId(orderId);
        }
        if (orderOpt.isEmpty() && impUid != null && !impUid.isBlank()) {
            orderOpt = paymentOrderRepository.findByImpUid(impUid);
        }
        if (orderOpt.isEmpty()) {
            log.warn("Payment webhook order not found. impUid={}, orderId={}, status={}", impUid, orderId, status);
            return;
        }

        PaymentOrder order = orderOpt.get();
        if ((order.getImpUid() == null || order.getImpUid().isBlank()) && impUid != null && !impUid.isBlank()) {
            order.setImpUid(impUid);
            paymentOrderRepository.save(order);
        }

        if (!isCancelledStatus(status)) {
            return;
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        if ("COMPLETED".equalsIgnoreCase(order.getStatus()) && order.getKind() == PaymentOrderKind.PANG_CHARGE) {
            pangService.revokeCharge(order.getUserId(), order.getPangAmount(), order.getOrderId(), order.getImpUid());
            notificationService.createForPaymentRefunded(order.getUserId(), order.getPangAmount(), order.getAmountWon());
        }

        order.setStatus("CANCELLED");
        paymentOrderRepository.save(order);
        log.info("Payment cancelled by webhook. orderId={}, impUid={}, status={}", order.getOrderId(), order.getImpUid(), status);
    }

    private void verifyAndMarkPayment(PaymentOrder order, String orderId, String impUid) {
        if (impUid == null || impUid.isBlank()) {
            throw new IllegalArgumentException("결제 검증에 필요한 imp_uid 값이 없습니다.");
        }
        if (!impUid.startsWith("imp_")) {
            throw new IllegalArgumentException("imp_uid 형식이 올바르지 않습니다.");
        }

        String token = getPortOneAccessToken();
        Map<String, Object> paymentData = getPortOnePayment(impUid, token);

        String respMerchantUid = (String) paymentData.get("merchant_uid");
        Object amountObj = paymentData.get("amount");
        Long amountPaid = amountObj instanceof Number ? Math.round(((Number) amountObj).doubleValue()) : null;
        String status = (String) paymentData.get("status");

        if (!orderId.equals(respMerchantUid) || amountPaid == null || !order.getAmountWon().equals(amountPaid)) {
            order.setStatus("FAILED");
            paymentOrderRepository.save(order);
            log.warn("Payment mismatch. orderId={}, impUid={}, merchant_uid={}, paid={}, expected={}",
                    orderId, impUid, respMerchantUid, amountPaid, order.getAmountWon());
            throw new IllegalArgumentException("결제 금액 또는 주문 정보가 일치하지 않습니다.");
        }

        if (!"paid".equalsIgnoreCase(status)) {
            order.setStatus("FAILED");
            paymentOrderRepository.save(order);
            throw new IllegalArgumentException("결제 상태가 paid가 아닙니다. status=" + status);
        }

        order.setStatus("COMPLETED");
        order.setImpUid(impUid);
        order.setCompletedAt(java.time.LocalDateTime.now());
        paymentOrderRepository.save(order);
    }

    private String getPortOneAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "imp_key", paymentProperties.getApiKey(),
                "imp_secret", paymentProperties.getApiSecret()
        );

        ResponseEntity<?> response;
        try {
            response = restTemplate.postForEntity(IAMPORT_GET_TOKEN, new HttpEntity<>(body, headers), Map.class);
        } catch (HttpStatusCodeException exception) {
            log.warn("PortOne token request failed: status={}, body={}", exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new IllegalStateException("PortOne 토큰 발급에 실패했습니다. HTTP " + exception.getStatusCode().value());
        } catch (Exception exception) {
            log.warn("PortOne token request failed", exception);
            throw new IllegalStateException("PortOne 토큰 발급 중 알 수 없는 오류가 발생했습니다.");
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("PortOne 토큰 발급에 실패했습니다. HTTP " + response.getStatusCode().value());
        }

        Object bodyObj = response.getBody();
        if (!(bodyObj instanceof Map<?, ?> bodyMap)) {
            throw new IllegalStateException("PortOne 토큰 응답 형식이 올바르지 않습니다.");
        }

        Number code = bodyMap.get("code") instanceof Number number ? number : null;
        String message = bodyMap.get("message") != null ? bodyMap.get("message").toString() : null;
        if (code != null && code.intValue() != 0) {
            throw new IllegalStateException("PortOne 토큰 발급에 실패했습니다. "
                    + (message != null ? message : "unknown")
                    + " (code=" + code.intValue() + ")");
        }

        Object responseObj = bodyMap.get("response");
        if (responseObj instanceof Map<?, ?> responseMap) {
            Object accessToken = responseMap.get("access_token");
            if (accessToken != null) return accessToken.toString();
        }

        throw new IllegalStateException("PortOne 토큰 응답에 access_token이 없습니다.");
    }

    private String resolveClientStoreId() {
        if (paymentProperties.getCustomerCode() != null && !paymentProperties.getCustomerCode().isBlank()) {
            return paymentProperties.getCustomerCode().trim();
        }
        return paymentProperties.getStoreId() != null ? paymentProperties.getStoreId().trim() : "";
    }

    private PaymentOrder findOrderForRefund(String orderId, String impUid) {
        if (orderId != null && !orderId.isBlank()) {
            return paymentOrderRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 주문 정보를 찾을 수 없습니다."));
        }
        if (impUid != null && !impUid.isBlank()) {
            return paymentOrderRepository.findByImpUid(impUid)
                    .orElseThrow(() -> new IllegalArgumentException("해당 주문 정보를 찾을 수 없습니다."));
        }
        throw new IllegalArgumentException("orderId 또는 impUid 중 하나는 반드시 필요합니다.");
    }

    @SuppressWarnings("unchecked")
    private void cancelPaymentAtPortOne(String impUid, String orderId, String reason) {
        String token = getPortOneAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        Map<String, Object> body = new HashMap<>();
        body.put("imp_uid", impUid);
        body.put("merchant_uid", orderId);
        body.put("reason", (reason != null && !reason.isBlank()) ? reason : "사용자 요청 환불");

        try {
            ResponseEntity<?> response = restTemplate.postForEntity(
                    IAMPORT_CANCEL_PAYMENT,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("PortOne 결제 취소에 실패했습니다. HTTP " + response.getStatusCode().value());
            }

            Object bodyObj = response.getBody();
            if (!(bodyObj instanceof Map<?, ?> bodyMap)) {
                throw new IllegalStateException("PortOne 결제 취소 응답 형식이 올바르지 않습니다.");
            }

            Number code = bodyMap.get("code") instanceof Number number ? number : null;
            String message = bodyMap.get("message") != null ? bodyMap.get("message").toString() : null;
            if (code != null && code.intValue() != 0) {
                throw new IllegalStateException("PortOne 결제 취소에 실패했습니다. "
                        + (message != null ? message : "unknown")
                        + " (code=" + code.intValue() + ")");
            }
        } catch (HttpStatusCodeException exception) {
            log.warn("PortOne cancel failed: imp_uid={}, status={}, body={}", impUid, exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new IllegalStateException("PortOne 결제 취소에 실패했습니다. HTTP " + exception.getStatusCode().value());
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("PortOne 결제 취소 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPortOnePayment(String impUid, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        String paymentLookupUrl = IAMPORT_GET_PAYMENT + impUid + (isSandboxPg() ? "?include_sandbox=true" : "");

        try {
            ResponseEntity<?> response = restTemplate.exchange(
                    paymentLookupUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalArgumentException("PortOne 결제 조회에 실패했습니다. HTTP " + response.getStatusCode().value());
            }

            Object bodyObj = response.getBody();
            if (!(bodyObj instanceof Map<?, ?> bodyMap)) {
                throw new IllegalArgumentException("PortOne 결제 조회 응답 형식이 올바르지 않습니다.");
            }

            Number code = bodyMap.get("code") instanceof Number number ? number : null;
            String message = bodyMap.get("message") != null ? bodyMap.get("message").toString() : null;
            if (code != null && code.intValue() != 0) {
                throw new IllegalArgumentException("PortOne 결제 조회에 실패했습니다. "
                        + (message != null ? message : "unknown")
                        + " (code=" + code.intValue() + ")");
            }

            Object responseObj = bodyMap.get("response");
            if (!(responseObj instanceof Map<?, ?> responseMap)) {
                throw new IllegalArgumentException("PortOne 결제 조회 응답에 결제 정보가 없습니다.");
            }
            return (Map<String, Object>) responseMap;
        } catch (HttpStatusCodeException exception) {
            log.warn("PortOne getPayment failed: imp_uid={}, status={}, body={}", impUid, exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new IllegalArgumentException("PortOne 결제 조회에 실패했습니다. HTTP " + exception.getStatusCode().value());
        } catch (Exception exception) {
            log.warn("PortOne getPayment failed: imp_uid={}", impUid, exception);
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("PortOne 결제 조회 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    private boolean isSandboxPg() {
        String pg = paymentProperties.getPg();
        if (pg == null) return false;
        String normalized = pg.toLowerCase();
        return normalized.contains("test") || normalized.contains("sandbox");
    }

    private static boolean isCancelledStatus(String status) {
        if (status == null) return false;
        return "cancelled".equalsIgnoreCase(status)
                || "canceled".equalsIgnoreCase(status)
                || "cancel".equalsIgnoreCase(status)
                || "paid_cancelled".equalsIgnoreCase(status);
    }

    private static String getString(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) return null;
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                String text = value.toString().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private void saveMileageRewardHistory(Long userId, long mileageAmount, MileagePurchaseType type) {
        if (userId == null || mileageAmount <= 0 || type == null) {
            return;
        }

        MileagePurchase purchase = new MileagePurchase();
        purchase.setUserId(userId);
        purchase.setType(type);
        purchase.setMileageCost(mileageAmount);
        mileagePurchaseRepository.save(purchase);
    }

    public record CreateOrderResult(String orderId, long amountWon, String orderName, String storeId, String pg, String payMethod) {}

    public record ConfirmResult(boolean success, long balance, String message) {}
}
