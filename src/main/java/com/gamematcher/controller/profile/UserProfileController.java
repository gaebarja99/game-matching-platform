package com.gamematcher.controller.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gamematcher.dto.profile.ProfilePublicResponseDto;
import com.gamematcher.service.auth.CurrentUserService;
import com.gamematcher.service.profile.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
public class UserProfileController {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    public UserProfileController(
            ProfileService profileService,
            ObjectMapper objectMapper,
            CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ProfilePublicResponseDto getProfile(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            HttpSession session) {
        Long viewerId = currentUserService.tryCurrentUserId(authToken, session).orElse(null);
        return profileService.getProfile(userId, viewerId);
    }

    @PatchMapping
    public ProfilePublicResponseDto patchProfile(
            @PathVariable Long userId,
            @RequestBody(required = false) ObjectNode body) {
        ObjectNode patch = body != null ? body : objectMapper.createObjectNode();
        return profileService.patchProfile(userId, patch);
    }
}
