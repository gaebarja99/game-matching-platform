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
import java.util.LinkedHashMap;
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
    public static final String TYPE_ADMIN_PANG_GIFT = "ADMIN_PANG_GIFT";
    public static final String TYPE_ADMIN_STREAM_NOTICE = "ADMIN_STREAM_NOTICE";

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PushTokenService pushTokenService;
    private final FcmPushService fcmPushService;
    private final SmsNotificationService smsNotificationService;

    @Transactional
    public void createForFriendRequest(Long toUserId, Long fromUserId) {
        if (toUserId == null || fromUserId == null || toUserId.equals(fromUserId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setType(TYPE_FRIEND_REQUEST);
        notification.setActorUserId(fromUserId);
        notificationRepository.save(notification);

        pushToUser(toUserId, "친구 요청", "새 친구 요청이 도착했습니다.", "/profile/friends");
    }

    @Transactional
    public void createForNewDm(Long toUserId, Long fromUserId) {
        if (toUserId == null || fromUserId == null || toUserId.equals(fromUserId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setType(TYPE_NEW_DM);
        notification.setActorUserId(fromUserId);
        notificationRepository.save(notification);

        pushToUser(toUserId, "새 메시지", "DM이 도착했습니다.", "/messages");
    }

    @Transactional
    public void createForFollowedStreamStart(Long streamId, Long streamerUserId) {
        if (streamId == null || streamerUserId == null) {
            return;
        }

        String streamerName = userRepository.findById(streamerUserId)
                .map(this::displayName)
                .orElse("스트리머");

        List<Follow> follows = followRepository.findByFollowingId(streamerUserId);
        for (Follow follow : follows) {
            Long followerId = follow.getFollowerId();
            if (followerId == null || followerId.equals(streamerUserId)) {
                continue;
            }

            Notification notification = new Notification();
            notification.setUserId(followerId);
            notification.setType(TYPE_FOLLOWING_STARTED_STREAM);
            notification.setStreamId(streamId);
            notification.setActorUserId(streamerUserId);
            notificationRepository.save(notification);

            pushToUser(followerId, "방송 시작", "팔로우한 스트리머가 방송을 시작했습니다.", "/watch/" + streamId);
            sendSmsToUser(followerId, "[GameMatcher] " + streamerName + "님이 방송을 시작했습니다. /watch/" + streamId);
        }
    }

    @Transactional
    public void createForPaymentCompleted(Long userId, int pangAmount, long amountWon) {
        if (userId == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_PAYMENT_COMPLETED);
        notificationRepository.save(notification);

        String body = pangAmount + "팡 충전이 완료되었습니다. (" + amountWon + "원)";
        pushToUser(userId, "결제 완료", body, "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] 결제 완료: " + body);
    }

    @Transactional
    public void createForPaymentRefunded(Long userId, int pangAmount, long amountWon) {
        if (userId == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_PAYMENT_REFUNDED);
        notificationRepository.save(notification);

        String body = pangAmount + "팡 환불이 완료되었습니다. (" + amountWon + "원)";
        pushToUser(userId, "환불 완료", body, "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] 환불 완료: " + body);
    }

    @Transactional
    public void createForAdminPangGift(Long userId, Long adminUserId, int pangAmount, String customMessage) {
        if (userId == null || pangAmount <= 0) {
            return;
        }

        String trimmedMessage = customMessage != null ? customMessage.trim() : "";

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_ADMIN_PANG_GIFT);
        notification.setActorUserId(adminUserId);
        notification.setMessage(trimmedMessage.isBlank()
                ? "운영자가 이벤트로 " + pangAmount + "팡을 지급했습니다."
                : trimmedMessage);
        notificationRepository.save(notification);

        pushToUser(userId, "이벤트 팡 지급", notification.getMessage(), "/profile/pang");
    }

    @Transactional
    public void createAdminStreamNotice(Long userId, Long adminUserId, String message) {
        if (userId == null || message == null || message.isBlank()) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_ADMIN_STREAM_NOTICE);
        notification.setActorUserId(adminUserId);
        notification.setMessage(message.trim());
        notificationRepository.save(notification);

        pushToUser(userId, "방송 관리 알림", notification.getMessage(), "/studio");
    }

    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        long total = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        long dmCount = notificationRepository.countByUserIdAndTypeAndReadAtIsNull(userId, TYPE_NEW_DM);
        return Math.max(0, total - dmCount);
    }

    public long getUnreadDmCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationRepository.countByUserIdAndTypeAndReadAtIsNull(userId, TYPE_NEW_DM);
    }

    public List<Map<String, Object>> getList(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }

        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
        return list.stream()
                .filter(notification -> !TYPE_NEW_DM.equals(notification.getType()))
                .map(notification -> {
                    String actorNickname = null;
                    if (notification.getActorUserId() != null) {
                        actorNickname = userRepository.findById(notification.getActorUserId())
                                .map(this::displayName)
                                .orElse(null);
                    }

                    String message = buildMessage(notification, actorNickname);

                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", notification.getId());
                    map.put("type", notification.getType() != null ? notification.getType() : "");
                    map.put("streamId", notification.getStreamId() != null ? notification.getStreamId() : 0L);
                    map.put("actorUserId", notification.getActorUserId() != null ? notification.getActorUserId() : 0L);
                    map.put("actorNickname", actorNickname != null ? actorNickname : "");
                    map.put("message", message);
                    map.put("read", notification.getReadAt() != null);
                    map.put("createdAt", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) {
            return;
        }

        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (userId.equals(notification.getUserId())) {
                notification.setReadAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        if (userId == null) {
            return;
        }

        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500));
        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : list) {
            if (notification.getReadAt() == null) {
                notification.setReadAt(now);
                notificationRepository.save(notification);
            }
        }
    }

    @Transactional
    public int markNewDmAsReadByActor(Long userId, Long fromUserId) {
        if (userId == null || fromUserId == null) {
            return 0;
        }

        List<Notification> list = notificationRepository.findByUserIdAndTypeAndActorUserIdAndReadAtIsNull(userId, TYPE_NEW_DM, fromUserId);
        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : list) {
            notification.setReadAt(now);
            notificationRepository.save(notification);
        }
        return list.size();
    }

    public long getUnreadDmCountByActor(Long userId, Long actorUserId) {
        if (userId == null || actorUserId == null) {
            return 0;
        }
        return notificationRepository.countByUserIdAndTypeAndActorUserIdAndReadAtIsNull(userId, TYPE_NEW_DM, actorUserId);
    }

    @Transactional
    public void deleteAll(Long userId) {
        if (userId == null) {
            return;
        }
        notificationRepository.deleteByUserId(userId);
    }

    private String buildMessage(Notification notification, String actorNickname) {
        if (notification.getMessage() != null && !notification.getMessage().isBlank()) {
            return notification.getMessage();
        }
        if (TYPE_FOLLOWING_STARTED_STREAM.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "스트리머") + "님이 방송을 시작했습니다.";
        }
        if (TYPE_NEW_DM.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "누군가") + "님이 메시지를 보냈습니다.";
        }
        if (TYPE_FRIEND_REQUEST.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "누군가") + "님이 친구 요청을 보냈습니다.";
        }
        if (TYPE_PAYMENT_COMPLETED.equals(notification.getType())) {
            return "팡 충전 결제가 완료되었습니다.";
        }
        if (TYPE_PAYMENT_REFUNDED.equals(notification.getType())) {
            return "팡 환불이 완료되었습니다.";
        }
        if (TYPE_ADMIN_PANG_GIFT.equals(notification.getType())) {
            return "운영자가 이벤트 팡을 지급했습니다.";
        }
        if (TYPE_ADMIN_STREAM_NOTICE.equals(notification.getType())) {
            return "운영자 방송 관리 알림이 도착했습니다.";
        }
        return "알림";
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private void pushToUser(Long userId, String title, String body, String url) {
        if (!fcmPushService.isEnabled() || userId == null) {
            return;
        }

        List<String> tokens = pushTokenService.findTokensByUserId(userId);
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("url", url != null ? url : "/");
        data.put("title", title != null ? title : "알림");
        data.put("body", body != null ? body : "");

        for (String token : tokens) {
            fcmPushService.sendToToken(token, title, body, data);
        }
    }

    private void sendPaymentSms(Long userId, String message) {
        if (userId == null || !smsNotificationService.isEnabled()) {
            return;
        }
        String phone = userRepository.findById(userId).map(User::getPhone).orElse(null);
        smsNotificationService.sendSms(phone, message);
    }

    private void sendSmsToUser(Long userId, String message) {
        if (userId == null || !smsNotificationService.isEnabled()) {
            return;
        }
        String phone = userRepository.findById(userId).map(User::getPhone).orElse(null);
        smsNotificationService.sendSms(phone, message);
    }
}
