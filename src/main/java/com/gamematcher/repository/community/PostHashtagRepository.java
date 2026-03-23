package com.gamematcher.repository.community;

import com.gamematcher.entity.community.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    List<PostHashtag> findByPostId(Long postId);

    void deleteByPostId(Long postId);
}
