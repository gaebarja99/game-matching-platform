import { useEffect, useMemo, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { useAuth } from '../contexts/AuthContext';

const STORAGE_KEY = 'gamematcher-studio-channel-permissions';

interface ManagerRow {
  nickname: string;
  loginId: string;
  role: string;
  registeredBy: string;
  registeredAt: string;
}

function todayLabel() {
  return new Date().toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  });
}

export default function StudioChannelPermissions() {
  const { user } = useAuth();
  const ownerName = user?.nickname ?? user?.username ?? user?.loginId ?? 'JY';
  const ownerLoginId = user?.loginId ?? 'asd8219';
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState('채널 관리자');
  const [rows, setRows] = useState<ManagerRow[]>([]);

  const ownerRow = useMemo<ManagerRow>(
    () => ({
      nickname: ownerName,
      loginId: ownerLoginId,
      role: '소유자',
      registeredBy: ownerName,
      registeredAt: todayLabel(),
    }),
    [ownerLoginId, ownerName],
  );

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as ManagerRow[];
        setRows(Array.isArray(parsed) ? parsed : []);
      }
    } catch {
      /* ignore */
    }
  }, []);

  const handleAdd = () => {
    const trimmed = keyword.trim();
    if (!trimmed) return;

    const nextRows = [
      ...rows,
      {
        nickname: trimmed,
        loginId: `${trimmed.toLowerCase().replace(/\s+/g, '_')}_${rows.length + 1}`,
        role,
        registeredBy: ownerName,
        registeredAt: todayLabel(),
      },
    ];

    setRows(nextRows);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextRows));
    setKeyword('');
  };

  return (
    <StudioLayout>
      <h1 className="page-title">권한 관리</h1>
      <p className="step-desc" style={{ marginTop: -8, marginBottom: 22 }}>
        닉네임 또는 아이디로 채널 관리자를 추가하고, 현재 권한 목록을 관리할 수 있습니다.
      </p>

      <div className="settings-card" style={{ marginBottom: 24, maxWidth: 920 }}>
        <div className="input-row" style={{ alignItems: 'stretch' }}>
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="닉네임 또는 UID를 입력해주세요."
            style={{ flex: 1 }}
          />
          <select value={role} onChange={(e) => setRole(e.target.value)} style={{ minWidth: 150 }}>
            <option>채널 관리자</option>
            <option>매니저</option>
            <option>편집자</option>
          </select>
          <button type="button" className="btn-copy" onClick={handleAdd}>
            추가
          </button>
        </div>
        <ul style={{ margin: '18px 0 0 18px', padding: 0, lineHeight: 1.8, color: 'var(--studio-text)' }}>
          <li>권한을 받은 사용자의 모든 작업 책임은 채널 소유자에게 있습니다.</li>
          <li>권한은 채널 관리 전용 권한으로 동작합니다.</li>
          <li>채널 관리자는 방송 매니저 권한에도 함께 반영됩니다.</li>
        </ul>
      </div>

      <div className="settings-card" style={{ maxWidth: 920 }}>
        <h2>목록 1</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>닉네임</th>
              <th>등록자</th>
              <th>등록일</th>
              <th>역할</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {[ownerRow, ...rows].map((row, index) => (
              <tr key={`${row.loginId}-${index}`}>
                <td>
                  <div style={{ fontWeight: 700 }}>{row.nickname}</div>
                  <div style={{ color: 'var(--studio-text-dim)', fontSize: '0.9rem' }}>{row.loginId}</div>
                </td>
                <td>{row.registeredBy}</td>
                <td>{row.registeredAt}</td>
                <td>{row.role}</td>
                <td>{row.role === '소유자' ? '-' : '관리 중'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </StudioLayout>
  );
}
