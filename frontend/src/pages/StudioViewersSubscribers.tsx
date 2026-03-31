import { useEffect, useMemo, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl, resolveProfileImageUrl } from '../api/client';

interface SubscriberItem {
  userId: number;
  nickname?: string;
  loginId?: string;
  profileImageUrl?: string | null;
  subscribedAt?: string;
}

function formatDate(value?: string) {
  if (!value) return '-';
  try {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });
  } catch {
    return value;
  }
}

function formatPeriod(value?: string) {
  if (!value) return '-';
  const start = new Date(value);
  if (Number.isNaN(start.getTime())) return '-';
  const diffMs = Date.now() - start.getTime();
  const diffDays = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));
  if (diffDays < 1) return '오늘 시작';
  if (diffDays < 30) return `${diffDays}일`;
  const months = Math.floor(diffDays / 30);
  const days = diffDays % 30;
  return days > 0 ? `${months}개월 ${days}일` : `${months}개월`;
}

export default function StudioViewersSubscribers() {
  const [list, setList] = useState<SubscriberItem[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(apiUrl('api/subscription/subscribers'), { credentials: 'include' })
      .then((response) => (response.ok ? response.json() : { list: [] }))
      .then((data: { list?: SubscriberItem[] }) => setList(Array.isArray(data?.list) ? data.list : []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return list;
    return list.filter((item) =>
      (item.nickname ?? '').toLowerCase().includes(keyword) ||
      (item.loginId ?? '').toLowerCase().includes(keyword),
    );
  }, [list, search]);

  return (
    <StudioLayout>
      <h1 className="page-title">구독자</h1>
      <div className="viewers-card">
        <div className="summary">내 채널 구독자 / {list.length}</div>
        <div className="search-row">
          <input
            type="text"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="닉네임 또는 아이디를 입력해 주세요."
          />
          <button type="button" className="btn-search">검색</button>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>구독자</th>
              <th>아이디</th>
              <th>구독 시작일</th>
              <th>구독 기간</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={4} className="empty-msg">불러오는 중...</td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={4} className="empty-msg">
                  {search.trim() ? '검색 결과가 없습니다.' : '아직 구독자가 없습니다.'}
                </td>
              </tr>
            ) : (
              filtered.map((item) => (
                <tr key={item.userId}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{ width: 34, height: 34, borderRadius: '50%', overflow: 'hidden', background: 'var(--studio-bg-card)' }}>
                        {resolveProfileImageUrl(item.profileImageUrl) ? (
                          <img src={resolveProfileImageUrl(item.profileImageUrl)!} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : null}
                      </div>
                      <div>{item.nickname || '-'}</div>
                    </div>
                  </td>
                  <td>{item.loginId || '-'}</td>
                  <td>{formatDate(item.subscribedAt)}</td>
                  <td>{formatPeriod(item.subscribedAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </StudioLayout>
  );
}
