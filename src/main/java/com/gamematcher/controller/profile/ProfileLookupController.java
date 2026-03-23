package com.gamematcher.controller.profile;

import com.gamematcher.dto.profile.ProfileMatchDto;
import com.gamematcher.dto.profile.ProfilePublicResponseDto;
import com.gamematcher.service.profile.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
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

    public ProfileLookupController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfilePublicResponseDto getByUsername(@RequestParam("username") String username) {
        return profileService.getProfileByUsername(username);
    }

    /** 닉네임 부분 일치 목록(최대 50). 상세는 {@code /api/users/{id}/profile} */
    @GetMapping("/matches")
    public List<ProfileMatchDto> searchMatches(@RequestParam("username") String username) {
        return profileService.searchProfilesByUsername(username);
    }
}
