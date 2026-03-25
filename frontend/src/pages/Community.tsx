import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import {
  BOARD_CATEGORIES,
  BOARD_LABELS,
  type BoardCategory,
  type PostListItem,
  fetchCommunityPostList,
  fetchPopularPosts,
} from '../api/community';

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' });
  } catch {
    return iso;
  }
}

export default function Community() {
  const [searchParams, setSearchParams] = useSearchParams();
  const catParam = searchParams.get('board');
  const category: BoardCategory | 'ALL' =
    catParam && (BOARD_CATEGORIES as string[]).includes(catParam) ? (catParam as BoardCategory) : 'ALL';

  const [keyword, setKeyword] = useState(searchParams.get('q') ?? '');
  const page = Number(searchParams.get('page')) || 0;
  const sortBy = searchParams.get('sort') || 'latest';

  const [rows, setRows] = useState<PostListItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [popular, setPopular] = useState<PostListItem[]>([]);

  const apiCategory = category === 'ALL' ? null : category;

  const loadList = useCallback(async () => {
    setLoading(true);
    const { ok, data } = await fetchCommunityPostList({
      category: apiCategory,
      keyword: keyword.trim() || undefined,
      page,
      size: 20,
      sortBy,
    });

    if (ok && data) {
      setRows(data.content);
      setTotalPages(data.totalPages);
    } else {
      setRows([]);
      setTotalPages(0);
    }

    setLoading(false);
  }, [apiCategory, keyword, page, sortBy]);

  useEffect(() => {
    void loadList();
  }, [loadList]);

  useEffect(() => {
    (async () => {
      const { ok, data } = await fetchPopularPosts(apiCategory, 5);
      if (ok && Array.isArray(data)) {
        setPopular(data);
      } else {
        setPopular([]);
      }
    })();
  }, [apiCategory]);

  const setBoard = (next: BoardCategory | 'ALL') => {
    const params = new URLSearchParams(searchParams);
    if (next === 'ALL') params.delete('board');
    else params.set('board', next);
    params.delete('page');
    setSearchParams(params);
  };

  const onSearch = (event: React.FormEvent) => {
    event.preventDefault();
    const params = new URLSearchParams(searchParams);
    if (keyword.trim()) params.set('q', keyword.trim());
    else params.delete('q');
    params.delete('page');
    setSearchParams(params);
  };

  const goPage = (nextPage: number) => {
    const params = new URLSearchParams(searchParams);
    if (nextPage <= 0) params.delete('page');
    else params.set('page', String(nextPage));
    setSearchParams(params);
  };

  return (
    <Layout>
      <div className="community-page">
        <div className="community-page-head">
          <h1 className="community-page-title">커뮤니티</h1>
          <div className="community-page-actions">
            <form className="community-search" onSubmit={onSearch}>
              <input
                type="search"
                placeholder="제목·내용 검색"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                aria-label="게시글 검색"
              />
              <button type="submit">검색</button>
            </form>
            <Link to="/community/write" className="community-btn-primary">
              글쓰기
            </Link>
          </div>
        </div>

        <div className="game-tabs community-board-tabs">
          <button type="button" className={category === 'ALL' ? 'active' : ''} onClick={() => setBoard('ALL')}>
            전체
          </button>
          {BOARD_CATEGORIES.map((board) => (
            <button
              key={board}
              type="button"
              className={category === board ? 'active' : ''}
              onClick={() => setBoard(board)}
            >
              {BOARD_LABELS[board]}
            </button>
          ))}
        </div>

        <div className="community-layout">
          <section className="community-main">
            {loading ? (
              <p className="community-muted">불러오는 중입니다.</p>
            ) : rows.length === 0 ? (
              <p className="community-muted">게시글이 없습니다.</p>
            ) : (
              <ul className="community-post-list">
                {rows.map((post) => (
                  <li key={post.id}>
                    <Link to={`/community/posts/${post.id}`} className="community-post-row">
                      <span className="community-post-cat">{BOARD_LABELS[post.boardCategory]}</span>
                      <span className="community-post-title">
                        {post.isNotice ? <span className="community-badge-notice">공지</span> : null}
                        {post.isPopular ? <span className="community-badge-hot">인기</span> : null}
                        {post.title}
                      </span>
                      <span className="community-post-meta">
                        {post.authorUsername} · 조회 {post.viewCount} · 댓글 {post.commentCount} · 추천{' '}
                        {post.recommendCount} · {formatTime(post.createdAt)}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}

            {totalPages > 1 ? (
              <div className="community-pagination">
                <button type="button" disabled={page <= 0} onClick={() => goPage(page - 1)}>
                  이전
                </button>
                <span>
                  {page + 1} / {totalPages}
                </span>
                <button type="button" disabled={page >= totalPages - 1} onClick={() => goPage(page + 1)}>
                  다음
                </button>
              </div>
            ) : null}
          </section>

          <aside className="community-aside">
            <h2 className="community-aside-title">인기글</h2>
            {popular.length === 0 ? (
              <p className="community-muted">인기글이 없습니다.</p>
            ) : (
              <ol className="community-popular-list">
                {popular.map((post) => (
                  <li key={post.id}>
                    <Link to={`/community/posts/${post.id}`}>{post.title}</Link>
                    <span className="community-popular-sub">
                      {BOARD_LABELS[post.boardCategory]} · 추천 {post.recommendCount}
                    </span>
                  </li>
                ))}
              </ol>
            )}
          </aside>
        </div>
      </div>
    </Layout>
  );
}