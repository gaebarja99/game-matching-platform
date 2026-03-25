package com.gamematcher.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FollowingItemDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
}
