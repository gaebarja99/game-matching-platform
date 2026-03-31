import { useEffect, useMemo, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl, resolveProfileImageUrl } from '../api/client';

interface FanItem {
  userId: number;
  nickname?: string;
  loginId?: string;
  profileImageUrl?: string | null;
  donationCount?: number;
  totalAmount?: number;
  lastDonatedAt?: string;
}

function formatDate(value?: string) {
  if (!value) return '-';
  try {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  } catch {
    return value;
  }
}

export default function StudioViewersFans() {
  const [list, setList] = useState<FanItem[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(apiUrl('api/donate/fans'), { credentials: 'include' })
      .then((response) => (response.ok ? response.json() : { list: [] }))
      .then((data: { list?: FanItem[] }) => setList(Array.isArray(data?.list) ? data.list : []))
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
      <h1 className="page-title">팬</h1>
      <div className="viewers-card">
        <div className="summary">내 채널 팬 수 / {list.length}</div>
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
              <th>팬</th>
              <th>후원 횟수</th>
              <th>총 후원 팡</th>
              <th>최근 후원일</th>
              <th>등급</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} className="empty-msg">불러오는 중...</td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-msg">
                  {search.trim() ? '검색 결과가 없습니다.' : '아직 집계된 팬이 없습니다.'}
                </td>
              </tr>
            ) : (
              filtered.map((item, index) => (
                <tr key={item.userId}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{ width: 34, height: 34, borderRadius: '50%', overflow: 'hidden', background: 'var(--studio-bg-card)' }}>
                        {resolveProfileImageUrl(item.profileImageUrl) ? (
                          <img src={resolveProfileImageUrl(item.profileImageUrl)!} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : null}
                      </div>
                      <div>
                        <div>{item.nickname || '-'}</div>
                        <div style={{ color: 'var(--studio-text-dim)', fontSize: '0.82rem' }}>{item.loginId || '-'}</div>
                      </div>
                    </div>
                  </td>
                  <td>{(item.donationCount ?? 0).toLocaleString()}회</td>
                  <td>{(item.totalAmount ?? 0).toLocaleString()} 팡</td>
                  <td>{formatDate(item.lastDonatedAt)}</td>
                  <td>{index === 0 ? 'TOP FAN' : index < 3 ? 'CORE FAN' : 'FAN'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </StudioLayout>
  );
}
