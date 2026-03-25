import { useState, useEffect } from 'react';
import StudioLayout from '../components/StudioLayout';

const ALERTS_KEY = 'studio-alerts-prefs';
const ALERT_ITEMS: { id: string; label: string; desc: string }[] = [
  { id: 'alert-follow', label: '팔로우 알림', desc: '누군가 나를 팔로우했을 때 알림' },
  { id: 'alert-donation', label: '후원 알림', desc: '시청자가 후원했을 때 알림 및 방송 화면 표시' },
  { id: 'alert-subscribe', label: '구독 알림', desc: '누군가 구독했을 때 알림' },
  { id: 'alert-stream-start', label: '방송 시작 알림', desc: '팔로우한 방송자가 방송을 시작했을 때 알림' },
  { id: 'alert-mention', label: '채팅 멘션 알림', desc: '방송 채팅에서 나를 멘션했을 때 알림' },
];

function loadPrefs(): Record<string, boolean> {
  try {
    const raw = localStorage.getItem(ALERTS_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

function savePrefs(prefs: Record<string, boolean>) {
  try {
    localStorage.setItem(ALERTS_KEY, JSON.stringify(prefs));
  } catch {}
}

export default function StudioAlerts() {
  const [prefs, setPrefs] = useState<Record<string, boolean>>(() => {
    const p = loadPrefs();
    ALERT_ITEMS.forEach(({ id }) => {
      if (p[id] === undefined) p[id] = true;
    });
    return p;
  });

  useEffect(() => {
    savePrefs(prefs);
  }, [prefs]);

  const toggle = (id: string) => {
    setPrefs((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <StudioLayout>
      <div className="alerts-card">
          <h2>알림 설정</h2>
          <p className="card-desc">방송·채널 관련 알림을 켜거나 끌 수 있습니다.</p>
          {ALERT_ITEMS.map(({ id, label, desc }) => (
            <div key={id} className="alert-row">
              <div>
                <div className="label">{label}</div>
                <div className="desc">{desc}</div>
              </div>
              <button
                type="button"
                className={`toggle-switch ${prefs[id] ? 'on' : ''}`}
                role="switch"
                aria-pressed={prefs[id]}
                onClick={() => toggle(id)}
              >
                <span className="knob" />
              </button>
            </div>
          ))}
        </div>
    </StudioLayout>
  );
}
