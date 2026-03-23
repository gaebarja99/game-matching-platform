package com.gamematcher.constant.profile;

/**
 * 프로필 이미지가 비어 있을 때 사용하는 정적 리소스 경로
 * ({@code src/main/resources/static/img/profile/default_img.png})
 */
public final class ProfileImageConstants {

    public static final String DEFAULT_PROFILE_IMAGE_URL = "/img/profile/default_img.png";

    private ProfileImageConstants() {
    }

    /** DB에 저장된 값이 없거나 비어 있으면 기본 이미지 URL */
    public static String resolveProfileImageUrl(String stored) {
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        return DEFAULT_PROFILE_IMAGE_URL;
    }
}
