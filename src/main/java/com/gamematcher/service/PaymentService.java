package com.gamematcher.service;

import com.gamematcher.config.PaymentProperties;
import com.gamematcher.constant.PangConstants;
import com.gamematcher.constant.PaymentOrderKind;
import com.gamematcher.entity.PaymentOrder;
import com.gamematcher.repository.PaymentOrderRepository;
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

/**
 * PortOne V1(?????밸븶??ｋ뜦???? ??β뼯援?????????댄뱼癲???癲ル슢怡??귦룈?? */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int MIN_PANG = 100;
    private static final int MAX_PANG = 999_999_999;

    private static final String IAMPORT_GET_TOKEN = "https://api.iamport.kr/users/getToken";
    private static final String IAMPORT_GET_PAYMENT = "https://api.iamport.kr/payments/";
    private static final String IAMPORT_CANCEL_PAYMENT = "https://api.iamport.kr/payments/cancel";

    private final PaymentOrderRepository paymentOrderRepository;
    private final PangService pangService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final PaymentProperties paymentProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public CreateOrderResult createPangOrder(Long userId, int pangAmount) {
        if (pangAmount < MIN_PANG || pangAmount > MAX_PANG) {
            throw new IllegalArgumentException("??濡ろ뜐筌?쓣?????? 100 ?????鶯??????⑤챷竊??????용츧????ロ뒌??");
        }
        if (paymentProperties.getApiKey() == null || paymentProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("??β뼯援???????濚밸Ŧ?????????룸??????????????낆젵. ????댁삩????숆강???????????筌?????????용츧????ロ뒌??");
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

        String storeId = paymentProperties.getClientInitKey();
        String pg = paymentProperties.getPg() != null ? paymentProperties.getPg() : "html5_inicis.INIpayTest";
        String payMethod = paymentProperties.getPayMethod() != null ? paymentProperties.getPayMethod() : "card";

        return new CreateOrderResult(orderId, amountWon, "GameMatcher ??" + pangAmount + "媛?異⑹쟾", storeId, pg, payMethod);
    }

    @Transactional
    public CreateOrderResult createSubscriptionOrder(Long subscriberId, Long streamerId) {
        if (subscriberId == null || streamerId == null) {
            throw new IllegalArgumentException("???????꿔꺂??????쒐춯誘↔데鸚????쎛 ????癲?? ???????????낆젵.");
        }
        if (subscriberId.equals(streamerId)) {
            throw new IllegalArgumentException("???ㅼ뒧?戮レ땡??????嶺?? ?????節뉗땡?????????욱룏???????낆젵.");
        }
        if (paymentProperties.getApiKey() == null || paymentProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("??β뼯援???????濚밸Ŧ?????????룸??????????????낆젵. ????댁삩????숆강???????????筌?????????용츧????ロ뒌??");
        }
        if (subscriptionService.isSubscribed(subscriberId, streamerId)) {
            throw new IllegalArgumentException("???? ?????節뉗땡??μ떝?띄몭??袁㏉떋???????낆젵.");
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

        String storeId = paymentProperties.getClientInitKey();
        String pg = paymentProperties.getPg() != null ? paymentProperties.getPg() : "html5_inicis.INIpayTest";
        String payMethod = paymentProperties.getPayMethod() != null ? paymentProperties.getPayMethod() : "card";

        return new CreateOrderResult(orderId, amountWon, "GameMatcher 스트리머 구독 결제", storeId, pg, payMethod);
    }

    @Transactional
    public ConfirmResult confirmPangPayment(Long userId, String orderId, String impUid) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("????용츧?嶺뚮?援ο쭩???꿔꺂?????????轅붽틓?????????????욱룏???????낆젵."));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("???ㅼ뒧?戮レ땡??????용츧?嶺뚮?援ο쭩?좎녇???꿔꺂??틝???????????????????낆젵.");
        }
        if (order.getKind() != PaymentOrderKind.PANG_CHARGE) {
            throw new IllegalArgumentException("????濡ろ뜐筌?쓣???????용츧?嶺뚮?援ο쭩???????밸븶?癲??????낆젵.");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            long balance = pangService.getBalance(userId);
            return new ConfirmResult(true, balance, "???? ?轅붽틓??影?뽧걤?????β뼯援?????????뽯쨦??");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("?轅붽틓??影?뽧걤?????????筌뤾쑵??????용츧?嶺뚮?援ο쭩?????釉먮빱???????뽯쨦??");
        }
        if (paymentProperties.getApiKey() == null || paymentProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("??β뼯援???????濚밸Ŧ?????????룸??????????????낆젵.");
        }

        verifyAndMarkPayment(order, orderId, impUid);
        long newBalance = pangService.charge(userId, order.getPangAmount(), order.getOrderId(), impUid);
        notificationService.createForPaymentCompleted(userId, order.getPangAmount(), order.getAmountWon());
        return new ConfirmResult(true, newBalance, "?????꼧 ?野껊챶爾???筌???????");
    }

    @Transactional
    public ConfirmResult confirmSubscriptionPayment(Long subscriberId, String orderId, String impUid) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("????용츧?嶺뚮?援ο쭩???꿔꺂?????????轅붽틓?????????????욱룏???????낆젵."));

        if (!order.getUserId().equals(subscriberId)) {
            throw new IllegalArgumentException("???ㅼ뒧?戮レ땡??????용츧?嶺뚮?援ο쭩?좎녇???꿔꺂??틝???????????????????낆젵.");
        }
        if (order.getKind() != PaymentOrderKind.SUBSCRIPTION) {
            throw new IllegalArgumentException("?????節뉗땡?????용츧?嶺뚮?援ο쭩???????밸븶?癲??????낆젵.");
        }
        if (order.getTargetUserId() == null) {
            throw new IllegalArgumentException("?????節뉗땡???????꿔꺂??????쒐춯誘↔데鸚????쎛 ?????욱룏???????낆젵.");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            return new ConfirmResult(true, 0L, "???? ?轅붽틓??影?뽧걤?????β뼯援?????????뽯쨦??");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("?轅붽틓??影?뽧걤?????????筌뤾쑵??????용츧?嶺뚮?援ο쭩?????釉먮빱???????뽯쨦??");
        }

        verifyAndMarkPayment(order, orderId, impUid);
        subscriptionService.grantSubscription(subscriberId, order.getTargetUserId(), true);
        return new ConfirmResult(true, 0L, "?????節뉗땡???????밸븶???癲???????");
    }

    @Transactional
    public ConfirmResult refundPangPayment(Long userId, String orderId, String impUid, String reason) {
        PaymentOrder order = findOrderForRefund(orderId, impUid);
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("???ㅼ뒧?戮レ땡??????용츧?嶺뚮?援ο쭩?좎녇????棺??????????????????낆젵.");
        }
        if (order.getKind() != PaymentOrderKind.PANG_CHARGE) {
            throw new IllegalArgumentException("????濡ろ뜐筌?쓣???????용츧?嶺뚮?援ο쭩?좎녇????棺??????????????????낆젵.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            long balance = pangService.getBalance(userId);
            return new ConfirmResult(true, balance, "???? ???棺?????????용츧?嶺뚮?援ο쭩??????뽯쨦??");
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("?????밸븶?????β뼯援???鶯ㅺ동???볥궚?????棺??????????????????낆젵.");
        }

        String targetImpUid = impUid != null && !impUid.isBlank() ? impUid : order.getImpUid();
        if (targetImpUid == null || targetImpUid.isBlank()) {
            throw new IllegalArgumentException("???棺????????impUid???轅붽틓?????????????욱룏???????낆젵.");
        }

        cancelPaymentAtPortOne(targetImpUid, order.getOrderId(), reason);
        long balance = pangService.revokeCharge(order.getUserId(), order.getPangAmount(), order.getOrderId(), targetImpUid);
        order.setStatus("CANCELLED");
        paymentOrderRepository.save(order);
        notificationService.createForPaymentRefunded(order.getUserId(), order.getPangAmount(), order.getAmountWon());
        return new ConfirmResult(true, balance, "???棺??????????밸븶???癲???????");
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
            throw new IllegalArgumentException("??β뼯援??????癲ル슢?뤷쳞???imp_uid)???ル봿?? ?????룸??????????????낆젵.");
        }
        if (!impUid.startsWith("imp_")) {
            throw new IllegalArgumentException("??β뼯援??????癲ル슢?뤷쳞?????꿔꺂??틝???놁뗄??????癲?? ???????????낆젵. imp_uid(imp_...) ???ル봿?????????밸븶????????용츧????ロ뒌??");
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
            throw new IllegalArgumentException("??β뼯援??????꿔꺂??????쒐춯誘↔데鸚????쎛 ????용츧?嶺뚮?援ο쭩????嚥싲갭큔?딆뼍留??? ???????????낆젵.");
        }

        if (!"paid".equalsIgnoreCase(status)) {
            order.setStatus("FAILED");
            paymentOrderRepository.save(order);
            throw new IllegalArgumentException("??β뼯援???鶯ㅺ동???怨?? ?????밸븶???? ?????源낅돹?????? (status=" + status + ")");
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

        ResponseEntity<?> resp;
        try {
            resp = restTemplate.postForEntity(IAMPORT_GET_TOKEN, new HttpEntity<>(body, headers), Map.class);
        } catch (HttpStatusCodeException e) {
            log.warn("PortOne token request failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("?????????節떷????ш끽維뽳쭩?좊쐪???????怨뚯댅 (HTTP " + e.getStatusCode().value() + ")");
        } catch (Exception e) {
            log.warn("PortOne token request failed", e);
            throw new IllegalStateException("?????????節떷????ш끽維뽳쭩?좊쐪???????怨뚯댅");
        }

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("?????????節떷????ш끽維뽳쭩?좊쐪???????怨뚯댅 (HTTP " + resp.getStatusCode().value() + ")");
        }

        Object bodyObj = resp.getBody();
        if (!(bodyObj instanceof Map<?, ?> bodyMap)) {
            throw new IllegalStateException("?????????節떷????ш끽維뽳쭩?좊쐪???????????꿔꺂??틝???놁뗄??????癲?? ???????????낆젵.");
        }

        Number code = bodyMap.get("code") instanceof Number n ? n : null;
        String message = bodyMap.get("message") != null ? bodyMap.get("message").toString() : null;
        if (code != null && code.intValue() != 0) {
            throw new IllegalStateException("?????????節떷????ш끽維뽳쭩?좊쐪???????怨뚯댅: " + (message != null ? message : "unknown") + " (code=" + code.intValue() + ")");
        }

        Object response = bodyMap.get("response");
        if (response instanceof Map<?, ?> responseMap) {
            Object accessToken = responseMap.get("access_token");
            if (accessToken != null) return accessToken.toString();
        }

        throw new IllegalStateException("?????????節떷??access_token)?????ル봿????꿔꺂????紐꾩뮏?β뼯猷????깆궔? ?轅붽틓??彛?臾믪뮏?鶯??????");
    }

    private PaymentOrder findOrderForRefund(String orderId, String impUid) {
        if (orderId != null && !orderId.isBlank()) {
            return paymentOrderRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("????용츧?嶺뚮?援ο쭩???꿔꺂?????????轅붽틓?????????????욱룏???????낆젵."));
        }
        if (impUid != null && !impUid.isBlank()) {
            return paymentOrderRepository.findByImpUid(impUid)
                    .orElseThrow(() -> new IllegalArgumentException("????용츧?嶺뚮?援ο쭩???꿔꺂?????????轅붽틓?????????????욱룏???????낆젵."));
        }
        throw new IllegalArgumentException("orderId ?????impUid???ル봿?? ?????밸븶???癲ル슢?????");
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
        body.put("reason", (reason != null && !reason.isBlank()) ? reason : "?ъ슜???붿껌 ?섎텋");

        try {
            ResponseEntity<?> resp = restTemplate.postForEntity(
                    IAMPORT_CANCEL_PAYMENT,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("????????棺?????????怨뚯댅 (HTTP " + resp.getStatusCode().value() + ")");
            }

            Object bodyObj = resp.getBody();
            if (!(bodyObj instanceof Map<?, ?> bodyMap)) {
                throw new IllegalStateException("????????棺?????????????꿔꺂??틝???놁뗄??????癲?? ???????????낆젵.");
            }

            Number code = bodyMap.get("code") instanceof Number n ? n : null;
            String message = bodyMap.get("message") != null ? bodyMap.get("message").toString() : null;
            if (code != null && code.intValue() != 0) {
                throw new IllegalStateException("????????棺?????????怨뚯댅: " + (message != null ? message : "unknown") + " (code=" + code.intValue() + ")");
            }
        } catch (HttpStatusCodeException e) {
            log.warn("PortOne cancel failed: imp_uid={}, status={}, body={}", impUid, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("????????棺?????????怨뚯댅 (HTTP " + e.getStatusCode().value() + ")");
        } catch (Exception e) {
            if (e instanceof IllegalStateException ise) {
                throw ise;
            }
            throw new IllegalStateException("????????棺???????????⑤챷逾???ル봿?? ??ш끽維뽳쭩?좊쐪筌먲퐢?????????????낆젵.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPortOnePayment(String impUid, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        String paymentLookupUrl = IAMPORT_GET_PAYMENT + impUid + (isSandboxPg() ? "?include_sandbox=true" : "");

        try {
            ResponseEntity<?> resp = restTemplate.exchange(
                    paymentLookupUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalArgumentException("???????β뼯援???????곗뒭?????????怨뚯댅 (HTTP " + resp.getStatusCode().value() + ")");
            }

            Object bodyObj = resp.getBody();
            if (!(bodyObj instanceof Map<?, ?> bodyMap)) {
                throw new IllegalArgumentException("???????β뼯援???????곗뒭?????????????꿔꺂??틝???놁뗄??????癲?? ???????????낆젵.");
            }

            Number code = bodyMap.get("code") instanceof Number n ? n : null;
            String message = bodyMap.get("message") != null ? bodyMap.get("message").toString() : null;
            if (code != null && code.intValue() != 0) {
                throw new IllegalArgumentException("???????β뼯援???????곗뒭?????????怨뚯댅: " + (message != null ? message : "unknown") + " (code=" + code.intValue() + ")");
            }

            Object response = bodyMap.get("response");
            if (!(response instanceof Map<?, ?> responseMap)) {
                throw new IllegalArgumentException("???????β뼯援???????곗뒭??????β뼯援?????筌믨퀣?? ?????룸??????????????낆젵.");
            }
            return (Map<String, Object>) responseMap;
        } catch (HttpStatusCodeException e) {
            log.warn("PortOne getPayment failed: imp_uid={}, status={}, body={}", impUid, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("???????β뼯援???????곗뒭?????????怨뚯댅 (HTTP " + e.getStatusCode().value() + ")");
        } catch (Exception e) {
            log.warn("PortOne getPayment failed: imp_uid={}", impUid, e);
            if (e instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw new IllegalArgumentException("???????β뼯援???????곗뒭???????????⑤챷逾???ル봿?? ??ш끽維뽳쭩?좊쐪筌먲퐢?????????????낆젵.");
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

    public record CreateOrderResult(String orderId, long amountWon, String orderName, String storeId, String pg, String payMethod) {}

    public record ConfirmResult(boolean success, long balance, String message) {}
}
