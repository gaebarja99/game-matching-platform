package com.gamematcher.controller.report;

import com.gamematcher.constant.ReportStatus;
import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.dto.report.ReportResponseDto;
import com.gamematcher.entity.AdminAuditLog;
import com.gamematcher.entity.Report;
import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.AdminAuditLogRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import com.gamematcher.repository.common.ReportRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 신고 처리 API
 * TODO: 운영 시 Spring Security로 ADMIN 역할만 접근 가능하도록 보안 설정 필요
 */
@RestController
@RequestMapping("/api/admin/reports")
public class ReportAdminController {

    private static final String SESSION_USER_ID = "userId";

    private final ReportRepository reportRepository;
    private final CommonUserRepository userRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    public ReportAdminController(
            ReportRepository reportRepository,
            CommonUserRepository userRepository,
            AdminAuditLogRepository adminAuditLogRepository
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    private void logAction(Long adminUserId, String targetType, Long targetId, String actionType, String summary, String detail) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(adminUserId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActionType(actionType);
        log.setSummary(summary);
        log.setDetail(detail);
        adminAuditLogRepository.save(log);
    }

    /** 신고 목록 조회 (상태별 필터) */
    @GetMapping
    public List<ReportResponseDto> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Report> reports = status != null
                ? reportRepository.findByStatus(status, PageRequest.of(page, size))
                : reportRepository.findAll(PageRequest.of(page, size));
        return reports.getContent().stream()
                .map(ReportResponseDto::from)
                .collect(Collectors.toList());
    }

    /** 신고 상태 업데이트 */
    @PatchMapping("/{reportId}")
    public ReportResponseDto updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam ReportStatus status,
            @RequestParam(required = false) String adminNote,
            HttpSession session) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));

        report.setStatus(status);
        report.setAdminNote(adminNote);
        if (status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED) {
            report.setResolvedAt(LocalDateTime.now());
        }
        reportRepository.save(report);

        Long adminUserId = session != null ? (Long) session.getAttribute(SESSION_USER_ID) : null;
        logAction(adminUserId, "REPORT", reportId, "REPORT_STATUS_CHANGED", "신고 상태 변경",
                "상태=" + status.name() + ", 메모=" + (adminNote == null ? "" : adminNote));

        return ReportResponseDto.from(report);
    }

    @PostMapping("/{reportId}/ban")
    public ReportResponseDto banReportedUser(
            @PathVariable Long reportId,
            @RequestParam(required = false) String adminNote,
            HttpSession session) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));

        User reportedUser = userRepository.findById(report.getReportedUser().getId())
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "피신고 사용자를 찾을 수 없습니다."));

        if (reportedUser.getRole() == Role.ADMIN) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "관리자 계정은 정지할 수 없습니다.");
        }

        reportedUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(reportedUser);

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        report.setAdminNote(
                adminNote == null || adminNote.isBlank()
                        ? "관리자에 의해 계정이 정지되었습니다."
                        : adminNote
        );
        reportRepository.save(report);

        Long adminUserId = session != null ? (Long) session.getAttribute(SESSION_USER_ID) : null;
        logAction(adminUserId, "REPORT", reportId, "REPORT_BAN", "신고 처리 정지", report.getAdminNote());
        logAction(adminUserId, "USER", reportedUser.getId(), "USER_SUSPENDED", "신고 처리 정지", report.getAdminNote());

        return ReportResponseDto.from(report);
    }

    @PostMapping("/{reportId}/unban")
    public ReportResponseDto unbanReportedUser(
            @PathVariable Long reportId,
            @RequestParam(required = false) String adminNote,
            HttpSession session) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));

        User reportedUser = userRepository.findById(report.getReportedUser().getId())
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "피신고 사용자를 찾을 수 없습니다."));

        if (reportedUser.getRole() == Role.ADMIN) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "관리자 계정은 상태를 변경할 수 없습니다.");
        }

        reportedUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(reportedUser);

        report.setAdminNote(
                adminNote == null || adminNote.isBlank()
                        ? "관리자에 의해 계정 정지가 해제되었습니다."
                        : adminNote
        );
        reportRepository.save(report);

        Long adminUserId = session != null ? (Long) session.getAttribute(SESSION_USER_ID) : null;
        logAction(adminUserId, "REPORT", reportId, "REPORT_UNBAN", "신고 처리 해제", report.getAdminNote());
        logAction(adminUserId, "USER", reportedUser.getId(), "USER_UNSUSPENDED", "신고 처리 해제", report.getAdminNote());

        return ReportResponseDto.from(report);
    }
}
