package com.gamematcher.dto.recruit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitPostDto {
    private Long id;
    private Long userId;
    private String game;
    private String summonerName;
    private String mainPosition;
    private String findPosition;
    private String tier;
    private String region;
    private String mode;
    private String memo;
    private LocalDateTime createdAt;
}
