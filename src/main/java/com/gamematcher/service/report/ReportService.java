package com.gamematcher.service.report;

import com.gamematcher.constant.ReportStatus;
import com.gamematcher.entity.Report;
import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.ReportRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final CommonUserRepository userRepository;

    public ReportService(ReportRepository reportRepository, CommonUserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Report createReport(Long reporterId, Long reportedUserId, com.gamematcher.constant.ReportReason reason, String description) {
        if (reporterId.equals(reportedUserId)) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "자기 자신은 신고할 수 없습니다.");
        }
        if (reportRepository.existsByReporterIdAndReportedUserId(reporterId, reportedUserId)) {
            throw new GameApiException(HttpStatus.CONFLICT, "이미 해당 사용자를 신고했습니다.");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고자를 찾을 수 없습니다."));
        User reportedUser = userRepository.findById(reportedUserId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "피신고자를 찾을 수 없습니다."));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus(ReportStatus.PENDING);

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<Report> getMyReports(Long reporterId, int page, int size) {
        return reportRepository.findByReporterId(reporterId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Report getReport(Long reportId, Long reporterId) {
        return reportRepository.findByIdAndReporterId(reportId, reporterId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."));
    }

    @Transactional
    public void cancelReport(Long reportId, Long reporterId) {
        Report report = reportRepository.findByIdAndReporterId(reportId, reporterId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "처리 중이거나 완료된 신고는 취소할 수 없습니다.");
        }
        reportRepository.delete(report);
    }
}
