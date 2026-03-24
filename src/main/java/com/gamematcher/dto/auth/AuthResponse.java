package com.gamematcher.dto.auth;

import com.gamematcher.entity.User;
import com.gamematcher.service.LevelService;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuthResponse {

    private Long id;
    private String loginId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String bio;
    /** 가입일 (프로필 활동 기간 표시용) */
    private LocalDateTime createdAt;
    /** 팡 잔액 */
    private Long pangBalance;
    /** 권한 (ADMIN: 운영자) */
    private String role;
    /** 레벨 (1~9999) */
    private Integer level;
    /** 현재 레벨 내 경험치 (0.1 단위). 바 표시용 */
    private Long experienceInCurrentLevelTenths;
    /** 다음 레벨까지 필요한 경험치 (0.1 단위). 바 표시용 */
    private Long experienceRequiredForNextLevelTenths;
    /** 마일리지 (팡 구매 시 10% 적립) */
    private Long mileage;
    /** 광고 제거 만료일시 */
    private LocalDateTime adFreeUntil;

    public static AuthResponse from(User user) {
        long tenths = user.getTotalExperienceTenths() != null ? user.getTotalExperienceTenths() : 0L;
        int level = LevelService.getLevel(tenths);
        return AuthResponse.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .username(user.getUsername())
                .nickname(user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .pangBalance(user.getPangBalance() != null ? user.getPangBalance() : 0L)
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .level(level)
                .experienceInCurrentLevelTenths(LevelService.getExperienceInCurrentLevel(tenths, level))
                .experienceRequiredForNextLevelTenths(LevelService.getExperienceRequiredForNextLevel(level))
                .mileage(user.getMileage() != null ? user.getMileage() : 0L)
                .adFreeUntil(user.getAdFreeUntil())
                .build();
    }
}
