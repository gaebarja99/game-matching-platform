package com.gamematcher.service;

import com.gamematcher.dto.friend.FriendSearchResult;
import com.gamematcher.entity.Follow;
import com.gamematcher.entity.FriendRequest;
import com.gamematcher.entity.User;
import com.gamematcher.entity.UserBlock;
import com.gamematcher.repository.FollowRepository;
import com.gamematcher.repository.FriendRequestRepository;
import com.gamematcher.repository.UserBlockRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /** 친구 요청 보내기 (수신자에게 알림 생성 + 실시간 푸시). 결과: "sent", "already_friends", "already_pending" (거절된 건 다시 보내기 가능) */
    @Transactional
    public String sendRequest(Long fromUserId, Long toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) return "invalid";
        if (getFriendUserIds(fromUserId).contains(toUserId)) return "already_friends";
        Optional<FriendRequest> existing = friendRequestRepository.findByFromUserIdAndToUserId(fromUserId, toUserId);
        if (existing.isPresent()) {
            FriendRequest fr = existing.get();
            if (fr.getStatus() == FriendRequest.FriendRequestStatus.PENDING) return "already_pending";
            if (fr.getStatus() == FriendRequest.FriendRequestStatus.REJECTED) {
                fr.setStatus(FriendRequest.FriendRequestStatus.PENDING);
                friendRequestRepository.save(fr);
                notificationService.createForFriendRequest(toUserId, fromUserId);
                String actorNickname = userRepository.findById(fromUserId)
                        .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                        .orElse("");
                messagingTemplate.convertAndSend("/topic/user/" + toUserId, Map.<String, Object>of(
                        "type", "FRIEND_REQUEST",
                        "fromUserId", fromUserId,
                        "actorNickname", actorNickname != null ? actorNickname : ""
                ));
                return "sent";
            }
            return "already_pending";
        }
        FriendRequest fr = new FriendRequest();
        fr.setFromUserId(fromUserId);
        fr.setToUserId(toUserId);
        fr.setStatus(FriendRequest.FriendRequestStatus.PENDING);
        friendRequestRepository.save(fr);
        notificationService.createForFriendRequest(toUserId, fromUserId);
        String actorNickname = userRepository.findById(fromUserId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("");
        messagingTemplate.convertAndSend("/topic/user/" + toUserId, Map.<String, Object>of(
                "type", "FRIEND_REQUEST",
                "fromUserId", fromUserId,
                "actorNickname", actorNickname != null ? actorNickname : ""
        ));
        return "sent";
    }

    /** 받은 친구 요청 목록 (PENDING만) */
    public List<Map<String, Object>> getReceivedPending(Long toUserId) {
        if (toUserId == null) return List.of();
        return friendRequestRepository.findByToUserIdAndStatusOrderByCreatedAtDesc(toUserId, FriendRequest.FriendRequestStatus.PENDING)
                .stream()
                .map(fr -> {
                    String nickname = userRepository.findById(fr.getFromUserId())
                            .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                            .orElse("알 수 없음");
                    String loginId = userRepository.findById(fr.getFromUserId())
                            .map(u -> u.getLoginId())
                            .orElse("");
                    String profileImageUrl = userRepository.findById(fr.getFromUserId())
                            .map(u -> u.getProfileImageUrl())
                            .orElse(null);
                    return Map.<String, Object>of(
                            "id", fr.getId(),
                            "fromUserId", fr.getFromUserId(),
                            "nickname", nickname,
                            "loginId", loginId,
                            "profileImageUrl", profileImageUrl != null ? profileImageUrl : "",
                            "createdAt", fr.getCreatedAt() != null ? fr.getCreatedAt().toString() : ""
                    );
                })
                .collect(Collectors.toList());
    }

    /** 수락된 친구의 사용자 ID 목록 (차단 관계 제외) */
    public List<Long> getFriendUserIds(Long userId) {
        if (userId == null) return List.of();
        Set<Long> ids = new LinkedHashSet<>();
        friendRequestRepository.findByToUserIdAndStatusOrderByCreatedAtDesc(userId, FriendRequest.FriendRequestStatus.ACCEPTED)
                .forEach(fr -> ids.add(fr.getFromUserId()));
        friendRequestRepository.findByFromUserIdAndStatus(userId, FriendRequest.FriendRequestStatus.ACCEPTED)
                .forEach(fr -> ids.add(fr.getToUserId()));
        ids.removeIf(friendId -> userBlockRepository.existsByBlockerIdAndBlockedId(userId, friendId)
                || userBlockRepository.existsByBlockerIdAndBlockedId(friendId, userId));
        return new ArrayList<>(ids);
    }

    /** 수락: 양쪽 Follow 생성 후 요청 상태를 ACCEPTED로 */
    @Transactional
    public void accept(Long requestId, Long toUserId) {
        if (requestId == null || toUserId == null) return;
        Optional<FriendRequest> opt = friendRequestRepository.findById(requestId);
        if (opt.isEmpty()) return;
        FriendRequest fr = opt.get();
        if (!toUserId.equals(fr.getToUserId()) || fr.getStatus() != FriendRequest.FriendRequestStatus.PENDING) return;
        fr.setStatus(FriendRequest.FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(fr);
        Long fromUserId = fr.getFromUserId();
        if (!followRepository.existsByFollowerIdAndFollowingId(toUserId, fromUserId)) {
            Follow f1 = new Follow();
            f1.setFollowerId(toUserId);
            f1.setFollowingId(fromUserId);
            followRepository.save(f1);
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(fromUserId, toUserId)) {
            Follow f2 = new Follow();
            f2.setFollowerId(fromUserId);
            f2.setFollowingId(toUserId);
            followRepository.save(f2);
        }
        messagingTemplate.convertAndSend("/topic/user/" + toUserId, Map.<String, Object>of("type", "FRIENDS_UPDATED"));
        messagingTemplate.convertAndSend("/topic/user/" + fromUserId, Map.<String, Object>of("type", "FRIENDS_UPDATED"));
    }

    /** 거절 */
    @Transactional
    public void reject(Long requestId, Long toUserId) {
        if (requestId == null || toUserId == null) return;
        friendRequestRepository.findById(requestId).ifPresent(fr -> {
            if (toUserId.equals(fr.getToUserId()) && fr.getStatus() == FriendRequest.FriendRequestStatus.PENDING) {
                fr.setStatus(FriendRequest.FriendRequestStatus.REJECTED);
                friendRequestRepository.save(fr);
            }
        });
    }

    /** 친구 삭제(언프렌드): ACCEPTED 요청을 REJECTED로 변경하고 양방향 Follow 제거 */
    @Transactional
    public boolean removeFriend(Long userId, Long friendUserId) {
        if (userId == null || friendUserId == null || userId.equals(friendUserId)) return false;
        List<FriendRequest> accepted = friendRequestRepository.findAcceptedBetween(userId, friendUserId);
        if (accepted.isEmpty()) return false;
        for (FriendRequest fr : accepted) {
            fr.setStatus(FriendRequest.FriendRequestStatus.REJECTED);
            friendRequestRepository.save(fr);
        }
        followRepository.deleteByFollowerIdAndFollowingId(userId, friendUserId);
        followRepository.deleteByFollowerIdAndFollowingId(friendUserId, userId);
        messagingTemplate.convertAndSend("/topic/user/" + userId, Map.<String, Object>of("type", "FRIENDS_UPDATED"));
        messagingTemplate.convertAndSend("/topic/user/" + friendUserId, Map.<String, Object>of("type", "FRIENDS_UPDATED"));
        return true;
    }

    /** 차단: 친구 관계 해제 후 블록 저장 */
    @Transactional
    public boolean blockUser(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null || blockerId.equals(blockedId)) return false;
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) return true;
        removeFriend(blockerId, blockedId);
        UserBlock ub = new UserBlock();
        ub.setBlockerId(blockerId);
        ub.setBlockedId(blockedId);
        userBlockRepository.save(ub);
        return true;
    }

    /** 차단 해제 */
    @Transactional
    public boolean unblockUser(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) return false;
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) return false;
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        return true;
    }

    /** 차단 목록 (최신 차단순) */
    public List<FriendSearchResult> getBlockedUsers(Long blockerId) {
        if (blockerId == null) return List.of();
        List<UserBlock> blocks = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId);
        if (blocks.isEmpty()) return List.of();
        List<Long> blockedIds = blocks.stream().map(UserBlock::getBlockedId).collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(blockedIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        return blocks.stream()
                .map(b -> userMap.get(b.getBlockedId()))
                .filter(Objects::nonNull)
                .map(FriendSearchResult::from)
                .collect(Collectors.toList());
    }
}
