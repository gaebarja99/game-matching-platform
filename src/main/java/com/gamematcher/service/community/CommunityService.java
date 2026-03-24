package com.gamematcher.service.community;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.ReportStatus;
import com.gamematcher.constant.community.*;
import com.gamematcher.dto.community.*;
import com.gamematcher.entity.User;
import com.gamematcher.entity.community.*;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.CommonUserRepository;
import com.gamematcher.repository.community.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommunityService {

    private static final List<PostStatus> VISIBLE_STATUSES = List.of(PostStatus.ACTIVE, PostStatus.BLIND);
    private static final int POPULAR_RECOMMEND_THRESHOLD = 10;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRecommendRepository postRecommendRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final CommunityReportRepository communityReportRepository;
    private final CommunityNotificationRepository notificationRepository;
    private final CommonUserRepository userRepository;
    private final FileStorageService fileStorageService;

    public CommunityService(PostRepository postRepository, CommentRepository commentRepository,
                            PostLikeRepository postLikeRepository, PostBookmarkRepository postBookmarkRepository,
                            PostRecommendRepository postRecommendRepository,
                            PostAttachmentRepository postAttachmentRepository,
                            HashtagRepository hashtagRepository, PostHashtagRepository postHashtagRepository,
                            CommunityReportRepository communityReportRepository,
                            CommunityNotificationRepository notificationRepository,
                            CommonUserRepository userRepository, FileStorageService fileStorageService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.postBookmarkRepository = postBookmarkRepository;
        this.postRecommendRepository = postRecommendRepository;
        this.postAttachmentRepository = postAttachmentRepository;
        this.hashtagRepository = hashtagRepository;
        this.postHashtagRepository = postHashtagRepository;
        this.communityReportRepository = communityReportRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    // ========== 게시글 ==========

    @Transactional
    public Post createPost(Long userId, PostCreateRequestDto request, List<MultipartFile> files) {
        User author = getUser(userId);

        // 공지사항은 관리자만
        if (request.isNotice() && author.getRole() != Role.ADMIN) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "공지사항 작성 권한이 없습니다.");
        }

        if (request.getBoardCategory() == BoardCategory.NOTICE && author.getRole() != Role.ADMIN) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "공지 게시판은 관리자만 작성할 수 있습니다.");
        }

        Post post = new Post();
        post.setBoardCategory(request.getBoardCategory());
        post.setAuthor(author);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setNotice(request.isNotice());
        post.setStatus(PostStatus.ACTIVE);

        post = postRepository.save(post);

        // 해시태그 처리
        processHashtags(post, request.getHashtags());

        // 파일 첨부
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    addAttachment(post, file);
                }
            }
        }

        return postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getPostList(BoardCategory category, String keyword,
                                                  int page, int size, String sortBy) {
        Pageable pageable = createPageable(page, size, sortBy);
        Page<Post> postPage;
        String k = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        if (category != null) {
            postPage = k != null
                    ? postRepository.searchByBoardCategory(category, VISIBLE_STATUSES, k, pageable)
                    : postRepository.findByBoardCategoryAndStatusIn(category, VISIBLE_STATUSES, pageable);
        } else {
            postPage = k != null
                    ? postRepository.searchAll(VISIBLE_STATUSES, k, pageable)
                    : postRepository.findByStatusIn(VISIBLE_STATUSES, pageable);
        }

        // popular 여부 확인 (likeCount >= 10)
        return postPage.map(p -> PostListResponseDto.from(p, p.getRecommendCount() >= POPULAR_RECOMMEND_THRESHOLD));
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPopularPosts(BoardCategory category, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Post> posts = category != null
                ? postRepository.findPopularByBoardCategory(category, VISIBLE_STATUSES, pageable)
                : postRepository.findPopularAll(VISIBLE_STATUSES, pageable);
        return posts.stream()
                .map(p -> PostListResponseDto.from(p, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getMyPosts(Long userId, int page, int size) {
        getUser(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable)
                .map(p -> PostListResponseDto.from(p, p.getRecommendCount() >= POPULAR_RECOMMEND_THRESHOLD));
    }

    @Transactional
    public PostDetailResponseDto getPostDetail(Long postId, Long userId, boolean incrementViewCount) {
        Post post = getPost(postId);

        if (post.getStatus() == PostStatus.DELETED_BY_ADMIN || post.getStatus() == PostStatus.DELETED_BY_USER) {
            post.setTitle("삭제된 글입니다.");
            post.setContent("삭제된 글입니다.");
            post.getAttachments().clear();
            post.getPostHashtags().clear();
        } else if (post.getStatus() == PostStatus.BLIND) {
            post.setTitle("블라인드 처리된 글입니다.");
            post.setContent("블라인드 처리된 글입니다.");
            post.getAttachments().clear();
            post.getPostHashtags().clear();
        } else if (!VISIBLE_STATUSES.contains(post.getStatus())) {
            throw new GameApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        if (incrementViewCount && post.getStatus() == PostStatus.ACTIVE) {
            postRepository.incrementViewCount(postId);
            post.setViewCount(post.getViewCount() + 1);
        }

        boolean liked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);
        boolean bookmarked = userId != null && postBookmarkRepository.existsByPostIdAndUserId(postId, userId);
        Integer myRecommend = null;
        if (userId != null) {
            Optional<PostRecommend> rec = postRecommendRepository.findByPostIdAndUserId(postId, userId);
            myRecommend = rec.map(r -> r.getRecommendType() == RecommendType.RECOMMEND ? 1 : -1).orElse(null);
        }

        return PostDetailResponseDto.from(post, liked, bookmarked, myRecommend);
    }

    @Transactional
    public Post updatePost(Long postId, Long userId, PostUpdateRequestDto request) {
        Post post = getPost(postId);
        validateAuthor(post, userId);
        if (post.getBoardCategory() == BoardCategory.NOTICE && !isAdmin(userId)) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "공지 게시판은 관리자만 수정할 수 있습니다.");
        }

        if (!VISIBLE_STATUSES.contains(post.getStatus())) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "수정할 수 없는 게시글입니다.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        postHashtagRepository.deleteByPostId(postId);
        post.getPostHashtags().clear();
        processHashtags(post, request.getHashtags());

        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = getPost(postId);
        validateAuthor(post, userId);

        post.setStatus(PostStatus.DELETED_BY_USER);
        postRepository.save(post);
    }

    @Transactional
    public void addAttachmentToPost(Long postId, Long userId, MultipartFile file) {
        Post post = getPost(postId);
        validateAuthor(post, userId);
        addAttachment(post, file);
        postRepository.save(post);
    }

    // ========== 댓글 ==========

    @Transactional
    public Comment createComment(Long postId, Long userId, CommentCreateRequestDto request) {
        Post post = getPost(postId);
        User author = getUser(userId);

        if (!VISIBLE_STATUSES.contains(post.getStatus())) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "댓글을 작성할 수 없는 게시글입니다.");
        }

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(request.getContent());

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));
            if (!parent.getPost().getId().equals(postId)) {
                throw new GameApiException(HttpStatus.BAD_REQUEST, "잘못된 부모 댓글입니다.");
            }
            comment.setParent(parent);
        }

        comment = commentRepository.save(comment);

        // 댓글 수 업데이트
        post.setCommentCount(commentRepository.countByPostId(postId));
        postRepository.save(post);

        // 알림 (대댓글은 부모 작성자에게, 댓글은 게시글 작성자에게)
        Long notifyUserId = request.getParentId() != null
                ? comment.getParent().getAuthor().getId()
                : post.getAuthor().getId();
        if (!notifyUserId.equals(userId)) {
            createNotification(notifyUserId, request.getParentId() != null ? NotificationType.REPLY : NotificationType.COMMENT,
                    (request.getParentId() != null ? "대댓글이" : "댓글이") + " 달렸습니다.", postId, comment.getId(), userId);
        }

        return comment;
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "본인 댓글만 삭제할 수 있습니다.");
        }

        comment.setDeleted(true);
        comment.setContent("(삭제된 댓글입니다)");
        commentRepository.save(comment);

        // 대댓글도 삭제 표시
        for (Comment reply : comment.getReplies()) {
            reply.setDeleted(true);
            reply.setContent("(삭제된 댓글입니다)");
        }
        commentRepository.saveAll(comment.getReplies());

        postRepository.save(comment.getPost()); // 댓글 수는 유지 (삭제된 댓글도 카운트)
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId) {
        Post post = getPost(postId);
        List<Comment> topLevel = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId);
        return topLevel.stream()
                .map(c -> CommentResponseDto.from(c, true))
                .collect(Collectors.toList());
    }

    // ========== 좋아요 ==========

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        Post post = getPost(postId);
        User user = getUser(userId);

        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(user);
            postLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);

            if (!post.getAuthor().getId().equals(userId)) {
                createNotification(post.getAuthor().getId(), NotificationType.LIKE,
                        "게시글에 좋아요가 달렸습니다.", postId, null, userId);
            }
        }
        postRepository.save(post);
    }

    // ========== 북마크 ==========

    @Transactional
    public void toggleBookmark(Long postId, Long userId) {
        getPost(postId);
        getUser(userId);

        if (postBookmarkRepository.existsByPostIdAndUserId(postId, userId)) {
            postBookmarkRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            PostBookmark bookmark = new PostBookmark();
            bookmark.setPost(getPost(postId));
            bookmark.setUser(getUser(userId));
            postBookmarkRepository.save(bookmark);
        }
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getBookmarks(Long userId, int page, int size) {
        User user = getUser(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostBookmark> bookmarks = postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return bookmarks.map(b -> PostListResponseDto.from(b.getPost(), b.getPost().getRecommendCount() >= POPULAR_RECOMMEND_THRESHOLD));
    }

    // ========== 추천/비추천 ==========

    @Transactional
    public void recommendPost(Long postId, Long userId, RecommendType type) {
        Post post = getPost(postId);
        getUser(userId);

        Optional<PostRecommend> existing = postRecommendRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            PostRecommend rec = existing.get();
            if (rec.getRecommendType() == type) {
                postRecommendRepository.delete(rec);
                if (type == RecommendType.RECOMMEND) {
                    post.setRecommendCount(Math.max(0, post.getRecommendCount() - 1));
                } else {
                    post.setNotRecommendCount(Math.max(0, post.getNotRecommendCount() - 1));
                }
            } else {
                rec.setRecommendType(type);
                postRecommendRepository.save(rec);
                if (type == RecommendType.RECOMMEND) {
                    post.setRecommendCount(post.getRecommendCount() + 1);
                    post.setNotRecommendCount(Math.max(0, post.getNotRecommendCount() - 1));
                } else {
                    post.setNotRecommendCount(post.getNotRecommendCount() + 1);
                    post.setRecommendCount(Math.max(0, post.getRecommendCount() - 1));
                }
            }
        } else {
            PostRecommend rec = new PostRecommend();
            rec.setPost(post);
            rec.setUser(getUser(userId));
            rec.setRecommendType(type);
            postRecommendRepository.save(rec);
            if (type == RecommendType.RECOMMEND) {
                post.setRecommendCount(post.getRecommendCount() + 1);
            } else {
                post.setNotRecommendCount(post.getNotRecommendCount() + 1);
            }
        }
        postRepository.save(post);
    }

    // ========== 신고 ==========

    @Transactional
    public void reportPostOrComment(Long userId, CommunityReportRequestDto request) {
        User reporter = getUser(userId);

        Long commentId = request.getCommentId();
        boolean isPostReport = request.getTargetType() == ReportTargetType.POST;

        if (isPostReport) {
            getPost(request.getPostId());
            if (communityReportRepository.existsByReporterIdAndTargetTypeAndPostIdAndCommentIdIsNull(
                    userId, ReportTargetType.POST, request.getPostId())) {
                throw new GameApiException(HttpStatus.CONFLICT, "이미 해당 게시글을 신고했습니다.");
            }
        } else {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
            if (!comment.getPost().getId().equals(request.getPostId())) {
                throw new GameApiException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");
            }
            if (communityReportRepository.existsByReporterIdAndTargetTypeAndPostIdAndCommentId(
                    userId, ReportTargetType.COMMENT, request.getPostId(), commentId)) {
                throw new GameApiException(HttpStatus.CONFLICT, "이미 해당 댓글을 신고했습니다.");
            }
        }

        CommunityReport report = new CommunityReport();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setPostId(request.getPostId());
        report.setCommentId(commentId);
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setStatus(ReportStatus.PENDING);
        communityReportRepository.save(report);

        if (isPostReport) {
            Post post = getPost(request.getPostId());
            post.setReportCount(post.getReportCount() + 1);
            postRepository.save(post);
        } else {
            Comment comment = commentRepository.findById(commentId).orElseThrow();
            comment.setReportCount(comment.getReportCount() + 1);
            commentRepository.save(comment);
        }
    }

    // ========== 알림 ==========

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long userId, int page, int size) {
        getUser(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponseDto::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markNotificationRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        if (!notification.getUser().getId().equals(userId)) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "본인 알림만 읽음 처리할 수 있습니다.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // ========== 내부 헬퍼 ==========

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private void validateAuthor(Post post, Long userId) {
        if (!post.getAuthor().getId().equals(userId) && !isAdmin(userId)) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);
    }

    private Pageable createPageable(int page, int size, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "view" -> sort = Sort.by(Sort.Direction.DESC, "viewCount", "createdAt");
                case "like" -> sort = Sort.by(Sort.Direction.DESC, "likeCount", "createdAt");
                case "comment" -> sort = Sort.by(Sort.Direction.DESC, "commentCount", "createdAt");
            }
        }
        return PageRequest.of(page, size, sort);
    }

    private void processHashtags(Post post, List<String> hashtagNames) {
        if (hashtagNames == null || hashtagNames.isEmpty()) {
            return;
        }
        for (String name : hashtagNames) {
            String tag = name.startsWith("#") ? name.substring(1).trim() : name.trim();
            if (tag.isEmpty()) continue;

            Hashtag hashtag = hashtagRepository.findByName(tag)
                    .orElseGet(() -> {
                        Hashtag h = new Hashtag();
                        h.setName(tag);
                        return hashtagRepository.save(h);
                    });

            PostHashtag ph = new PostHashtag();
            ph.setPost(post);
            ph.setHashtag(hashtag);
            postHashtagRepository.save(ph);
        }
    }

    private void addAttachment(Post post, MultipartFile file) {
        String storedName = fileStorageService.storeFile(file);
        String originalName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());

        PostAttachment attachment = new PostAttachment();
        attachment.setPost(post);
        attachment.setFileName(originalName);
        attachment.setFilePath("/uploads/community/" + storedName);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        postAttachmentRepository.save(attachment);
    }

    private void createNotification(Long userId, NotificationType type, String message,
                                    Long postId, Long commentId, Long actorId) {
        Notification n = new Notification();
        n.setUser(getUser(userId));
        n.setType(type);
        n.setMessage(message);
        n.setPostId(postId);
        n.setCommentId(commentId);
        n.setActorId(actorId);
        notificationRepository.save(n);
    }
}
