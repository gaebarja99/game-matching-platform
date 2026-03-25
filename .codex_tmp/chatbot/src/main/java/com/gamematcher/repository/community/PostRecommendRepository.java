package com.gamematcher.repository.community;

import com.gamematcher.constant.community.RecommendType;
import com.gamematcher.entity.community.PostRecommend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRecommendRepository extends JpaRepository<PostRecommend, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<PostRecommend> findByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);
}
