package com.gamematcher.repository.community;

import com.gamematcher.entity.community.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtAsc(Long postId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    int countByPostId(Long postId);

    boolean existsByIdAndAuthorId(Long commentId, Long authorId);
}
