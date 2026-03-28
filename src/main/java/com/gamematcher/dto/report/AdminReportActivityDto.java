package com.gamematcher.dto.report;

import com.gamematcher.constant.ReportReason;
import com.gamematcher.constant.ReportStatus;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.entity.AdminAuditLog;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.Report;
import com.gamematcher.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminReportActivityDto {

    private Long id;
    private String entryType;
    private Long reportId;
    private Long reporterId;
    private String reporterUsername;
    private Long reportedUserId;
    private String reportedUsername;
    private UserStatus reportedUserStatus;
    private String reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String adminNote;

    public static AdminReportActivityDto fromReport(Report report) {
        AdminReportActivityDto dto = new AdminReportActivityDto();
        dto.setId(report.getId());
        dto.setEntryType("USER_REPORT");
        dto.setReportId(report.getId());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterUsername(report.getReporter().getUsername());
        dto.setReportedUserId(report.getReportedUser().getId());
        dto.setReportedUsername(report.getReportedUser().getUsername());
        dto.setReportedUserStatus(report.getReportedUser().getStatus());
        ReportReason reason = report.getReason();
        dto.setReason(reason != null ? reason.name() : "신고");
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        dto.setResolvedAt(report.getResolvedAt());
        dto.setAdminNote(report.getAdminNote());
        return dto;
    }

    public static AdminReportActivityDto fromWarning(AdminAuditLog log, LiveStream stream, User streamer) {
        AdminReportActivityDto dto = new AdminReportActivityDto();
        dto.setId(log.getId());
        dto.setEntryType("ADMIN_WARNING");
        dto.setReportId(null);
        dto.setReporterId(log.getAdminUserId());
        dto.setReporterUsername("운영자");
        dto.setReportedUserId(streamer != null ? streamer.getId() : null);
        dto.setReportedUsername(streamer != null ? streamer.getUsername() : "알 수 없는 사용자");
        dto.setReportedUserStatus(streamer != null ? streamer.getStatus() : UserStatus.ACTIVE);
        dto.setReason(log.getSummary() != null && !log.getSummary().isBlank() ? log.getSummary() : "방송 경고");
        String streamTitle = stream != null && stream.getTitle() != null && !stream.getTitle().isBlank()
                ? stream.getTitle()
                : "방송 제목 없음";
        String warningMessage = log.getDetail() != null && !log.getDetail().isBlank()
                ? log.getDetail()
                : stream != null ? stream.getLastAdminWarningMessage() : null;
        dto.setDescription("방송: " + streamTitle + (warningMessage != null && !warningMessage.isBlank() ? " / " + warningMessage : ""));
        dto.setStatus(ReportStatus.RESOLVED);
        dto.setCreatedAt(log.getCreatedAt());
        dto.setUpdatedAt(log.getCreatedAt());
        dto.setResolvedAt(log.getCreatedAt());
        dto.setAdminNote(warningMessage);
        return dto;
    }
}
