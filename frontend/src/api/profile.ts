export type ProfileDto = {
  userId: number;
  username: string;
  bio: string | null;
  profileImageUrl: string | null;
  bannerImageUrl: string | null;
  preferredGames: string | null;
};

/** 닉네임 검색 목록 행 */
export type ProfileMatchDto = {
  userId: number;
  username: string;
  profileImageUrl: string;
};

export async function fetchProfile(userId: number): Promise<ProfileDto> {
  const res = await fetch(`/api/users/${userId}/profile`);
  if (res.status === 404) {
    throw new Error('NOT_FOUND');
  }
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

export async function fetchProfileByUsername(username: string): Promise<ProfileDto> {
  const params = new URLSearchParams({ username: username.trim() });
  const res = await fetch(`/api/profiles?${params.toString()}`);
  if (res.status === 404) {
    throw new Error('NOT_FOUND');
  }
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

/** 닉네임 부분 일치로 최대 50명 */
export async function fetchProfileMatches(username: string): Promise<ProfileMatchDto[]> {
  const params = new URLSearchParams({ username: username.trim() });
  const res = await fetch(`/api/profiles/matches?${params.toString()}`);
  if (res.status === 400) {
    throw new Error('BAD_REQUEST');
  }
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}

/** 공백 제거 후, 숫자만(1 이상)이면 ID 조회, 아니면 닉네임 조회 */
export async function fetchProfileFlexible(query: string): Promise<ProfileDto> {
  const q = query.trim();
  if (!q) {
    throw new Error('EMPTY');
  }
  if (/^\d+$/.test(q)) {
    const id = parseInt(q, 10);
    if (id >= 1) {
      return fetchProfile(id);
    }
  }
  return fetchProfileByUsername(q);
}

export async function patchProfile(
  userId: number,
  body: {
    username?: string;
    bio: string | null;
    profileImageUrl: string | null;
    bannerImageUrl: string | null;
    preferredGames: string | null;
  }
): Promise<ProfileDto> {
  const res = await fetch(`/api/users/${userId}/profile`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}
