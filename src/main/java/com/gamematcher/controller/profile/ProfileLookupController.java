package com.gamematcher.controller.profile;

import com.gamematcher.dto.profile.ProfileMatchDto;
import com.gamematcher.dto.profile.ProfilePublicResponseDto;
import com.gamematcher.service.auth.CurrentUserService;
import com.gamematcher.service.profile.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 닉네임 등으로 프로필 조회 ({@code /api/users/{id}/profile} 보조).
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileLookupController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public ProfileLookupController(ProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ProfilePublicResponseDto getByUsername(
            @RequestParam("username") String username,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            HttpSession session) {
        Long viewerId = currentUserService.tryCurrentUserId(authToken, session).orElse(null);
        return profileService.getProfileByUsername(username, viewerId);
    }

    /** 닉네임 부분 일치 목록(최대 50). 상세는 {@code /api/users/{id}/profile} */
    @GetMapping("/matches")
    public List<ProfileMatchDto> searchMatches(@RequestParam("username") String username) {
        return profileService.searchProfilesByUsername(username);
    }
}
