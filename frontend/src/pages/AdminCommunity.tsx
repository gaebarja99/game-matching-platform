import { useEffect, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { fetchAdminCommunityPosts, updateAdminCommunityPost } from '../api/admin';
import type { AdminCommunityPostRow } from '../api/admin';
import { useAuth } from '../contexts/AuthContext';

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function shortenLoginId(loginId: string) {
  if (!loginId) return '-';
  const socialPrefixes = ['google_', 'naver_', 'kakao_'];
  const matchedPrefix = socialPrefixes.find((prefix) => loginId.startsWith(prefix));
  if (!matchedPrefix) return loginId;
  const rest = loginId.slice(matchedPrefix.length);
  if (rest.length <= 5) return loginId;
  return `${matchedPrefix}${rest.slice(0, 5)}...`;
}

function statusTone(status: string) {
  switch (status) {
    case 'ACTIVE':
      return 'tone-success';
    case 'BLIND':
      return 'tone-warning';
    case 'DELETED_BY_ADMIN':
      return 'tone-danger';
    case 'DELETED_BY_USER':
      return 'tone-muted';
    default:
      return 'tone-neutral';
  }
}

function canRestore(status: string) {
  return status === 'BLIND' || status === 'DELETED_BY_ADMIN' || status === 'DELETED_BY_USER';
}

export default function AdminCommunity() {
  const { user, loading: authLoading } = useAuth();
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('all');
  const [status, setStatus] = useState('all');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<{
    content: AdminCommunityPostRow[];
    totalPages: number;
    first: boolean;
    last: boolean;
  } | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    const response = await fetchAdminCommunityPosts({ query, category, status, page, size: 12 });
    setResult(
      response.ok && response.data
        ? response.data
        : { content: [], totalPages: 0, first: true, last: true },
    );
    setLoading(false);
  };

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    void load();
  }, [user, query, category, status, page]);

  const handleAction = async (postId: number, action: 'blind' | 'restore' | 'delete') => {
    const response = await updateAdminCommunityPost(postId, action);
    if (response.ok) {
      await load();
    } else {
      window.alert(response.error ?? '처리에 실패했습니다.');
    }
  };

  if (authLoading) {
    return (
      <AdminLayout title="커뮤니티 관리" description="관리자 권한과 커뮤니티 게시글을 확인하는 중입니다.">
        <section className="admin-panel">
          <p className="admin-subtext">관리자 화면을 불러오는 중입니다.</p>
        </section>
      </AdminLayout>
    );
  }

  if (!user || user.role !== 'ADMIN') {
    return null;
  }

  return (
    <AdminLayout
      title="커뮤니티 관리"
      description="게시글 검색, 카테고리 필터, 블라인드/복구/삭제를 한 화면에서 처리합니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="제목, 작성자 아이디, 닉네임 검색"
          />
          <select
            className="admin-filter-select"
            value={category}
            onChange={(event) => setCategory(event.target.value)}
          >
            <option value="all">전체 카테고리</option>
            <option value="free">자유</option>
            <option value="notice">공지</option>
            <option value="question">질문</option>
            <option value="lol">롤</option>
            <option value="valorant">발로란트</option>
            <option value="battleground">배그</option>
            <option value="overwatch">오버워치</option>
            <option value="cs2">CS2</option>
            <option value="apex">에이펙스</option>
            <option value="blizzard">블리자드</option>
            <option value="steam">스팀</option>
          </select>
          <select
            className="admin-filter-select"
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="all">전체 상태</option>
            <option value="active">활성</option>
            <option value="blind">블라인드</option>
            <option value="deleted_by_admin">관리자 삭제</option>
            <option value="deleted_by_user">사용자 삭제</option>
          </select>
          <button
            type="button"
            className="admin-action-btn primary"
            onClick={() => {
              setQuery(queryInput.trim());
              setPage(0);
            }}
          >
            검색
          </button>
          <button
            type="button"
            className="admin-action-btn"
            onClick={() => {
              setQueryInput('');
              setQuery('');
              setCategory('all');
              setStatus('all');
              setPage(0);
            }}
          >
            초기화
          </button>
        </div>
      </section>

      <section className="admin-panel admin-table-card">
        <table className="data-table admin-community-table">
          <colgroup>
            <col style={{ width: '34%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '10%' }} />
            <col style={{ width: '11%' }} />
            <col style={{ width: '9%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '10%' }} />
          </colgroup>
          <thead>
            <tr>
              <th>게시글</th>
              <th>작성자</th>
              <th>카테고리</th>
              <th>상태</th>
              <th>추천수</th>
              <th>작성일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="empty-msg">
                  불러오는 중입니다.
                </td>
              </tr>
            ) : !result || result.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-msg">
                  조건에 맞는 게시글이 없습니다.
                </td>
              </tr>
            ) : (
              result.content.map((post) => (
                <tr key={post.id}>
                  <td>
                    <div>{post.title}</div>
                    <div className="admin-subtext">
                      조회 {post.viewCount} · 추천수 {post.likeCount} · 댓글 {post.commentCount} · 신고{' '}
                      {post.reportCount}
                      {post.isNotice ? ' · 공지' : ''}
                    </div>
                  </td>
                  <td>
                    <div>{post.authorName || '-'}</div>
                    <div className="admin-subtext">{shortenLoginId(post.authorLoginId || '-')}</div>
                  </td>
                  <td>{post.categoryLabel}</td>
                  <td>
                    <span className={`admin-status-badge ${statusTone(post.status)}`}>
                      {post.statusLabel}
                    </span>
                  </td>
                  <td>{post.likeCount}</td>
                  <td>{formatDate(post.createdAt)}</td>
                  <td>
                    <div className="admin-inline-actions">
                      {post.status === 'ACTIVE' ? (
                        <button
                          type="button"
                          className="admin-action-btn"
                          onClick={() => void handleAction(post.id, 'blind')}
                        >
                          블라인드
                        </button>
                      ) : canRestore(post.status) ? (
                        <button
                          type="button"
                          className="admin-action-btn"
                          onClick={() => void handleAction(post.id, 'restore')}
                        >
                          복구
                        </button>
                      ) : null}
                      {post.status !== 'DELETED_BY_ADMIN' ? (
                        <button
                          type="button"
                          className="admin-action-btn danger"
                          onClick={() => void handleAction(post.id, 'delete')}
                        >
                          삭제
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="admin-action-btn danger"
                          onClick={() => void handleAction(post.id, 'restore')}
                        >
                          복구
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        <div className="admin-pagination">
          <button
            type="button"
            className="admin-action-btn"
            disabled={result?.first ?? true}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            이전
          </button>
          <span>
            페이지 {page + 1}
            {result?.totalPages ? ` / ${Math.max(result.totalPages, 1)}` : ''}
          </span>
          <button
            type="button"
            className="admin-action-btn"
            disabled={result?.last ?? true}
            onClick={() => setPage((current) => current + 1)}
          >
            다음
          </button>
        </div>
      </section>
    </AdminLayout>
  );
}
