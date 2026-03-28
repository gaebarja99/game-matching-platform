package com.gamematcher.controller.report;

import com.gamematcher.constant.ReportStatus;
import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.dto.report.AdminReportActivityDto;
import com.gamematcher.dto.report.ReportResponseDto;
import com.gamematcher.entity.AdminAuditLog;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.Report;
import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.AdminAuditLogRepository;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import com.gamematcher.repository.common.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportAdminController {

    private final ReportRepository reportRepository;
    private final CommonUserRepository userRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final LiveStreamRepository liveStreamRepository;

    public ReportAdminController(
            ReportRepository reportRepository,
            CommonUserRepository userRepository,
            AdminAuditLogRepository adminAuditLogRepository,
            LiveStreamRepository liveStreamRepository
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.liveStreamRepository = liveStreamRepository;
    }

    @GetMapping
    public List<AdminReportActivityDto> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AdminReportActivityDto> activities = new ArrayList<>();

        activities.addAll(reportRepository.findAll().stream()
                .filter(report -> status == null || report.getStatus() == status)
                .map(AdminReportActivityDto::fromReport)
                .collect(Collectors.toList()));

        if (status == null || status == ReportStatus.RESOLVED) {
            Map<Long, LiveStream> streamMap = liveStreamRepository.findAll().stream()
                    .collect(Collectors.toMap(LiveStream::getId, stream -> stream));
            Map<Long, User> userMap = userRepository.findAll().stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            List<AdminAuditLog> warningLogs = adminAuditLogRepository.findByActionTypeOrderByCreatedAtDesc("STREAM_WARNED");
            activities.addAll(warningLogs.stream()
                    .map(log -> {
                        LiveStream stream = streamMap.get(log.getTargetId());
                        if (stream == null) {
                            return null;
                        }
                        User streamer = userMap.get(stream.getUserId());
                        return AdminReportActivityDto.fromWarning(log, stream, streamer);
                    })
                    .filter(activity -> activity != null)
                    .collect(Collectors.toList()));
        }

        activities.sort(Comparator.comparing(
                AdminReportActivityDto::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, activities.size());
        int toIndex = Math.min(fromIndex + safeSize, activities.size());
        return activities.subList(fromIndex, toIndex);
    }

    @PatchMapping("/{reportId}")
    public ReportResponseDto updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam ReportStatus status,
            @RequestParam(required = false) String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));

        report.setStatus(status);
        report.setAdminNote(adminNote);
        if (status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED) {
            report.setResolvedAt(LocalDateTime.now());
        }
        reportRepository.save(report);

        return ReportResponseDto.from(report);
    }

    @PostMapping("/{reportId}/ban")
    public ReportResponseDto banReportedUser(
            @PathVariable Long reportId,
            @RequestParam(required = false) String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));

        User reportedUser = userRepository.findById(report.getReportedUser().getId())
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고 대상 사용자를 찾을 수 없습니다."));

        if (reportedUser.getRole() == Role.ADMIN) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "관리자 계정은 정지할 수 없습니다.");
        }

        reportedUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(reportedUser);

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        report.setAdminNote(
                adminNote == null || adminNote.isBlank()
                        ? "운영자에 의해 계정이 정지되었습니다."
                        : adminNote
        );
        reportRepository.save(report);

        return ReportResponseDto.from(report);
    }

    @PostMapping("/{reportId}/unban")
    public ReportResponseDto unbanReportedUser(
            @PathVariable Long reportId,
            @RequestParam(required = false) String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));

        User reportedUser = userRepository.findById(report.getReportedUser().getId())
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고 대상 사용자를 찾을 수 없습니다."));

        if (reportedUser.getRole() == Role.ADMIN) {
            throw new GameApiException(HttpStatus.FORBIDDEN, "관리자 계정 상태는 변경할 수 없습니다.");
        }

        reportedUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(reportedUser);

        report.setAdminNote(
                adminNote == null || adminNote.isBlank()
                        ? "운영자에 의해 계정 정지가 해제되었습니다."
                        : adminNote
        );
        reportRepository.save(report);

        return ReportResponseDto.from(report);
    }
}
