package com.gamematcher.controller.report;

import com.gamematcher.constant.ReportStatus;
import com.gamematcher.dto.report.ReportResponseDto;
import com.gamematcher.entity.Report;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.ReportRepository;
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

    private final ReportRepository reportRepository;

    public ReportAdminController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
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
}
