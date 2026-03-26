package com.gamematcher.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponseDto {
    private Long userId;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private Integer level;
    private Long matchCount;
}

