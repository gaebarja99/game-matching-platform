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

        pushToUser(toUserId, "친구 요청", "새 친구 요청이 도착했습니다.", "/profile");
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

        String actorName = userRepository.findById(fromUserId).map(this::displayName).orElse("유저");
        pushToUser(toUserId, "새 메시지", actorName + "님이 메시지를 보냈습니다.", "/dm?userId=" + fromUserId);
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

            pushToUser(followerId, "방송 시작", streamerName + "님이 방송을 시작했습니다.", "/watch/" + streamId);
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
        notification.setMessage(pangAmount + "팡 충전이 완료되었습니다. (" + amountWon + "원)");
        notificationRepository.save(notification);

        pushToUser(userId, "결제 완료", notification.getMessage(), "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] 결제 완료: " + notification.getMessage());
    }

    @Transactional
    public void createForPaymentRefunded(Long userId, int pangAmount, long amountWon) {
        if (userId == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_PAYMENT_REFUNDED);
        notification.setMessage(pangAmount + "팡 환불이 완료되었습니다. (" + amountWon + "원)");
        notificationRepository.save(notification);

        pushToUser(userId, "환불 완료", notification.getMessage(), "/profile/pang");
        sendPaymentSms(userId, "[GameMatcher] 환불 완료: " + notification.getMessage());
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

    @Transactional
    public void createForChannelPermissionGranted(Long userId, Long ownerUserId, String ownerName) {
        if (userId == null || ownerUserId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_CHANNEL_PERMISSION_GRANTED);
        notification.setActorUserId(ownerUserId);
        notification.setMessage(ownerName + "님의 채널 관리 권한이 부여되었습니다.");
        notificationRepository.save(notification);
        pushToUser(userId, "채널 권한 부여", notification.getMessage(), "/studio/channel/manage");
    }

    @Transactional
    public void createForChannelPermissionRevoked(Long userId, Long ownerUserId, String ownerName) {
        if (userId == null || ownerUserId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_CHANNEL_PERMISSION_REVOKED);
        notification.setActorUserId(ownerUserId);
        notification.setMessage(ownerName + "님의 채널 관리 권한이 해제되었습니다.");
        notificationRepository.save(notification);
        pushToUser(userId, "채널 권한 해제", notification.getMessage(), "/studio");
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
        notification.setMessage((roomName == null || roomName.isBlank() ? "채팅방" : roomName) + " 초대가 도착했습니다.");
        notificationRepository.save(notification);
        pushToUser(userId, "채팅방 초대", notification.getMessage(), "/group-chat/room/" + roomId);
    }

    @Transactional
    public void createForGroupChatMention(Long userId, Long fromUserId, Long roomId, String roomName, String preview) {
        if (userId == null || fromUserId == null || roomId == null || userId.equals(fromUserId)) {
            return;
        }
        String actorName = userRepository.findById(fromUserId).map(this::displayName).orElse("유저");
        String roomLabel = roomName == null || roomName.isBlank() ? "채팅방" : roomName;
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_GROUP_CHAT_MENTION);
        notification.setActorUserId(fromUserId);
        notification.setStreamId(roomId);
        notification.setMessage(actorName + "님이 " + roomLabel + "에서 회원님을 멘션했습니다.");
        notificationRepository.save(notification);
        pushToUser(userId, "채팅방 멘션", previewMessage(notification.getMessage(), preview), "/group-chat/room/" + roomId);
    }

    @Transactional
    public void createForMatchChatMention(Long userId, Long fromUserId, Long sessionId, String gameName, String preview) {
        if (userId == null || fromUserId == null || sessionId == null || userId.equals(fromUserId)) {
            return;
        }
        String actorName = userRepository.findById(fromUserId).map(this::displayName).orElse("유저");
        String roomLabel = gameName == null || gameName.isBlank() ? "매칭 채팅방" : gameName + " 매칭 채팅방";
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_MATCH_CHAT_MENTION);
        notification.setActorUserId(fromUserId);
        notification.setStreamId(sessionId);
        notification.setMessage(actorName + "님이 " + roomLabel + "에서 회원님을 멘션했습니다.");
        notificationRepository.save(notification);
        pushToUser(userId, "매칭 채팅 멘션", previewMessage(notification.getMessage(), preview), "/match-chat/" + sessionId);
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
            return (actorNickname != null ? actorNickname : "유저") + "님이 메시지를 보냈습니다.";
        }
        if (TYPE_FRIEND_REQUEST.equals(notification.getType())) {
            return (actorNickname != null ? actorNickname : "유저") + "님이 친구 요청을 보냈습니다.";
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
        if (TYPE_CHANNEL_PERMISSION_GRANTED.equals(notification.getType())
                || TYPE_CHANNEL_PERMISSION_REVOKED.equals(notification.getType())
                || TYPE_GROUP_CHAT_INVITE.equals(notification.getType())
                || TYPE_GROUP_CHAT_MENTION.equals(notification.getType())
                || TYPE_MATCH_CHAT_MENTION.equals(notification.getType())) {
            return notification.getMessage();
        }
        return "알림";
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
        if (TYPE_FRIEND_REQUEST.equals(notification.getType())) {
            return "/profile";
        }
        if (TYPE_ADMIN_STREAM_NOTICE.equals(notification.getType())) {
            return "/studio";
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
