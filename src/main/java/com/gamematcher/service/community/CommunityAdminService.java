package com.gamematcher.service.community;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.ReportStatus;
import com.gamematcher.constant.community.NotificationType;
import com.gamematcher.constant.community.PostStatus;
import com.gamematcher.dto.community.CommunityReportResponseDto;
import com.gamematcher.dto.community.PageResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.entity.community.CommunityReport;
import com.gamematcher.entity.community.Comment;
import com.gamematcher.entity.community.Notification;
import com.gamematcher.entity.community.Post;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.CommonUserRepository;
import com.gamematcher.repository.community.CommentRepository;
import com.gamematcher.repository.community.CommunityReportRepository;
import com.gamematcher.repository.community.CommunityNotificationRepository;
import com.gamematcher.repository.community.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommunityAdminService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommunityReportRepository communityReportRepository;
    private final CommunityNotificationRepository notificationRepository;
    private final CommonUserRepository userRepository;

    public CommunityAdminService(PostRepository postRepository, CommentRepository commentRepository,
                                 CommunityReportRepository communityReportRepository,
                                 CommunityNotificationRepository notificationRepository,
                                 CommonUserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.communityReportRepository = communityReportRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void deletePostByAdmin(Long postId, Long adminUserId) {
        validateAdmin(adminUserId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        post.setStatus(PostStatus.DELETED_BY_ADMIN);
        postRepository.save(post);

        // 작성자에게 알림
        createNotification(post.getAuthor().getId(), NotificationType.POST_DELETED,
                "관리자에 의해 게시글이 삭제되었습니다.", postId, null, adminUserId);
    }

    @Transactional
    public void blindPost(Long postId, Long adminUserId) {
        validateAdmin(adminUserId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        post.setStatus(PostStatus.BLIND);
        postRepository.save(post);

        createNotification(post.getAuthor().getId(), NotificationType.POST_DELETED,
                "게시글이 블라인드 처리되었습니다.", postId, null, adminUserId);
    }

    @Transactional
    public void restorePost(Long postId, Long adminUserId) {
        validateAdmin(adminUserId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        post.setStatus(PostStatus.ACTIVE);
        postRepository.save(post);
    }

    @Transactional
    public void deleteCommentByAdmin(Long commentId, Long adminUserId) {
        validateAdmin(adminUserId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        comment.setDeleted(true);
        comment.setContent("(관리자에 의해 삭제된 댓글입니다)");
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommunityReport> getReportList(ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return status != null
                ? communityReportRepository.findByStatus(status, pageable)
                : communityReportRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CommunityReportResponseDto> getReportListDto(ReportStatus status, int page, int size) {
        Page<CommunityReport> pageResult = getReportList(status, page, size);
        Page<CommunityReportResponseDto> dtoPage = pageResult.map(CommunityReportResponseDto::from);
        return PageResponseDto.of(dtoPage);
    }

    @Transactional
    public void resolveReport(Long reportId, Long adminUserId, boolean approve, String adminNote) {
        validateAdmin(adminUserId);

        CommunityReport report = communityReportRepository.findById(reportId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "이미 처리된 신고입니다.");
        }

        report.setStatus(approve ? ReportStatus.RESOLVED : ReportStatus.DISMISSED);
        report.setResolvedAt(LocalDateTime.now());
        report.setAdminNote(adminNote);
        communityReportRepository.save(report);

        if (approve) {
            if (report.getTargetType() == com.gamematcher.constant.community.ReportTargetType.POST) {
                Post post = postRepository.findById(report.getPostId()).orElse(null);
                if (post != null) {
                    post.setStatus(PostStatus.BLIND);
                    postRepository.save(post);
                    createNotification(post.getAuthor().getId(), NotificationType.REPORT_RESOLVED,
                            "게시글이 신고에 의해 블라인드 처리되었습니다.", post.getId(), null, adminUserId);
                }
            } else {
                Comment comment = report.getCommentId() != null
                        ? commentRepository.findById(report.getCommentId()).orElse(null)
                        : null;
                if (comment != null) {
                    comment.setDeleted(true);
                    comment.setContent("(관리자에 의해 삭제된 댓글입니다)");
                    commentRepository.save(comment);
                }
            }
        }
    }

    private void validateAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getRole() != Role.ADMIN) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
    }

    private void createNotification(Long userId, NotificationType type, String message,
                                    Long postId, Long commentId, Long actorId) {
        Notification n = new Notification();
        n.setUser(userRepository.findById(userId).orElseThrow());
        n.setType(type);
        n.setMessage(message);
        n.setPostId(postId);
        n.setCommentId(commentId);
        n.setActorId(actorId);
        notificationRepository.save(n);
    }
}
