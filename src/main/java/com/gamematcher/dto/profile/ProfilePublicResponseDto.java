package com.gamematcher.dto.profile;

import com.gamematcher.constant.profile.ProfileImageConstants;
import com.gamematcher.entity.User;
import com.gamematcher.entity.profile.UserProfile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfilePublicResponseDto {

    private Long userId;
    private String username;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private String preferredGames;

    public static ProfilePublicResponseDto from(User user, UserProfile profile) {
        ProfilePublicResponseDto dto = new ProfilePublicResponseDto();
        dto.userId = user.getId();
        dto.username = user.getUsername();
        dto.profileImageUrl = ProfileImageConstants.resolveProfileImageUrl(
                profile != null ? profile.getProfileImageUrl() : null);
        if (profile != null) {
            dto.bio = profile.getBio();
            dto.bannerImageUrl = profile.getBannerImageUrl();
            dto.preferredGames = profile.getPreferredGames();
        }
        return dto;
    }
}
