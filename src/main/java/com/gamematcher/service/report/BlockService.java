package com.gamematcher.service.report;

import com.gamematcher.entity.BlockedUser;
import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.BlockedUserRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlockService {

    private final BlockedUserRepository blockedUserRepository;
    private final CommonUserRepository userRepository;

    public BlockService(BlockedUserRepository blockedUserRepository, CommonUserRepository userRepository) {
        this.blockedUserRepository = blockedUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BlockedUser blockUser(Long blockerId, Long blockedUserId) {
        if (blockerId.equals(blockedUserId)) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "자기 자신은 차단할 수 없습니다.");
        }
        if (blockedUserRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId)) {
            throw new GameApiException(HttpStatus.CONFLICT, "이미 해당 사용자를 차단했습니다.");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "차단 요청자를 찾을 수 없습니다."));
        User blockedUser = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "차단할 사용자를 찾을 수 없습니다."));

        BlockedUser blocked = new BlockedUser();
        blocked.setBlocker(blocker);
        blocked.setBlockedUser(blockedUser);

        return blockedUserRepository.save(blocked);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedUserId) {
        BlockedUser blockedUser = blockedUserRepository.findByBlockerIdAndBlockedUserId(blockerId, blockedUserId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "차단 목록에 해당 사용자가 없습니다."));
        blockedUserRepository.delete(blockedUser);
    }

    @Transactional(readOnly = true)
    public List<BlockedUser> getBlockedList(Long blockerId) {
        return blockedUserRepository.findByBlockerId(blockerId);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long targetUserId) {
        return blockedUserRepository.existsByBlockerIdAndBlockedUserId(blockerId, targetUserId);
    }

    /**
     * 차단자 제외: userId 목록에서 blocker가 차단한 사용자를 필터링
     * 매칭/목록 조회 시 사용 예: filterBlockedUserIds(currentUserId, candidateIds)
     */
    @Transactional(readOnly = true)
    public List<Long> filterBlockedUserIds(Long blockerId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<BlockedUser> blockedList = blockedUserRepository.findByBlockerId(blockerId);
        List<Long> blockedIds = blockedList.stream()
                .map(b -> b.getBlockedUser().getId())
                .collect(Collectors.toList());
        return userIds.stream()
                .filter(id -> !blockedIds.contains(id))
                .collect(Collectors.toList());
    }
}
