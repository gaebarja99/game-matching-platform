package com.gamematcher.repository;

import com.gamematcher.entity.Follow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long followingId);

    /** 내가 팔로우한 사용자 ID 목록 (최신순) */
    java.util.List<Follow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);

    /** 해당 사용자를 팔로우한 사람들의 목록 (followingId = 해당 사용자) */
    java.util.List<Follow> findByFollowingId(Long followingId);

    /** 해당 사용자를 팔로우한 사람들의 목록, 최신순 */
    java.util.List<Follow> findByFollowingIdOrderByCreatedAtDesc(Long followingId);

    /** 팔로우 순위: 팔로워 수 많은 순 userId 목록 (상위 N명) */
    @Query("SELECT f.followingId FROM Follow f GROUP BY f.followingId ORDER BY COUNT(f) DESC")
    List<Long> findTopUserIdsByFollowerCount(Pageable pageable);
}
