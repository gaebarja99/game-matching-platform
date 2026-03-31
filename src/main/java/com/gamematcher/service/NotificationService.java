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
    public static final String TYPE_ADMIN_MILEAGE_GIFT = "ADMIN_MILEAGE_GIFT";
    public static final String TYPE_ADMIN_STREAM_NOTICE = "ADMIN_STREAM_NOTICE";
    public static final String TYPE_NEW_FOLLOWER = "NEW_FOLLOWER";
    public static final String TYPE_NEW_SUBSCRIBER = "NEW_SUBSCRIBER";
    public static final String TYPE_CHANNEL_PERMISSION_GRANTED = "CHANNEL_PERMISSION_GRANTED";
    public static final String TYPE_CHANNEL_PERMISSION_REVOKED = "CHANNEL_PERMISSION_REVOKED";
    public static final String TYPE_GROUP_CHAT_INVITE = "GROUP_CHAT_INVITE";
    public static final String TYPE_GROUP_CHAT_MENTION = "GROUP_CHAT_MENTION";
    public static final String TYPE_MATCH_CHAT_MENTION = "MATCH_CHAT_MENTION";

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

        pushToUser(
                toUserId,
                "\uCE5C\uAD6C \uC694\uCCAD",
                "\uC0C8 \uCE5C\uAD6C \uC694\uCCAD\uC774 \uB3C4\uCC29\uD588\uC2B5\uB2C8\uB2E4.",
                "/profile"
        );
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

        String actorName = userRepository.findById(fromUserId).map(this::displayName).orElse("\uC0AC\uC6A9\uC790");
        pushToUser(
                toUserId,
                "\uC0C8 \uB2E4\uC774\uB809\uD2B8 \uBA54\uC2DC\uC9C0",
                actorName + "\uB2D8\uC774 \uB2E4\uC774\uB809\uD2B8 \uBA54\uC2DC\uC9C0\uB97C \uBCF4\uB0C8\uC2B5\uB2C8\uB2E4.",
                "/dm?userId=" + fromUserId
        );
    }

    @Transactional
    public void createForFollowedStreamStart(Long streamId, Long streamerUserId) {
        if (streamId == null || streamerUserId == null) {
            return;
        }

        String streamerName = userRepository.findById(streamerUserId)
                .map(this::displayName)
                .orElse("\uC2A4\uD2B8\uB9AC\uBA38");

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

            String message = streamerName + "\uB2D8\uC774 \uBC29\uC1A1\uC744 \uC2DC\uC791\uD588\uC2B5\uB2C8\uB2E4.";
            pushToUser(followerId, "\uD314\uB85C\uC6B0 \uBC29\uC1A1 \uC2DC\uC791", message, "/watch/" + streamId);
            sendSmsToUser(followerId, "[GameMatcher] " + message + " /watch/" + streamId);
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
        notification.setMessage(pangAmount + "\uD31D \uACB0\uC81C\uAC00 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. (" + amountWon + "\uC6D0)");
        notificationRepository.save(notification);

        pushToUser(userId, "\uD31D \uACB0\uC81C \uC644\uB8CC", notification.getMessage(), "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] " + notification.getMessage());
    }

    @Transactional
    public void createForPaymentRefunded(Long userId, int pangAmount, long amountWon) {
        if (userId == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_PAYMENT_REFUNDED);
        notification.setMessage(pangAmount + "\uD31D \uD658\uBD88\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. (" + amountWon + "\uC6D0)");
        notificationRepository.save(notification);

        pushToUser(userId, "\uD31D \uD658\uBD88 \uC644\uB8CC", notification.getMessage(), "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] " + notification.getMessage());
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
                ? "\uC774\uBCA4\uD2B8\uB85C " + pangAmount + "\uD31D\uC744 \uC9C0\uAE09\uD588\uC2B5\uB2C8\uB2E4."
                : trimmedMessage);
        notificationRepository.save(notification);

        pushToUser(userId, "\uC774\uBCA4\uD2B8 \uD31D \uC9C0\uAE09", notification.getMessage(), "/profile/pang");
    }

    @Transactional
    public void createForAdminMileageGift(Long userId, Long adminUserId, long mileageAmount, String customMessage) {
        if (userId == null || mileageAmount <= 0) {
            return;
        }

        String trimmedMessage = customMessage != null ? customMessage.trim() : "";

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_ADMIN_MILEAGE_GIFT);
        notification.setActorUserId(adminUserId);
        notification.setMessage(trimmedMessage.isBlank()
                ? "\uC774\uBCA4\uD2B8\uB85C " + mileageAmount + "\uB9C8\uC77C\uB9AC\uC9C0\uB97C \uC9C0\uAE09\uD588\uC2B5\uB2C8\uB2E4."
                : trimmedMessage);
        notificationRepository.save(notification);

        pushToUser(userId, "\uC774\uBCA4\uD2B8 \uB9C8\uC77C\uB9AC\uC9C0 \uC9C0\uAE09", notification.getMessage(), "/profile/mileage-shop");
    }

    @Transactional
    public void backfillAdminMileageGiftNotification(
            Long userId,
            Long adminUserId,
            long mileageAmount,
            String customMessage,
            LocalDateTime createdAt
    ) {
        if (userId == null || mileageAmount <= 0) {
            return;
        }

        LocalDateTime safeCreatedAt = createdAt != null ? createdAt : LocalDateTime.now();
        if (notificationRepository.existsByUserIdAndTypeAndCreatedAt(userId, TYPE_ADMIN_MILEAGE_GIFT, safeCreatedAt)) {
            return;
        }

        String trimmedMessage = customMessage != null ? customMessage.trim() : "";

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_ADMIN_MILEAGE_GIFT);
        notification.setActorUserId(adminUserId);
        notification.setMessage(trimmedMessage.isBlank()
                ? "\uC774\uBCA4\uD2B8\uB85C " + mileageAmount + "\uB9C8\uC77C\uB9AC\uC9C0\uB97C \uC9C0\uAE09\uD588\uC2B5\uB2C8\uB2E4."
                : trimmedMessage);
        notification.setCreatedAt(safeCreatedAt);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createForNewFollower(Long streamerUserId, Long followerUserId) {
        if (streamerUserId == null || followerUserId == null || streamerUserId.equals(followerUserId)) {
            return;
        }

        String followerName = userRepository.findById(followerUserId)
                .map(this::displayName)
                .orElse("\uC2DC\uCCAD\uC790");

        Notification notification = new Notification();
        notification.setUserId(streamerUserId);
        notification.setType(TYPE_NEW_FOLLOWER);
        notification.setActorUserId(followerUserId);
        notification.setMessage(followerName + "\uB2D8\uC774 \uCC44\uB110\uC744 \uD314\uB85C\uC6B0\uD588\uC2B5\uB2C8\uB2E4.");
        notificationRepository.save(notification);

        pushToUser(streamerUserId, "\uC0C8 \uD314\uB85C\uC6B0", notification.getMessage(), "/studio/viewers/followers");
    }

    @Transactional
    public void createForNewSubscriber(Long streamerUserId, Long subscriberUserId) {
        if (streamerUserId == null || subscriberUserId == null || streamerUserId.equals(subscriberUserId)) {
            return;
        }

        String subscriberName = userRepository.findById(subscriberUserId)
                .map(this::displayName)
                .orElse("\uC2DC\uCCAD\uC790");

        Notification notification = new Notification();
        notification.setUserId(streamerUserId);
        notification.setType(TYPE_NEW_SUBSCRIBER);
        notification.setActorUserId(subscriberUserId);
        notification.setMessage(subscriberName + "\uB2D8\uC774 \uCC44\uB110\uC744 \uAD6C\uB3C5\uD588\uC2B5\uB2C8\uB2E4.");
        notificationRepository.save(notification);

        pushToUser(streamerUserId, "\uC0C8 \uAD6C\uB3C5\uC790", notification.getMessage(), "/studio/viewers/subscribers");
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

        pushToUser(userId, "\uBC29\uC1A1 \uAD00\uB828 \uC548\uB0B4", notification.getMessage(), "/studio");
    }

    @Transactional
    public void createForChannelPermissionGranted(Long userId, Long ownerUserId, String ownerName) {
        if (userId == null || ownerUserId == null) {
            return;
        }

        String safeOwnerName = ownerName == null || ownerName.isBlank() ? "\uCC44\uB110 \uC6B4\uC601\uC790" : ownerName;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_CHANNEL_PERMISSION_GRANTED);
        notification.setActorUserId(ownerUserId);
        notification.setMessage(safeOwnerName + "\uB2D8\uC774 \uCC44\uB110 \uAD00\uB9AC \uAD8C\uD55C\uC744 \uBD80\uC5EC\uD588\uC2B5\uB2C8\uB2E4.");
        notificationRepository.save(notification);

        pushToUser(userId, "\uCC44\uB110 \uAD00\uB9AC \uAD8C\uD55C \uBD80\uC5EC", notification.getMessage(), "/studio/channel/manage");
    }

    @Transactional
    public void createForChannelPermissionRevoked(Long userId, Long ownerUserId, String ownerName) {
        if (userId == null || ownerUserId == null) {
            return;
        }

        String safeOwnerName = ownerName == null || ownerName.isBlank() ? "\uCC44\uB110 \uC6B4\uC601\uC790" : ownerName;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_CHANNEL_PERMISSION_REVOKED);
        notification.setActorUserId(ownerUserId);
        notification.setMessage(safeOwnerName + "\uB2D8\uC774 \uCC44\uB110 \uAD00\uB9AC \uAD8C\uD55C\uC744 \uD68C\uC218\uD588\uC2B5\uB2C8\uB2E4.");
        notificationRepository.save(notification);

        pushToUser(userId, "\uCC44\uB110 \uAD00\uB9AC \uAD8C\uD55C \uD68C\uC218", notification.getMessage(), "/studio");
    }

    @Transactional
    public void createForGroupChatInvite(Long userId, Long fromUserId, Long roomId, String roomName) {
        if (userId == null || fromUserId == null || roomId == null || userId.equals(fromUserId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_GROUP_CHAT_INVITE);
        notification.setActorUserId(fromUserId);
        notification.setStreamId(roomId);
        notification.setMessage((roomName == null || roomName.isBlank() ? "\uCC44\uD305\uBC29" : roomName)
                + " \uCD08\uB300\uAC00 \uB3C4\uCC29\uD588\uC2B5\uB2C8\uB2E4.");
        notificationRepository.save(notification);

        pushToUser(userId, "\uADF8\uB8F9 \uCC44\uD305 \uCD08\uB300", notification.getMessage(), "/group-chat/room/" + roomId);
    }

    @Transactional
    public void createForGroupChatMention(Long userId, Long fromUserId, Long roomId, String roomName, String preview) {
        if (userId == null || fromUserId == null || roomId == null || userId.equals(fromUserId)) {
            return;
        }

        String actorName = userRepository.findById(fromUserId).map(this::displayName).orElse("\uC0AC\uC6A9\uC790");
        String roomLabel = roomName == null || roomName.isBlank() ? "\uCC44\uD305\uBC29" : roomName;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_GROUP_CHAT_MENTION);
        notification.setActorUserId(fromUserId);
        notification.setStreamId(roomId);
        notification.setMessage(actorName + "\uB2D8\uC774 " + roomLabel + "\uC5D0\uC11C \uB2F9\uC2E0\uC744 \uBA58\uC158\uD588\uC2B5\uB2C8\uB2E4.");
        notificationRepository.save(notification);

        pushToUser(userId, "\uADF8\uB8F9 \uCC44\uD305 \uBA58\uC158", previewMessage(notification.getMessage(), preview), "/group-chat/room/" + roomId);
    }

    @Transactional
    public void createForMatchChatMention(Long userId, Long fromUserId, Long sessionId, String gameName, String preview) {
        if (userId == null || fromUserId == null || sessionId == null || userId.equals(fromUserId)) {
            return;
        }

        String actorName = userRepository.findById(fromUserId).map(this::displayName).orElse("\uC0AC\uC6A9\uC790");
        String roomLabel = gameName == null || gameName.isBlank()
                ? "\uB9E4\uCE58 \uCC44\uD305\uBC29"
                : gameName + " \uB9E4\uCE58 \uCC44\uD305\uBC29";

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_MATCH_CHAT_MENTION);
        notification.setActorUserId(fromUserId);
        notification.setStreamId(sessionId);
        notification.setMessage(actorName + "\uB2D8\uC774 " + roomLabel + "\uC5D0\uC11C \uB2F9\uC2E0\uC744 \uBA58\uC158\uD588\uC2B5\uB2C8\uB2E4.");
        notificationRepository.save(notification);

        pushToUser(userId, "\uB9E4\uCE58 \uCC44\uD305 \uBA58\uC158", previewMessage(notification.getMessage(), preview), "/match-chat/" + sessionId);
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

                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", notification.getId());
                    map.put("type", notification.getType() != null ? notification.getType() : "");
                    map.put("streamId", notification.getStreamId() != null ? notification.getStreamId() : 0L);
                    map.put("actorUserId", notification.getActorUserId() != null ? notification.getActorUserId() : 0L);
                    map.put("actorNickname", actorNickname != null ? actorNickname : "");
                    map.put("message", buildMessage(notification, actorNickname));
                    map.put("targetPath", resolveTargetPath(notification));
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
        List<Notification> list = notificationRepository.findByUserIdAndTypeAndActorUserIdAndReadAtIsNull(
                userId,
                TYPE_NEW_DM,
                fromUserId
        );
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
            return (actorNickname != null ? actorNickname : "\uC2A4\uD2B8\uB9AC\uBA38")
                    + "\uB2D8\uC774 \uBC29\uC1A1\uC744 \uC2DC\uC791\uD588\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_NEW_DM.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "\uC0AC\uC6A9\uC790")
                    + "\uB2D8\uC774 \uB2E4\uC774\uB809\uD2B8 \uBA54\uC2DC\uC9C0\uB97C \uBCF4\uB0C8\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_FRIEND_REQUEST.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "\uC0AC\uC6A9\uC790")
                    + "\uB2D8\uC774 \uCE5C\uAD6C \uC694\uCCAD\uC744 \uBCF4\uB0C8\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_PAYMENT_COMPLETED.equals(notification.getType())) {
            return "\uD31D \uACB0\uC81C\uAC00 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_PAYMENT_REFUNDED.equals(notification.getType())) {
            return "\uD31D \uD658\uBD88\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_ADMIN_PANG_GIFT.equals(notification.getType())) {
            return "\uC774\uBCA4\uD2B8\uB85C \uD31D\uC744 \uC9C0\uAE09\uD588\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_ADMIN_MILEAGE_GIFT.equals(notification.getType())) {
            return "\uC774\uBCA4\uD2B8\uB85C \uB9C8\uC77C\uB9AC\uC9C0\uB97C \uC9C0\uAE09\uD588\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_ADMIN_STREAM_NOTICE.equals(notification.getType())) {
            return "\uC6B4\uC601\uC790\uAC00 \uBC29\uC1A1 \uAD00\uB828 \uC548\uB0B4\uB97C \uBCF4\uB0C8\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_NEW_FOLLOWER.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "\uC2DC\uCCAD\uC790")
                    + "\uB2D8\uC774 \uCC44\uB110\uC744 \uD314\uB85C\uC6B0\uD588\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_NEW_SUBSCRIBER.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "\uC2DC\uCCAD\uC790")
                    + "\uB2D8\uC774 \uCC44\uB110\uC744 \uAD6C\uB3C5\uD588\uC2B5\uB2C8\uB2E4.";
        }
        if (TYPE_CHANNEL_PERMISSION_GRANTED.equals(notification.getType())
                || TYPE_CHANNEL_PERMISSION_REVOKED.equals(notification.getType())
                || TYPE_GROUP_CHAT_INVITE.equals(notification.getType())
                || TYPE_GROUP_CHAT_MENTION.equals(notification.getType())
                || TYPE_MATCH_CHAT_MENTION.equals(notification.getType())) {
            return notification.getMessage();
        }
        return "\uC54C\uB9BC\uC774 \uB3C4\uCC29\uD588\uC2B5\uB2C8\uB2E4.";
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getLoginId();
    }

    private String resolveTargetPath(Notification notification) {
        if (notification == null) {
            return "/";
        }
        if (TYPE_FOLLOWING_STARTED_STREAM.equals(notification.getType())) {
            return notification.getStreamId() != null ? "/watch/" + notification.getStreamId() : "/streams";
        }
        if (TYPE_PAYMENT_COMPLETED.equals(notification.getType())
                || TYPE_PAYMENT_REFUNDED.equals(notification.getType())
                || TYPE_ADMIN_PANG_GIFT.equals(notification.getType())) {
            return "/profile/pang";
        }
        if (TYPE_ADMIN_MILEAGE_GIFT.equals(notification.getType())) {
            return "/profile/mileage-shop";
        }
        if (TYPE_FRIEND_REQUEST.equals(notification.getType())) {
            return "/profile";
        }
        if (TYPE_ADMIN_STREAM_NOTICE.equals(notification.getType())) {
            return "/studio";
        }
        if (TYPE_NEW_FOLLOWER.equals(notification.getType())) {
            return "/studio/viewers/followers";
        }
        if (TYPE_NEW_SUBSCRIBER.equals(notification.getType())) {
            return "/studio/viewers/subscribers";
        }
        if (TYPE_CHANNEL_PERMISSION_GRANTED.equals(notification.getType())) {
            return "/studio/channel/manage";
        }
        if (TYPE_CHANNEL_PERMISSION_REVOKED.equals(notification.getType())) {
            return "/studio";
        }
        if (TYPE_NEW_DM.equals(notification.getType())) {
            return notification.getActorUserId() != null
                    ? "/dm?userId=" + notification.getActorUserId()
                    : "/dm";
        }
        if (TYPE_GROUP_CHAT_INVITE.equals(notification.getType())
                || TYPE_GROUP_CHAT_MENTION.equals(notification.getType())) {
            return notification.getStreamId() != null
                    ? "/group-chat/room/" + notification.getStreamId()
                    : "/group-chat";
        }
        if (TYPE_MATCH_CHAT_MENTION.equals(notification.getType())) {
            return notification.getStreamId() != null
                    ? "/match-chat/" + notification.getStreamId()
                    : "/match-history";
        }
        return "/";
    }

    private String previewMessage(String fallback, String preview) {
        String trimmedPreview = preview != null ? preview.trim() : "";
        if (trimmedPreview.isBlank()) {
            return fallback;
        }
        return fallback + " " + trimmedPreview;
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
        data.put("title", title != null ? title : "\uC54C\uB9BC");
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
