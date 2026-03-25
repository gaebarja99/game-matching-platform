package com.gamematcher.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FollowerItemDto {
    private Long userId;
    private String nickname;
    private String loginId;
    private LocalDateTime followedAt;
}
