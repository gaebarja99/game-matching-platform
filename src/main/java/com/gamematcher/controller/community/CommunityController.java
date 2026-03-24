package com.gamematcher.controller.community;

import com.gamematcher.constant.community.BoardCategory;
import com.gamematcher.constant.community.RecommendType;
import com.gamematcher.dto.community.*;
import com.gamematcher.entity.community.Comment;
import com.gamematcher.entity.community.Post;
import com.gamematcher.service.community.CommunityService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    // ========== 게시판 목록 (비로그인도 조회 가능) ==========
    @GetMapping("/boards/{category}/posts")
    public PageResponseDto<PostListResponseDto> getPostList(
            @PathVariable BoardCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        Page<PostListResponseDto> result = communityService.getPostList(category, keyword, page, size, sortBy);
        return PageResponseDto.of(result);
    }

    // ========== 전체 검색 ==========
    @GetMapping("/posts")
    public PageResponseDto<PostListResponseDto> searchAllPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        Page<PostListResponseDto> result = communityService.getPostList(null, keyword, page, size, sortBy);
        return PageResponseDto.of(result);
    }

    // ========== 인기글 ==========
    @GetMapping("/boards/{category}/posts/popular")
    public List<PostListResponseDto> getPopularPosts(
            @PathVariable BoardCategory category,
            @RequestParam(defaultValue = "5") int limit) {
        return communityService.getPopularPosts(category, limit);
    }

    @GetMapping("/posts/popular")
    public List<PostListResponseDto> getAllPopularPosts(
            @RequestParam(defaultValue = "5") int limit) {
        return communityService.getPopularPosts(null, limit);
    }

    // ========== 게시글 상세 (userId는 로그인 사용자, 비로그인 시 조회만 가능하게 하려면 별도 엔드포인트 필요) ==========
    @GetMapping("/posts/{postId}")
    public PostDetailResponseDto getPostDetail(
            @PathVariable Long userId,
            @PathVariable Long postId,
            HttpSession session) {
        return communityService.getPostDetail(postId, userId, markViewed(postId, session));
    }

    // ========== 게시글 작성 ==========
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponseDto createPost(
            @PathVariable Long userId,
            @Valid @RequestPart("post") PostCreateRequestDto request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Post post = communityService.createPost(userId, request, files != null ? files : List.of());
        return communityService.getPostDetail(post.getId(), userId, false);
    }

    /** 파일 없이 게시글 작성 (JSON only) */
    @PostMapping("/posts/json")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponseDto createPostJson(
            @PathVariable Long userId,
            @Valid @RequestBody PostCreateRequestDto request) {
        Post post = communityService.createPost(userId, request, List.of());
        return communityService.getPostDetail(post.getId(), userId, false);
    }

    // ========== 게시글 수정 ==========
    @PutMapping("/posts/{postId}")
    public PostDetailResponseDto updatePost(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequestDto request) {
        communityService.updatePost(postId, userId, request);
        return communityService.getPostDetail(postId, userId, false);
    }

    // ========== 게시글 삭제 ==========
    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable Long userId,
            @PathVariable Long postId) {
        communityService.deletePost(postId, userId);
    }

    // ========== 파일 첨부 추가 ==========
    @PostMapping("/posts/{postId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public void addAttachment(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file) {
        communityService.addAttachmentToPost(postId, userId, file);
    }

    // ========== 댓글 ==========
    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponseDto> getComments(@PathVariable Long postId) {
        return communityService.getComments(postId);
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto createComment(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequestDto request) {
        Comment comment = communityService.createComment(postId, userId, request);
        return CommentResponseDto.from(comment, false);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        communityService.deleteComment(commentId, userId);
    }

    // ========== 좋아요 ==========
    @PostMapping("/posts/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleLike(
            @PathVariable Long userId,
            @PathVariable Long postId) {
        communityService.toggleLike(postId, userId);
    }

    // ========== 북마크 ==========
    @PostMapping("/posts/{postId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleBookmark(
            @PathVariable Long userId,
            @PathVariable Long postId) {
        communityService.toggleBookmark(postId, userId);
    }

    @GetMapping("/bookmarks")
    public PageResponseDto<PostListResponseDto> getBookmarks(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponseDto.of(communityService.getBookmarks(userId, page, size));
    }

    @GetMapping("/my-posts")
    public PageResponseDto<PostListResponseDto> getMyPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponseDto.of(communityService.getMyPosts(userId, page, size));
    }

    // ========== 추천/비추천 ==========
    @PostMapping("/posts/{postId}/recommend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recommendPost(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @RequestParam RecommendType type) {
        communityService.recommendPost(postId, userId, type);
    }

    // ========== 신고 ==========
    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public void reportPostOrComment(
            @PathVariable Long userId,
            @Valid @RequestBody CommunityReportRequestDto request) {
        communityService.reportPostOrComment(userId, request);
    }

    // ========== 알림 ==========
    @GetMapping("/notifications")
    public PageResponseDto<NotificationResponseDto> getNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponseDto.of(communityService.getNotifications(userId, page, size));
    }

    @GetMapping("/notifications/unread-count")
    public long getUnreadNotificationCount(@PathVariable Long userId) {
        return communityService.getUnreadNotificationCount(userId);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markNotificationRead(
            @PathVariable Long userId,
            @PathVariable Long notificationId) {
        communityService.markNotificationRead(notificationId, userId);
    }

    @SuppressWarnings("unchecked")
    private boolean markViewed(Long postId, HttpSession session) {
        if (session == null || postId == null) return true;
        Object viewedPosts = session.getAttribute("communityViewedPosts");
        java.util.Set<Long> viewed;
        if (viewedPosts instanceof java.util.Set<?>) {
            viewed = (java.util.Set<Long>) viewedPosts;
        } else {
            viewed = new java.util.HashSet<>();
            session.setAttribute("communityViewedPosts", viewed);
        }
        if (viewed.contains(postId)) {
            return false;
        }
        viewed.add(postId);
        return true;
    }
}
