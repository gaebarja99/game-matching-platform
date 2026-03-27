package com.gamematcher.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gamematcher.constant.profile.ProfileImageConstants;
import com.gamematcher.dto.account.AccountConnectionStatusDto;
import com.gamematcher.entity.User;
import com.gamematcher.entity.profile.UserProfile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    /** 연동된 계정 요약. 본인: 연결된 것만·공개 여부 포함. 타인: 공개로 설정된 연동만 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<AccountConnectionStatusDto> connections;

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
                    true,
                    true,
                    true,
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

        boolean gamesOk = profile == null || effectiveVisible(profile.getPublicPreferredGamesVisible());

        if (profile != null) {
            dto.bio = profile.getBio();
            dto.bannerImageUrl = profile.getBannerImageUrl();
            dto.preferredGames = gamesOk ? profile.getPreferredGames() : null;
        }

        String storedImg = profile != null ? profile.getProfileImageUrl() : null;
        dto.profileImageUrl = ProfileImageConstants.resolveProfileImageUrl(storedImg);
        return dto;
    }

    private static boolean effectiveVisible(Boolean flag) {
        return flag == null || flag;
    }
}
