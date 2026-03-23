package com.gamematcher.dto.community;

import com.gamematcher.constant.ReportReason;
import com.gamematcher.constant.ReportStatus;
import com.gamematcher.constant.community.ReportTargetType;
import com.gamematcher.entity.community.CommunityReport;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CommunityReportResponseDto {

    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private ReportTargetType targetType;
    private Long postId;
    private Long commentId;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String adminNote;

    public static CommunityReportResponseDto from(CommunityReport report) {
        CommunityReportResponseDto dto = new CommunityReportResponseDto();
        dto.id = report.getId();
        dto.reporterId = report.getReporter().getId();
        dto.reporterUsername = report.getReporter().getUsername();
        dto.targetType = report.getTargetType();
        dto.postId = report.getPostId();
        dto.commentId = report.getCommentId();
        dto.reason = report.getReason();
        dto.description = report.getDescription();
        dto.status = report.getStatus();
        dto.createdAt = report.getCreatedAt();
        dto.resolvedAt = report.getResolvedAt();
        dto.adminNote = report.getAdminNote();
        return dto;
    }
}
