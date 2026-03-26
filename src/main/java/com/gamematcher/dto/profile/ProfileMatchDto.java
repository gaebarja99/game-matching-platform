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
public class ProfileMatchDto {

    private long userId;
    private String username;
    /** 목록용 썸네일(비어 있으면 기본 아바타 URL) */
    private String profileImageUrl;

    public static ProfileMatchDto from(User user, UserProfile profile) {
        ProfileMatchDto dto = new ProfileMatchDto();
        dto.userId = user.getId();
        dto.username = user.getUsername();
        dto.profileImageUrl = ProfileImageConstants.resolveProfileImageUrl(
                profile != null ? profile.getProfileImageUrl() : null);
        return dto;
    }
}
