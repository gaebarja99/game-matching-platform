import { useEffect, useMemo, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

interface MyStream {
  id: number;
  externalUrl?: string;
  status?: string;
}

interface BlacklistEntry {
  userId: number;
  displayName?: string;
  loginId?: string;
}

function readSafeMessage(data: unknown, fallback: string) {
  if (!data || typeof data !== 'object' || !('message' in data)) return fallback;
  const message = String((data as { message?: unknown }).message ?? '').trim();
  if (!message) return fallback;
  const looksBroken = /[�?濡釉붾옓由ъ뒪]/.test(message) && !/[가-힣]/.test(message);
  return looksBroken ? fallback : message;
}

export default function StudioViewersBlocklist() {
  const [streamId, setStreamId] = useState<number | null>(null);
  const [input, setInput] = useState('');
  const [search, setSearch] = useState('');
  const [items, setItems] = useState<BlacklistEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetch(apiUrl('api/streams/by-user/me'), { credentials: 'include' })
      .then((response) => (response.ok ? response.json() : []))
      .then((list: MyStream[]) => {
        const streams = Array.isArray(list) ? list : [];
        const liveStream = streams.find((item) => item.status === 'LIVE');
        const obsStream = streams.find((item) => !item.externalUrl);
        const sid = liveStream?.id ?? obsStream?.id ?? streams[0]?.id ?? null;
        setStreamId(sid);
        if (!sid) {
          setLoading(false);
          return;
        }
        return fetch(apiUrl(`api/streams/${sid}/chat/blacklist`), { credentials: 'include' })
          .then((response) => (response.ok ? response.json() : []))
          .then((rows: BlacklistEntry[]) => setItems(Array.isArray(rows) ? rows : []))
          .finally(() => setLoading(false));
      })
      .catch(() => setLoading(false));
  }, []);

  const filteredItems = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return items;
    return items.filter((item) =>
      [item.displayName, item.loginId, String(item.userId)]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword)),
    );
  }, [items, search]);

  const reload = async (sid: number) => {
    const response = await fetch(apiUrl(`api/streams/${sid}/chat/blacklist`), { credentials: 'include' });
    const data = await response.json().catch(() => []);
    setItems(Array.isArray(data) ? data : []);
  };

  const addBlacklist = async () => {
    const value = input.trim();
    if (!streamId || !value || adding) return;
    setAdding(true);
    setMessage('');
    const payload = /^\d+$/.test(value) ? { userId: Number(value) } : { loginId: value };
    try {
      const response = await fetch(apiUrl(`api/streams/${streamId}/chat/blacklist`), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setMessage(readSafeMessage(data, '블랙리스트 사용자를 추가하지 못했습니다.'));
        return;
      }
      setInput('');
      setMessage('블랙리스트에 추가했습니다.');
      await reload(streamId);
    } catch {
      setMessage('블랙리스트 사용자를 추가하지 못했습니다.');
    } finally {
      setAdding(false);
    }
  };

  const removeBlacklist = async (targetUserId: number) => {
    if (!streamId || removingId === targetUserId) return;
    setRemovingId(targetUserId);
    setMessage('');
    try {
      const response = await fetch(apiUrl(`api/streams/${streamId}/chat/blacklist/${targetUserId}`), {
        method: 'DELETE',
        credentials: 'include',
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setMessage(readSafeMessage(data, '블랙리스트 해제에 실패했습니다.'));
        return;
      }
      setMessage('블랙리스트를 해제했습니다.');
      await reload(streamId);
    } catch {
      setMessage('블랙리스트 해제에 실패했습니다.');
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <StudioLayout>
      <h1 className="page-title">블랙리스트</h1>
      <div className="settings-card" style={{ marginBottom: 24 }}>
        <h2>블랙리스트 추가</h2>
        <div className="settings-row">
          <div className="input-row">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="닉네임, 로그인 ID 또는 UID"
              style={{ flex: 1, minWidth: 200 }}
              disabled={!streamId || adding}
            />
            <button type="button" className="btn-copy" onClick={() => void addBlacklist()} disabled={!streamId || adding}>
              {adding ? '추가 중...' : '추가'}
            </button>
          </div>
          <p className="hint" style={{ color: 'var(--studio-accent)', marginTop: 8 }}>
            이 목록에 있는 사용자는 방송에 다시 입장할 수 없습니다.
            <br />
            숫자를 입력하면 UID로 처리되고, 문자를 입력하면 닉네임 또는 로그인 ID로 처리됩니다.
            <br />
            언제든지 이 페이지에서 사용자를 해제할 수 있습니다.
          </p>
          {message && <p className="revenue-msg ok" style={{ marginTop: 10 }}>{message}</p>}
          {!streamId && !loading && <p className="revenue-msg err" style={{ marginTop: 10 }}>관리할 방송을 찾을 수 없습니다.</p>}
        </div>
      </div>

      <div className="settings-card">
        <h2>블랙리스트 목록 {filteredItems.length}</h2>
        <div className="settings-row">
          <div className="input-row">
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="닉네임, 로그인 ID, UID 검색"
              style={{ flex: 1, minWidth: 200 }}
            />
          </div>
        </div>

        <table className="data-table">
          <thead>
            <tr>
              <th>닉네임</th>
              <th>로그인 ID</th>
              <th>UID</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} className="empty-msg">불러오는 중...</td>
              </tr>
            ) : filteredItems.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-msg">블랙리스트 사용자가 없습니다.</td>
              </tr>
            ) : (
              filteredItems.map((item) => (
                <tr key={item.userId}>
                  <td>{item.displayName || '-'}</td>
                  <td>{item.loginId || '-'}</td>
                  <td>{item.userId}</td>
                  <td>차단됨</td>
                  <td>
                    <button
                      type="button"
                      className="btn-manage"
                      onClick={() => void removeBlacklist(item.userId)}
                      disabled={removingId === item.userId}
                    >
                      {removingId === item.userId ? '해제 중...' : '해제'}
                    </button>
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
