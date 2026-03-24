package com.gamematcher.controller.report;

import com.gamematcher.dto.report.*;
import com.gamematcher.service.report.BlockService;
import com.gamematcher.service.report.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/{userId}")
public class ReportBlockController {

    private final ReportService reportService;
    private final BlockService blockService;

    public ReportBlockController(ReportService reportService, BlockService blockService) {
        this.reportService = reportService;
        this.blockService = blockService;
    }

    /** 신고 등록 */
    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponseDto createReport(@PathVariable Long userId, @Valid @RequestBody ReportRequestDto request) {
        return ReportResponseDto.from(reportService.createReport(
                userId,
                request.getReportedUserId(),
                request.getReason(),
                request.getDescription()
        ));
    }

    /** 내가 신고한 목록 조회 */
    @GetMapping("/reports")
    public List<ReportResponseDto> getMyReports(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportService.getMyReports(userId, page, size).stream()
                .map(ReportResponseDto::from)
                .collect(Collectors.toList());
    }

    /** 신고 상세 조회 */
    @GetMapping("/reports/{reportId}")
    public ReportResponseDto getReport(@PathVariable Long userId, @PathVariable Long reportId) {
        return ReportResponseDto.from(reportService.getReport(reportId, userId));
    }

    /** 신고 취소 */
    @DeleteMapping("/reports/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReport(@PathVariable Long userId, @PathVariable Long reportId) {
        reportService.cancelReport(reportId, userId);
    }

    /** 사용자 차단 */
    @PostMapping("/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public BlockedUserResponseDto blockUser(@PathVariable Long userId, @Valid @RequestBody BlockRequestDto request) {
        return BlockedUserResponseDto.from(blockService.blockUser(userId, request.getBlockedUserId()));
    }

    /** 차단 해제 */
    @DeleteMapping("/blocks/{blockedUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblockUser(@PathVariable Long userId, @PathVariable Long blockedUserId) {
        blockService.unblockUser(userId, blockedUserId);
    }

    /** 차단 목록 조회 */
    @GetMapping("/blocks")
    public List<BlockedUserResponseDto> getBlockedList(@PathVariable Long userId) {
        return blockService.getBlockedList(userId).stream()
                .map(BlockedUserResponseDto::from)
                .collect(Collectors.toList());
    }

    /** 특정 사용자 차단 여부 확인 */
    @GetMapping("/blocks/check/{targetUserId}")
    public boolean isBlocked(@PathVariable Long userId, @PathVariable Long targetUserId) {
        return blockService.isBlocked(userId, targetUserId);
    }
}
