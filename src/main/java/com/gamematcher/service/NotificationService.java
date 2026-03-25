package com.gamematcher.service;

import com.gamematcher.entity.Follow;
import com.gamematcher.entity.Notification;
import com.gamematcher.entity.User;
import com.gamematcher.repository.FollowRepository;
import com.gamematcher.repository.NotificationRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_FOLLOWING_STARTED_STREAM = "FOLLOWING_STARTED_STREAM";
    public static final String TYPE_NEW_DM = "NEW_DM";
    public static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String TYPE_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String TYPE_PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
    public static final String TYPE_ADMIN_STREAM_NOTICE = "ADMIN_STREAM_NOTICE";
    public static final String TYPE_ADMIN_PANG_GIFT = "ADMIN_PANG_GIFT";

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PushTokenService pushTokenService;
    private final FcmPushService fcmPushService;
    private final SmsNotificationService smsNotificationService;

    @Transactional
    public void createForFriendRequest(Long toUserId, Long fromUserId) {
        if (toUserId == null || fromUserId == null || toUserId.equals(fromUserId)) return;

        Notification n = new Notification();
        n.setUserId(toUserId);
        n.setType(TYPE_FRIEND_REQUEST);
        n.setActorUserId(fromUserId);
        notificationRepository.save(n);

        pushToUser(toUserId, "친구 요청", "새 친구 요청이 도착했습니다.", "/profile/friends");
    }

    @Transactional
    public void createForNewDm(Long toUserId, Long fromUserId) {
        if (toUserId == null || fromUserId == null || toUserId.equals(fromUserId)) return;

        Notification n = new Notification();
        n.setUserId(toUserId);
        n.setType(TYPE_NEW_DM);
        n.setActorUserId(fromUserId);
        notificationRepository.save(n);

        pushToUser(toUserId, "새 메시지", "DM이 도착했습니다.", "/messages");
    }

    @Transactional
    public void createForFollowedStreamStart(Long streamId, Long streamerUserId) {
        if (streamId == null || streamerUserId == null) return;

        String streamerName = userRepository.findById(streamerUserId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("스트리머");

        List<Follow> follows = followRepository.findByFollowingId(streamerUserId);
        for (Follow f : follows) {
            Long followerId = f.getFollowerId();
            if (followerId == null || followerId.equals(streamerUserId)) continue;

            Notification n = new Notification();
            n.setUserId(followerId);
            n.setType(TYPE_FOLLOWING_STARTED_STREAM);
            n.setStreamId(streamId);
            n.setActorUserId(streamerUserId);
            notificationRepository.save(n);

            pushToUser(followerId, "방송 시작", "팔로우한 스트리머가 방송을 시작했습니다.", "/watch/" + streamId);
            sendSmsToUser(followerId, "[GameMatcher] " + streamerName + "님이 방송을 시작했습니다. /watch/" + streamId);
        }
    }

    @Transactional
    public void createForPaymentCompleted(Long userId, int pangAmount, long amountWon) {
        if (userId == null) return;

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(TYPE_PAYMENT_COMPLETED);
        notificationRepository.save(n);

        String body = pangAmount + "팡 충전이 완료되었습니다. (" + amountWon + "원)";
        pushToUser(userId, "결제 완료", body, "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] 결제 완료: " + body);
    }

    @Transactional
    public void createForPaymentRefunded(Long userId, int pangAmount, long amountWon) {
        if (userId == null) return;

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(TYPE_PAYMENT_REFUNDED);
        notificationRepository.save(n);

        String body = pangAmount + "팡 환불이 완료되었습니다. (" + amountWon + "원)";
        pushToUser(userId, "환불 완료", body, "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] 환불 완료: " + body);
    }

    @Transactional
    public void createAdminStreamNotice(Long toUserId, Long adminUserId, String text) {
        if (toUserId == null || adminUserId == null) return;
        String safe = text != null ? text.trim() : "";
        if (safe.isEmpty()) {
            safe = "운영자 안내가 도착했습니다.";
        }

        Notification n = new Notification();
        n.setUserId(toUserId);
        n.setType(TYPE_ADMIN_STREAM_NOTICE);
        n.setActorUserId(adminUserId);
        n.setBody(safe);
        notificationRepository.save(n);

        pushToUser(toUserId, "운영 안내", safe, "/notifications");
    }

    @Transactional
    public void createForAdminPangGift(Long toUserId, Long adminUserId, int pangAmount, String customMessage) {
        if (toUserId == null || adminUserId == null || pangAmount <= 0) return;

        String extra = customMessage != null ? customMessage.trim() : "";
        String body = "운영자가 " + pangAmount + "팡을 지급했습니다." + (extra.isEmpty() ? "" : " " + extra);

        Notification n = new Notification();
        n.setUserId(toUserId);
        n.setType(TYPE_ADMIN_PANG_GIFT);
        n.setActorUserId(adminUserId);
        n.setBody(body);
        notificationRepository.save(n);

        pushToUser(toUserId, "팡 지급", body, "/profile/pang");
    }

    public long getUnreadCount(Long userId) {
        if (userId == null) return 0;
        long total = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        long dmCount = notificationRepository.countByUserIdAndTypeAndReadAtIsNull(userId, TYPE_NEW_DM);
        return Math.max(0, total - dmCount);
    }

    public long getUnreadDmCount(Long userId) {
        if (userId == null) return 0;
        return notificationRepository.countByUserIdAndTypeAndReadAtIsNull(userId, TYPE_NEW_DM);
    }

    public List<Map<String, Object>> getList(Long userId, int limit) {
        if (userId == null) return List.of();

        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
        return list.stream()
                .filter(n -> !TYPE_NEW_DM.equals(n.getType()))
                .map(n -> {
                    String actorNickname = null;
                    if (n.getActorUserId() != null) {
                        actorNickname = userRepository.findById(n.getActorUserId())
                                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                                .orElse(null);
                    }

                    String message;
                    if (TYPE_FOLLOWING_STARTED_STREAM.equals(n.getType())) {
                        message = (actorNickname != null ? actorNickname : "스트리머") + "님이 방송을 시작했습니다.";
                    } else if (TYPE_NEW_DM.equals(n.getType())) {
                        message = (actorNickname != null ? actorNickname : "누군가") + "님이 메시지를 보냈습니다.";
                    } else if (TYPE_FRIEND_REQUEST.equals(n.getType())) {
                        message = (actorNickname != null ? actorNickname : "누군가") + "님이 친구 요청을 보냈습니다.";
                    } else if (TYPE_PAYMENT_COMPLETED.equals(n.getType())) {
                        message = "팡 충전 결제가 완료되었습니다.";
                    } else if (TYPE_PAYMENT_REFUNDED.equals(n.getType())) {
                        message = "팡 환불이 완료되었습니다.";
                    } else if (TYPE_ADMIN_STREAM_NOTICE.equals(n.getType())) {
                        message = (n.getBody() != null && !n.getBody().isBlank()) ? n.getBody() : "운영자 안내";
                    } else if (TYPE_ADMIN_PANG_GIFT.equals(n.getType())) {
                        message = (n.getBody() != null && !n.getBody().isBlank()) ? n.getBody() : "운영자 팡 지급";
                    } else {
                        message = "알림";
                    }

                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", n.getId());
                    map.put("type", n.getType() != null ? n.getType() : "");
                    map.put("streamId", n.getStreamId() != null ? n.getStreamId() : 0L);
                    map.put("actorUserId", n.getActorUserId() != null ? n.getActorUserId() : 0L);
                    map.put("actorNickname", actorNickname != null ? actorNickname : "");
                    map.put("message", message);
                    map.put("read", n.getReadAt() != null);
                    map.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) return;

        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (userId.equals(n.getUserId())) {
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        if (userId == null) return;

        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500));
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : list) {
            if (n.getReadAt() == null) {
                n.setReadAt(now);
                notificationRepository.save(n);
            }
        }
    }

    @Transactional
    public int markNewDmAsReadByActor(Long userId, Long fromUserId) {
        if (userId == null || fromUserId == null) return 0;

        List<Notification> list = notificationRepository.findByUserIdAndTypeAndActorUserIdAndReadAtIsNull(userId, TYPE_NEW_DM, fromUserId);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : list) {
            n.setReadAt(now);
            notificationRepository.save(n);
        }
        return list.size();
    }

    public long getUnreadDmCountByActor(Long userId, Long actorUserId) {
        if (userId == null || actorUserId == null) return 0;
        return notificationRepository.countByUserIdAndTypeAndActorUserIdAndReadAtIsNull(userId, TYPE_NEW_DM, actorUserId);
    }

    @Transactional
    public void deleteAll(Long userId) {
        if (userId == null) return;
        notificationRepository.deleteByUserId(userId);
    }

    private void pushToUser(Long userId, String title, String body, String url) {
        if (!fcmPushService.isEnabled() || userId == null) return;

        List<String> tokens = pushTokenService.findTokensByUserId(userId);
        if (tokens.isEmpty()) return;

        Map<String, String> data = new HashMap<>();
        data.put("url", url != null ? url : "/");
        data.put("title", title != null ? title : "알림");
        data.put("body", body != null ? body : "");

        for (String token : tokens) {
            fcmPushService.sendToToken(token, title, body, data);
        }
    }

    private void sendPaymentSms(Long userId, String message) {
        if (userId == null || !smsNotificationService.isEnabled()) return;
        String phone = userRepository.findById(userId).map(User::getPhone).orElse(null);
        smsNotificationService.sendSms(phone, message);
    }

    private void sendSmsToUser(Long userId, String message) {
        if (userId == null || !smsNotificationService.isEnabled()) return;
        String phone = userRepository.findById(userId).map(User::getPhone).orElse(null);
        smsNotificationService.sendSms(phone, message);
    }
}
