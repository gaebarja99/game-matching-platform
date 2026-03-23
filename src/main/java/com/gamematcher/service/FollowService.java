package com.gamematcher.service;

import com.gamematcher.dto.follow.FollowerItemDto;
import com.gamematcher.dto.follow.FollowingItemDto;
import com.gamematcher.entity.Follow;
import com.gamematcher.repository.FollowRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            return;
        }
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return;
        }
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            return;
        }
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    /** 내가 팔로우한 사용자 목록 (userId, nickname) */
    public List<FollowingItemDto> getFollowingList(Long followerId) {
        if (followerId == null) return List.of();
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(followerId).stream()
                .map(f -> {
                    return userRepository.findById(f.getFollowingId())
                            .map(u -> {
                                String nickname = (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername();
                                return new FollowingItemDto(f.getFollowingId(), nickname, u.getProfileImageUrl());
                            })
                            .orElse(new FollowingItemDto(f.getFollowingId(), "유저", null));
                })
                .collect(Collectors.toList());
    }

    /** 내가 팔로우한 사용자 ID 목록 */
    public List<Long> getFollowingUserIds(Long followerId) {
        if (followerId == null) return List.of();
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(followerId).stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());
    }

    /** 나를 팔로우한 사용자 목록 (스트리머용 팔로워 목록), 최신순 */
    public List<FollowerItemDto> getFollowerList(Long followingId) {
        if (followingId == null) return List.of();
        return followRepository.findByFollowingIdOrderByCreatedAtDesc(followingId).stream()
                .map(f -> userRepository.findById(f.getFollowerId())
                        .map(u -> {
                            String nickname = (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername();
                            return new FollowerItemDto(u.getId(), nickname, u.getLoginId(), f.getCreatedAt());
                        })
                        .orElse(null))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
