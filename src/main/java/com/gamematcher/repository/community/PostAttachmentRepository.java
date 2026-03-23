package com.gamematcher.repository.community;

import com.gamematcher.entity.community.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    List<PostAttachment> findByPostId(Long postId);

    void deleteByPostId(Long postId);
}
