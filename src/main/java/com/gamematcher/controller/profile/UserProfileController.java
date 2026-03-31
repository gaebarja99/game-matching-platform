package com.gamematcher.controller.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gamematcher.dto.profile.ProfilePublicResponseDto;
import com.gamematcher.service.profile.ProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
public class UserProfileController {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    public UserProfileController(ProfileService profileService, ObjectMapper objectMapper) {
        this.profileService = profileService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ProfilePublicResponseDto getProfile(@PathVariable Long userId) {
        return profileService.getProfile(userId);
    }

    @PatchMapping
    public ProfilePublicResponseDto patchProfile(
            @PathVariable Long userId,
            @RequestBody(required = false) ObjectNode body) {
        ObjectNode patch = body != null ? body : objectMapper.createObjectNode();
        return profileService.patchProfile(userId, patch);
    }
}
