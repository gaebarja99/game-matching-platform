package com.gamematcher.repository.community;

import com.gamematcher.constant.community.BoardCategory;
import com.gamematcher.constant.community.PostStatus;
import com.gamematcher.entity.community.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByBoardCategoryAndStatusIn(
            BoardCategory boardCategory,
            List<PostStatus> statuses,
            Pageable pageable);

    Page<Post> findByStatusIn(List<PostStatus> statuses, Pageable pageable);

    Page<Post> findByAuthorIdAndStatusIn(Long authorId, List<PostStatus> statuses, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.boardCategory = :category AND p.status IN :statuses " +
            "AND (:keyword IS NULL OR :keyword = '' OR p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))")
    Page<Post> searchByBoardCategory(
            @Param("category") BoardCategory category,
            @Param("statuses") List<PostStatus> statuses,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status IN :statuses " +
            "AND (:keyword IS NULL OR :keyword = '' OR p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))")
    Page<Post> searchAll(
            @Param("statuses") List<PostStatus> statuses,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.boardCategory = :category AND p.status IN :statuses " +
            "ORDER BY p.recommendCount DESC, p.viewCount DESC, p.createdAt DESC")
    List<Post> findPopularByBoardCategory(
            @Param("category") BoardCategory category,
            @Param("statuses") List<PostStatus> statuses,
            Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status IN :statuses " +
            "ORDER BY p.recommendCount DESC, p.viewCount DESC, p.createdAt DESC")
    List<Post> findPopularAll(
            @Param("statuses") List<PostStatus> statuses,
            Pageable pageable);

    Page<Post> findByBoardCategoryAndStatusInAndIsNoticeOrderByCreatedAtDesc(
            BoardCategory boardCategory,
            List<PostStatus> statuses,
            boolean isNotice,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    boolean existsByIdAndAuthorId(Long postId, Long authorId);
}
