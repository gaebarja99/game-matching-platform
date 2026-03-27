import { useEffect, useMemo, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';

const STORAGE_KEY = 'gamematcher-studio-channel-manage';

interface ChannelManageState {
  nickname: string;
  bio: string;
  socialLinks: string[];
  cafeEnabled: boolean;
  sponsorRankingVisible: boolean;
  missionVisible: boolean;
}

export default function StudioChannelManage() {
  const { user } = useAuth();
  const [socialInput, setSocialInput] = useState('');
  const [savedMessage, setSavedMessage] = useState('');

  const defaultState = useMemo<ChannelManageState>(
    () => ({
      nickname: user?.nickname ?? user?.username ?? user?.loginId ?? '',
      bio: '안녕하세요!',
      socialLinks: [],
      cafeEnabled: false,
      sponsorRankingVisible: false,
      missionVisible: false,
    }),
    [user],
  );

  const [form, setForm] = useState<ChannelManageState>(defaultState);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<ChannelManageState>;
        setForm({
          ...defaultState,
          ...parsed,
          nickname: parsed.nickname ?? defaultState.nickname,
          bio: parsed.bio ?? defaultState.bio,
          socialLinks: Array.isArray(parsed.socialLinks) ? parsed.socialLinks : defaultState.socialLinks,
        });
        return;
      }
    } catch {
      /* ignore */
    }

    setForm(defaultState);
  }, [defaultState]);

  const setField = <K extends keyof ChannelManageState>(key: K, value: ChannelManageState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setSavedMessage('');
  };

  const handleAddLink = () => {
    const trimmed = socialInput.trim();
    if (!trimmed || form.socialLinks.length >= 5) return;
    setField('socialLinks', [...form.socialLinks, trimmed]);
    setSocialInput('');
  };

  const handleSave = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(form));
    setSavedMessage('채널 정보가 저장되었습니다.');
  };

  return (
    <StudioLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 24, marginBottom: 20 }}>
        <div>
          <h1 className="page-title">채널 관리</h1>
          <p className="step-desc" style={{ marginTop: -8 }}>
            채널 프로필과 노출 정보를 치지직 예시처럼 한 곳에서 관리할 수 있습니다.
          </p>
        </div>
        <button type="button" className="btn-copy" onClick={handleSave}>
          저장
        </button>
      </div>

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
              {resolveProfileImageUrl(user?.profileImageUrl) ? (
                <img
                  src={resolveProfileImageUrl(user?.profileImageUrl)!}
                  alt=""
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              ) : null}
            </div>
            <button type="button" className="btn-secondary">
              이미지 수정
            </button>
          </div>
        </div>

        <div className="settings-row" style={{ display: 'flex', alignItems: 'flex-start', gap: 28 }}>
          <label style={{ width: 170, fontWeight: 700 }}>닉네임</label>
          <div style={{ flex: 1 }}>
            <input
              type="text"
              value={form.nickname}
              maxLength={30}
              onChange={(e) => setField('nickname', e.target.value)}
              style={{ width: '100%' }}
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
              value={form.bio}
              maxLength={500}
              onChange={(e) => setField('bio', e.target.value)}
              style={{
                width: '100%',
                minHeight: 140,
                padding: 14,
                borderRadius: 16,
                border: '1px solid var(--studio-border-strong)',
                background: 'var(--studio-bg-card)',
                color: 'var(--studio-text)',
                fontFamily: 'inherit',
                resize: 'vertical',
              }}
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
                value={socialInput}
                onChange={(e) => setSocialInput(e.target.value)}
                placeholder="https://"
                style={{ flex: 1 }}
              />
              <button type="button" className="btn-copy" onClick={handleAddLink}>
                링크 추가
              </button>
            </div>
            <p className="hint">내 채널에 소셜 링크를 최대 5개까지 등록할 수 있습니다.</p>
            {form.socialLinks.length > 0 ? (
              <ul style={{ margin: '10px 0 0', paddingLeft: 18 }}>
                {form.socialLinks.map((link, index) => (
                  <li key={`${link}-${index}`} style={{ marginBottom: 6 }}>
                    {link}
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        </div>

        <div className="settings-row studio-toggle-row">
          <label>카페 연결</label>
          <div className="studio-toggle-options">
            <label><input type="radio" checked={!form.cafeEnabled} onChange={() => setField('cafeEnabled', false)} /> OFF</label>
            <label><input type="radio" checked={form.cafeEnabled} onChange={() => setField('cafeEnabled', true)} /> ON</label>
          </div>
        </div>

        <div className="settings-row studio-toggle-row">
          <label>후원 랭킹 노출</label>
          <div className="studio-toggle-options">
            <label><input type="radio" checked={!form.sponsorRankingVisible} onChange={() => setField('sponsorRankingVisible', false)} /> 비노출</label>
            <label><input type="radio" checked={form.sponsorRankingVisible} onChange={() => setField('sponsorRankingVisible', true)} /> 노출</label>
          </div>
        </div>

        <div className="settings-row studio-toggle-row" style={{ marginBottom: 0 }}>
          <label>미션 후원 목록 노출</label>
          <div className="studio-toggle-options">
            <label><input type="radio" checked={!form.missionVisible} onChange={() => setField('missionVisible', false)} /> 비노출</label>
            <label><input type="radio" checked={form.missionVisible} onChange={() => setField('missionVisible', true)} /> 노출</label>
          </div>
        </div>

        {savedMessage ? <p className="revenue-msg ok">{savedMessage}</p> : null}
      </div>
    </StudioLayout>
  );
}
