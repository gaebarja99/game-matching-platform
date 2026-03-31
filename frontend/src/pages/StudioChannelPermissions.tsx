import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import StudioLayout from '../components/StudioLayout';
import { apiFetch } from '../api/client';

type PermissionContext = {
  ownerUserId: number;
  ownerNickname: string;
  ownerLoginId: string;
  actingAsManager: boolean;
  canManagePermissions: boolean;
  myUserId: number;
};

type ManagerRow = {
  userId: number;
  nickname: string;
  loginId: string;
  role: string;
  registeredBy: string;
  registeredAt: string;
  removable: boolean;
};

type PermissionResponse = {
  context: PermissionContext;
  list: ManagerRow[];
};

function formatDate(value: string) {
  if (!value) return '-';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  });
}

export default function StudioChannelPermissions() {
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState('채널 관리자');
  const [context, setContext] = useState<PermissionContext | null>(null);
  const [rows, setRows] = useState<ManagerRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const ownerUserIdParam = searchParams.get('ownerUserId');
  const parsedOwnerUserId = ownerUserIdParam ? Number(ownerUserIdParam) : NaN;
  const ownerUserId = Number.isFinite(parsedOwnerUserId) ? parsedOwnerUserId : null;
  const permissionEndpoint = ownerUserId ? `api/studio/channel/permissions?ownerUserId=${ownerUserId}` : 'api/studio/channel/permissions';

  const loadPermissions = async () => {
    setLoading(true);
    setError('');
    const response = await apiFetch<PermissionResponse>(permissionEndpoint);
    if (!response.ok || !response.data) {
      setError(response.message ?? '권한 목록을 불러오지 못했습니다.');
      setRows([]);
      setContext(null);
      setLoading(false);
      return;
    }
    setContext(response.data.context);
    setRows(Array.isArray(response.data.list) ? response.data.list : []);
    setLoading(false);
  };

  useEffect(() => {
    loadPermissions();
  }, [permissionEndpoint]);

  const ownerLabel = useMemo(() => {
    if (!context) return '';
    return `${context.ownerNickname} (${context.ownerLoginId})`;
  }, [context]);

  const handleAdd = async () => {
    const trimmed = keyword.trim();
    if (!trimmed || !context?.canManagePermissions || saving) return;
    setSaving(true);
    setError('');
    setMessage('');
    const response = await apiFetch('api/studio/channel/permissions', {
      method: 'POST',
      body: JSON.stringify({
        keyword: trimmed,
        role,
      }),
    });
    if (!response.ok) {
      setError(response.message ?? '권한 추가에 실패했습니다.');
      setSaving(false);
      return;
    }
    setKeyword('');
    setMessage('권한을 추가했습니다. 대상 사용자에게 사이트 알림도 전송됩니다.');
    await loadPermissions();
    setSaving(false);
  };

  const handleRemove = async (row: ManagerRow) => {
    if (!context?.canManagePermissions || !row.removable || saving) return;
    setSaving(true);
    setError('');
    setMessage('');
    const response = await apiFetch(`api/studio/channel/permissions/${row.userId}`, {
      method: 'DELETE',
    });
    if (!response.ok) {
      setError(response.message ?? '권한 해제에 실패했습니다.');
      setSaving(false);
      return;
    }
    setMessage('권한을 해제했습니다. 대상 사용자에게 사이트 알림이 전송됩니다.');
    await loadPermissions();
    setSaving(false);
  };

  return (
    <StudioLayout>
      <h1 className="page-title">권한 관리</h1>
      <p className="step-desc" style={{ marginTop: -8, marginBottom: 22 }}>
        닉네임 또는 아이디로 실제 회원만 추가할 수 있고, 권한을 받은 사용자는 해당 채널의 관리 화면을 편집할 수 있습니다.
      </p>

      {context?.actingAsManager ? (
        <div className="settings-card" style={{ marginBottom: 20, maxWidth: 920, background: '#ecfdf5', borderColor: 'rgba(0, 230, 118, 0.25)' }}>
          <strong>{context.ownerNickname}</strong>님의 채널을 관리 중입니다. 권한 관리 자체는 채널 소유자만 변경할 수 있습니다.
        </div>
      ) : null}

      <div className="settings-card" style={{ marginBottom: 24, maxWidth: 920 }}>
        <div className="settings-row" style={{ marginBottom: 0 }}>
          <div className="input-row studio-permissions-add-row">
            <input
              type="text"
              className="studio-input"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="닉네임 또는 아이디를 정확히 입력해주세요."
              disabled={!context?.canManagePermissions || saving}
            />
            <select
              className="studio-select"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              disabled={!context?.canManagePermissions || saving}
            >
              <option>채널 관리자</option>
              <option>매니저</option>
              <option>편집자</option>
            </select>
            <button
              type="button"
              className="btn-copy"
              onClick={handleAdd}
              disabled={!context?.canManagePermissions || saving}
            >
              추가
            </button>
          </div>
        </div>
        <ul style={{ margin: '18px 0 0 18px', padding: 0, lineHeight: 1.8, color: 'var(--studio-text)' }}>
          <li>사이트에 실제로 존재하는 회원만 닉네임 또는 아이디로 추가됩니다.</li>
          <li>권한을 받은 사용자는 해당 채널의 채널 관리 화면을 대신 관리할 수 있습니다.</li>
          <li>권한 부여와 해제 시 대상 사용자에게 사이트 알림이 전송됩니다.</li>
        </ul>
        {ownerLabel ? (
          <p className="hint" style={{ marginTop: 12 }}>
            현재 관리 대상 채널: {ownerLabel}
          </p>
        ) : null}
        {error ? <p className="revenue-msg error">{error}</p> : null}
        {message ? <p className="revenue-msg ok">{message}</p> : null}
      </div>

      <div className="settings-card" style={{ maxWidth: 920 }}>
        <h2>권한 목록</h2>
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
            {loading ? (
              <tr>
                <td colSpan={5} className="empty-msg">불러오는 중...</td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-msg">등록된 권한이 없습니다.</td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.userId}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{row.nickname}</div>
                    <div style={{ color: 'var(--studio-text-dim)', fontSize: '0.9rem' }}>{row.loginId}</div>
                  </td>
                  <td>{row.registeredBy}</td>
                  <td>{formatDate(row.registeredAt)}</td>
                  <td>{row.role}</td>
                  <td>
                    {row.removable ? (
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={() => handleRemove(row)}
                        disabled={!context?.canManagePermissions || saving}
                      >
                        권한 해제
                      </button>
                    ) : (
                      '-'
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </StudioLayout>
  );
}
