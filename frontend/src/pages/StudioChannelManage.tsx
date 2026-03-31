import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import StudioLayout from '../components/StudioLayout';
import { apiFetch, resolveProfileImageUrl } from '../api/client';

type ChannelManageResponse = {
  ownerUserId: number;
  ownerNickname: string;
  ownerLoginId: string;
  actingAsManager: boolean;
  canManagePermissions: boolean;
  myUserId: number;
  nickname: string;
  bio: string;
  profileImageUrl?: string | null;
  socialLinks: string[];
  cafeEnabled: boolean;
  sponsorRankingVisible: boolean;
  missionVisible: boolean;
};

type ChannelManageState = {
  nickname: string;
  bio: string;
  socialLinks: string[];
  cafeEnabled: boolean;
  sponsorRankingVisible: boolean;
  missionVisible: boolean;
};

export default function StudioChannelManage() {
  const [searchParams] = useSearchParams();
  const [socialInput, setSocialInput] = useState('');
  const [savedMessage, setSavedMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [context, setContext] = useState<ChannelManageResponse | null>(null);
  const [profileImageUrl, setProfileImageUrl] = useState<string | null>(null);
  const [form, setForm] = useState<ChannelManageState>({
    nickname: '',
    bio: '',
    socialLinks: [],
    cafeEnabled: false,
    sponsorRankingVisible: false,
    missionVisible: false,
  });
  const ownerUserIdParam = searchParams.get('ownerUserId');
  const parsedOwnerUserId = ownerUserIdParam ? Number(ownerUserIdParam) : NaN;
  const ownerUserId = Number.isFinite(parsedOwnerUserId) ? parsedOwnerUserId : null;
  const manageEndpoint = ownerUserId ? `api/studio/channel/manage?ownerUserId=${ownerUserId}` : 'api/studio/channel/manage';

  const loadManage = async () => {
    setLoading(true);
    setError('');
    const response = await apiFetch<ChannelManageResponse>(manageEndpoint);
    if (!response.ok || !response.data) {
      setError(response.message ?? '채널 정보를 불러오지 못했습니다.');
      setLoading(false);
      return;
    }

    setContext(response.data);
    setProfileImageUrl(response.data.profileImageUrl ?? null);
    setForm({
      nickname: response.data.nickname ?? '',
      bio: response.data.bio ?? '',
      socialLinks: Array.isArray(response.data.socialLinks) ? response.data.socialLinks : [],
      cafeEnabled: Boolean(response.data.cafeEnabled),
      sponsorRankingVisible: Boolean(response.data.sponsorRankingVisible),
      missionVisible: Boolean(response.data.missionVisible),
    });
    setLoading(false);
  };

  useEffect(() => {
    loadManage();
  }, [manageEndpoint]);

  const setField = <K extends keyof ChannelManageState>(key: K, value: ChannelManageState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setSavedMessage('');
    setError('');
  };

  const handleAddLink = () => {
    const trimmed = socialInput.trim();
    if (!trimmed || form.socialLinks.length >= 5) return;
    setField('socialLinks', [...form.socialLinks, trimmed]);
    setSocialInput('');
  };

  const handleRemoveLink = (index: number) => {
    setField(
      'socialLinks',
      form.socialLinks.filter((_, currentIndex) => currentIndex !== index),
    );
  };

  const handleSave = async () => {
    if (saving) return;
    setSaving(true);
    setSavedMessage('');
    setError('');
    const response = await apiFetch<ChannelManageResponse>('api/studio/channel/manage', {
      method: 'PUT',
      body: JSON.stringify({
        ...form,
        ownerUserId,
      }),
    });
    if (!response.ok || !response.data) {
      setError(response.message ?? '채널 정보 저장에 실패했습니다.');
      setSaving(false);
      return;
    }
    setContext(response.data);
    setProfileImageUrl(response.data.profileImageUrl ?? null);
    setForm({
      nickname: response.data.nickname ?? '',
      bio: response.data.bio ?? '',
      socialLinks: Array.isArray(response.data.socialLinks) ? response.data.socialLinks : [],
      cafeEnabled: Boolean(response.data.cafeEnabled),
      sponsorRankingVisible: Boolean(response.data.sponsorRankingVisible),
      missionVisible: Boolean(response.data.missionVisible),
    });
    setSavedMessage(
      response.data.actingAsManager
        ? `${response.data.ownerNickname}님의 채널 정보가 저장되었습니다.`
        : '채널 정보가 저장되었습니다.',
    );
    setSaving(false);
  };

  return (
    <StudioLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 24, marginBottom: 20 }}>
        <div>
          <h1 className="page-title">채널 관리</h1>
          <p className="step-desc" style={{ marginTop: -8 }}>
            채널 프로필과 노출 정보를 한 곳에서 관리할 수 있습니다.
          </p>
        </div>
        <button type="button" className="btn-copy" onClick={handleSave} disabled={saving || loading}>
          저장
        </button>
      </div>

      {context?.actingAsManager ? (
        <div className="settings-card" style={{ marginBottom: 20, maxWidth: 1080, background: '#ecfdf5', borderColor: 'rgba(0, 230, 118, 0.25)' }}>
          <strong>{context.ownerNickname}</strong>님의 채널을 대신 관리 중입니다. 저장하면 해당 채널 정보에 바로 반영됩니다.
        </div>
      ) : null}

      <div className="settings-card" style={{ marginBottom: 24, maxWidth: 1080 }}>
        <h2>기본 정보</h2>

        <div className="settings-row" style={{ display: 'flex', alignItems: 'flex-start', gap: 28 }}>
          <label style={{ width: 170, fontWeight: 700 }}>프로필 이미지</label>
          <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
            <div
              style={{
                width: 128,
                height: 128,
                borderRadius: '50%',
                background: '#eceef3',
                overflow: 'hidden',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {resolveProfileImageUrl(profileImageUrl) ? (
                <img
                  src={resolveProfileImageUrl(profileImageUrl)!}
                  alt=""
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              ) : null}
            </div>
            <button type="button" className="btn-secondary" disabled>
              이미지 수정 준비 중
            </button>
          </div>
        </div>

        <div className="settings-row" style={{ display: 'flex', alignItems: 'flex-start', gap: 28 }}>
          <label style={{ width: 170, fontWeight: 700 }}>닉네임</label>
          <div style={{ flex: 1 }}>
            <input
              type="text"
              className="studio-input studio-input-full"
              value={form.nickname}
              maxLength={30}
              onChange={(e) => setField('nickname', e.target.value)}
              disabled={loading || saving}
            />
            <div className="hint" style={{ textAlign: 'right' }}>
              {form.nickname.length}/30
            </div>
          </div>
        </div>

        <div className="settings-row" style={{ display: 'flex', alignItems: 'flex-start', gap: 28 }}>
          <label style={{ width: 170, fontWeight: 700 }}>채널 소개</label>
          <div style={{ flex: 1 }}>
            <textarea
              className="studio-textarea"
              value={form.bio}
              maxLength={500}
              onChange={(e) => setField('bio', e.target.value)}
              disabled={loading || saving}
            />
            <div className="hint" style={{ textAlign: 'right' }}>
              {form.bio.length}/500
            </div>
          </div>
        </div>
      </div>

      <div className="settings-card" style={{ maxWidth: 1080 }}>
        <h2>채널 정보</h2>

        <div className="settings-row" style={{ display: 'flex', alignItems: 'flex-start', gap: 28 }}>
          <label style={{ width: 170, fontWeight: 700 }}>소셜 링크</label>
          <div style={{ flex: 1 }}>
            <div className="input-row">
              <input
                type="url"
                className="studio-input"
                value={socialInput}
                onChange={(e) => setSocialInput(e.target.value)}
                placeholder="https://"
                disabled={loading || saving}
              />
              <button type="button" className="btn-copy" onClick={handleAddLink} disabled={loading || saving}>
                링크 추가
              </button>
            </div>
            <p className="hint">채널에 표시할 소셜 링크를 최대 5개까지 등록할 수 있습니다.</p>
            {form.socialLinks.length > 0 ? (
              <ul style={{ margin: '10px 0 0', paddingLeft: 18 }}>
                {form.socialLinks.map((link, index) => (
                  <li key={`${link}-${index}`} style={{ marginBottom: 6, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ flex: 1, wordBreak: 'break-all' }}>{link}</span>
                    <button type="button" className="btn-secondary" onClick={() => handleRemoveLink(index)} disabled={loading || saving}>
                      제거
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        </div>

        {loading ? <p className="hint">채널 정보를 불러오는 중입니다...</p> : null}
        {error ? <p className="revenue-msg error">{error}</p> : null}
        {savedMessage ? <p className="revenue-msg ok">{savedMessage}</p> : null}
      </div>
    </StudioLayout>
  );
}
