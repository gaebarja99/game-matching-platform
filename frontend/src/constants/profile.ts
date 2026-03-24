/** {@link com.gamematcher.constant.profile.ProfileImageConstants} 와 동일 경로 */
export const DEFAULT_PROFILE_IMAGE_URL = '/img/profile/default_img.png';

export function effectiveCustomProfileUrl(stored: string | null | undefined): string {
  if (!stored || stored === DEFAULT_PROFILE_IMAGE_URL) {
    return '';
  }
  return stored;
}
