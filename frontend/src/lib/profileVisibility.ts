import type { ProfileVisibilityDto } from '../api/profile';

export const DEFAULT_PROFILE_VISIBILITY: ProfileVisibilityDto = {
  bio: true,
  bannerImage: true,
  profileImage: true,
  preferredGames: true,
};

/**
 * 자기소개·배너·프로필 이미지는 항상 공개로 간주한다.
 * 선호 게임만 서버 저장값을 반영한다.
 */
export function effectiveVisibility(v: ProfileVisibilityDto | null | undefined): ProfileVisibilityDto {
  return {
    bio: true,
    bannerImage: true,
    profileImage: true,
    preferredGames: v?.preferredGames !== false,
  };
}
