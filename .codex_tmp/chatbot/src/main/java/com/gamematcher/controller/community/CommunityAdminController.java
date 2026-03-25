package com.gamematcher.controller.community;

import com.gamematcher.constant.ReportStatus;
import com.gamematcher.dto.community.CommunityReportResponseDto;
import com.gamematcher.dto.community.PageResponseDto;
import com.gamematcher.service.community.CommunityAdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/community")
public class CommunityAdminController {

    private final CommunityAdminService communityAdminService;

    public CommunityAdminController(CommunityAdminService communityAdminService) {
        this.communityAdminService = communityAdminService;
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestHeader("X-Admin-User-Id") Long adminUserId,
            @PathVariable Long postId) {
        communityAdminService.deletePostByAdmin(postId, adminUserId);
    }

    @PatchMapping("/posts/{postId}/blind")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blindPost(
            @RequestHeader("X-Admin-User-Id") Long adminUserId,
            @PathVariable Long postId) {
        communityAdminService.blindPost(postId, adminUserId);
    }

    @PatchMapping("/posts/{postId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restorePost(
            @RequestHeader("X-Admin-User-Id") Long adminUserId,
            @PathVariable Long postId) {
        communityAdminService.restorePost(postId, adminUserId);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @RequestHeader("X-Admin-User-Id") Long adminUserId,
            @PathVariable Long commentId) {
        communityAdminService.deleteCommentByAdmin(commentId, adminUserId);
    }

    @GetMapping("/reports")
    public PageResponseDto<CommunityReportResponseDto> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return communityAdminService.getReportListDto(status, page, size);
    }

    @PatchMapping("/reports/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveReport(
            @RequestHeader("X-Admin-User-Id") Long adminUserId,
            @PathVariable Long reportId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String adminNote) {
        communityAdminService.resolveReport(reportId, adminUserId, approve, adminNote);
    }
}
