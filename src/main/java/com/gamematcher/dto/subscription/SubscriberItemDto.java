package com.gamematcher.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberItemDto {
    private Long userId;
    private String nickname;
    private String loginId;
    private String profileImageUrl;
    private LocalDateTime subscribedAt;
}
