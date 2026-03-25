import type { ProfileVisibilityDto } from '../api/profile';

export const DEFAULT_PROFILE_VISIBILITY: ProfileVisibilityDto = {
  bio: true,
  bannerImage: true,
  profileImage: true,
  preferredGames: true,
};

/** null/undefined 필드는 공개(true)로 간주 */
export function effectiveVisibility(v: ProfileVisibilityDto | null | undefined): ProfileVisibilityDto {
  return {
    bio: v?.bio !== false,
    bannerImage: v?.bannerImage !== false,
    profileImage: v?.profileImage !== false,
    preferredGames: v?.preferredGames !== false,
  };
}
