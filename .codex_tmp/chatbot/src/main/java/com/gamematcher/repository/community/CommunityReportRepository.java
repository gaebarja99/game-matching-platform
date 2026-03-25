package com.gamematcher.repository.community;

import com.gamematcher.constant.ReportStatus;
import com.gamematcher.constant.community.ReportTargetType;
import com.gamematcher.entity.community.CommunityReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {

    boolean existsByReporterIdAndTargetTypeAndPostIdAndCommentId(
            Long reporterId, ReportTargetType targetType, Long postId, Long commentId);

    boolean existsByReporterIdAndTargetTypeAndPostIdAndCommentIdIsNull(
            Long reporterId, ReportTargetType targetType, Long postId);

    Page<CommunityReport> findByStatus(ReportStatus status, Pageable pageable);

    Page<CommunityReport> findByTargetTypeAndPostId(ReportTargetType targetType, Long postId, Pageable pageable);
}
