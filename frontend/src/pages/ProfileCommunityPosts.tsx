import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { PostListItem } from '../api/community';
import { BOARD_LABELS, fetchMyCommunityPosts, fetchSavedCommunityPosts } from '../api/community';
import { useAuth } from '../contexts/AuthContext';

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' });
  } catch {
    return iso;
  }
}

interface ProfileCommunityPostsProps {
  mode: 'mine' | 'saved';
}

export default function ProfileCommunityPosts({ mode }: ProfileCommunityPostsProps) {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rows, setRows] = useState<PostListItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const load = async () => {
      if (!user) return;
      setLoading(true);
      const response =
        mode === 'mine'
          ? await fetchMyCommunityPosts(user.id, page, 15)
          : await fetchSavedCommunityPosts(user.id, page, 15);

      if (response.ok && response.data) {
        setRows(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        setRows([]);
        setTotalPages(0);
      }
      setLoading(false);
    };

    void load();
  }, [mode, page, user]);

  return (
    <div className="community-page profile-community-page">
      <div className="community-page-head">
        <h1 className="community-page-title">{mode === 'mine' ? '작성한 글' : '저장한 글'}</h1>
      </div>

      {loading ? (
        <p className="community-muted">불러오는 중입니다.</p>
      ) : rows.length === 0 ? (
        <p className="community-muted">{mode === 'mine' ? '작성한 글이 없습니다.' : '저장한 글이 없습니다.'}</p>
      ) : (
        <ul className="community-post-list">
          {rows.map((post) => (
            <li key={post.id}>
              <Link to={`/community/posts/${post.id}`} className="community-post-row">
                <span className="community-post-cat">{BOARD_LABELS[post.boardCategory]}</span>
                <span className="community-post-title">{post.title}</span>
                <span className="community-post-meta">
                  {post.authorUsername} · 조회 {post.viewCount} · 댓글 {post.commentCount} · {formatTime(post.createdAt)}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="community-pagination">
          <button type="button" disabled={page <= 0} onClick={() => setPage((current) => current - 1)}>
            이전
          </button>
          <span>
            {page + 1} / {totalPages}
          </span>
          <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage((current) => current + 1)}>
            다음
          </button>
        </div>
      )}
    </div>
  );
}
