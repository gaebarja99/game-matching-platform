package com.gamematcher.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 본인 프로필 조회 시에만 응답에 포함. 각 항목이 타인에게 공개되는지 여부 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileFieldVisibilityDto {

    private boolean bio;
    private boolean bannerImage;
    private boolean profileImage;
    private boolean preferredGames;
}
