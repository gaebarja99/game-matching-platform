package com.gamematcher.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    /** 본인 조회 시에만 설정됨 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProfileFieldVisibilityDto visibility;

    /** 본인에게는 저장값 그대로, visibility 포함 */
    public static ProfilePublicResponseDto buildOwner(User user, UserProfile profile) {
        ProfilePublicResponseDto dto = new ProfilePublicResponseDto();
        dto.userId = user.getId();
        dto.username = user.getUsername();
        dto.profileImageUrl = ProfileImageConstants.resolveProfileImageUrl(
                profile != null ? profile.getProfileImageUrl() : null);
        if (profile != null) {
            dto.bio = profile.getBio();
            dto.bannerImageUrl = profile.getBannerImageUrl();
            dto.preferredGames = profile.getPreferredGames();
            dto.visibility = new ProfileFieldVisibilityDto(
                    effectiveVisible(profile.getPublicBioVisible()),
                    effectiveVisible(profile.getPublicBannerVisible()),
                    effectiveVisible(profile.getPublicProfileImageVisible()),
                    effectiveVisible(profile.getPublicPreferredGamesVisible()));
        } else {
            dto.visibility = new ProfileFieldVisibilityDto(true, true, true, true);
        }
        return dto;
    }

    /** 타인 조회: 공개 설정에 따라 필드 마스킹, visibility 없음 */
    public static ProfilePublicResponseDto buildPublic(User user, UserProfile profile) {
        ProfilePublicResponseDto dto = new ProfilePublicResponseDto();
        dto.userId = user.getId();
        dto.username = user.getUsername();
        dto.visibility = null;

        boolean bioOk = profile == null || effectiveVisible(profile.getPublicBioVisible());
        boolean bannerOk = profile == null || effectiveVisible(profile.getPublicBannerVisible());
        boolean imgOk = profile == null || effectiveVisible(profile.getPublicProfileImageVisible());
        boolean gamesOk = profile == null || effectiveVisible(profile.getPublicPreferredGamesVisible());

        if (profile != null) {
            dto.bio = bioOk ? profile.getBio() : null;
            dto.bannerImageUrl = bannerOk ? profile.getBannerImageUrl() : null;
            dto.preferredGames = gamesOk ? profile.getPreferredGames() : null;
        }

        String storedImg = profile != null ? profile.getProfileImageUrl() : null;
        if (imgOk) {
            dto.profileImageUrl = ProfileImageConstants.resolveProfileImageUrl(storedImg);
        } else {
            dto.profileImageUrl = ProfileImageConstants.DEFAULT_PROFILE_IMAGE_URL;
        }
        return dto;
    }

    private static boolean effectiveVisible(Boolean flag) {
        return flag == null || flag;
    }
}
