package com.gamematcher.repository.common;

import com.gamematcher.constant.ReportStatus;
import com.gamematcher.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporterId(Long reporterId, Pageable pageable);
    List<Report> findByReportedUserId(Long reportedUserId, Pageable pageable);
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId);
    Optional<Report> findByIdAndReporterId(Long id, Long reporterId);
}
