import { useState, useEffect } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

interface Follower {
  nickname?: string;
  loginId?: string;
  followedAt?: string;
}

export default function StudioViewersFollowers() {
  const [list, setList] = useState<Follower[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(apiUrl('api/follow/followers'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((data) => {
        setList(data.list || []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);

  const filtered = search.trim()
    ? list.filter((f) => (f.nickname?.toLowerCase().includes(search.trim().toLowerCase())) || (f.loginId?.toLowerCase().includes(search.trim().toLowerCase())))
    : list;

  const formatDate = (s: string | undefined) => {
    if (!s) return '-';
    try {
      const d = new Date(s);
      return isNaN(d.getTime()) ? s : d.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
    } catch {
      return '-';
    }
  };
  const daysSince = (s: string | undefined) => {
    if (!s) return '-';
    try {
      const d = new Date(s);
      if (isNaN(d.getTime())) return '-';
      const days = Math.floor((Date.now() - d.getTime()) / (24 * 60 * 60 * 1000));
      if (days < 1) return '오늘';
      if (days === 1) return '1일';
      if (days < 30) return `${days}일`;
      if (days < 365) return `${Math.floor(days / 30)}개월`;
      return `${Math.floor(days / 365)}년`;
    } catch {
      return '-';
    }
  };

  return (
    <StudioLayout>
      <h1 className="page-title">팔로워</h1>
        <div className="viewers-card">
          <div className="summary">내 채널 팔로워 / {list.length}</div>
          <div className="search-row">
            <input type="text" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Q 닉네임을 입력해 주세요" onKeyDown={(e) => e.key === 'Enter' && setSearch((s) => s)} />
            <button type="button" className="btn-search">검색</button>
          </div>
          <table className="data-table">
            <thead>
              <tr><th>닉네임</th><th>팔로우 등록일</th><th>기간</th><th>액션</th></tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={4} className="empty-msg">불러오는 중...</td></tr>
              ) : filtered.length === 0 ? (
                <tr><td colSpan={4} className="empty-msg">{search.trim() ? '검색 결과가 없습니다.' : '아직은 고요합니다.'}</td></tr>
              ) : (
                filtered.map((f, i) => (
                  <tr key={i}>
                    <td>{f.nickname || '-'} {f.loginId && <span style={{ color: 'var(--studio-text-dim)', fontSize: '0.85rem' }}>({f.loginId})</span>}</td>
                    <td>{formatDate(f.followedAt)}</td>
                    <td>{daysSince(f.followedAt)}</td>
                    <td>-</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
    </StudioLayout>
  );
}
