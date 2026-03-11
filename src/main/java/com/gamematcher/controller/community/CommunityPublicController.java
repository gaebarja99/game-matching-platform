package com.gamematcher.controller.community;

import com.gamematcher.constant.community.BoardCategory;
import com.gamematcher.dto.community.*;
import com.gamematcher.service.community.CommunityService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 비로그인 사용자도 접근 가능한 커뮤니티 공개 API
 */
@RestController
@RequestMapping("/api/community")
public class CommunityPublicController {

    private final CommunityService communityService;

    public CommunityPublicController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/boards/{category}/posts")
    public PageResponseDto<PostListResponseDto> getPostList(
            @PathVariable BoardCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        Page<PostListResponseDto> result = communityService.getPostList(category, keyword, page, size, sortBy);
        return PageResponseDto.of(result);
    }

    @GetMapping("/posts")
    public PageResponseDto<PostListResponseDto> searchAllPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        Page<PostListResponseDto> result = communityService.getPostList(null, keyword, page, size, sortBy);
        return PageResponseDto.of(result);
    }

    @GetMapping("/boards/{category}/posts/popular")
    public List<PostListResponseDto> getPopularPosts(
            @PathVariable BoardCategory category,
            @RequestParam(defaultValue = "5") int limit) {
        return communityService.getPopularPosts(category, limit);
    }

    @GetMapping("/posts/popular")
    public List<PostListResponseDto> getAllPopularPosts(
            @RequestParam(defaultValue = "5") int limit) {
        return communityService.getPopularPosts(null, limit);
    }

    @GetMapping("/posts/{postId}")
    public PostDetailResponseDto getPostDetail(@PathVariable Long postId) {
        return communityService.getPostDetail(postId, null);
    }

    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponseDto> getComments(@PathVariable Long postId) {
        return communityService.getComments(postId);
    }
}
