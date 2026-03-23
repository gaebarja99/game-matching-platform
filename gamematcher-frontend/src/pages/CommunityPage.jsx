import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getCommunityPosts } from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './CommunityPage.module.css'

const CATEGORIES = [
  { id: 'NOTICE', label: '공지' },
  { id: null, label: '전체' },
  { id: 'FREE', label: '자유' },
  { id: 'QUESTION', label: '질문' },
  { id: 'LOL', label: '롤' },
  { id: 'VALORANT', label: '발로란트' },
  { id: 'PUBG', label: '배그' },
  { id: 'OVERWATCH', label: '오버워치' },
  { id: 'CS2', label: 'CS2' },
  { id: 'APEX', label: '에이펙스' },
  { id: 'BLIZZARD', label: '블리자드' },
  { id: 'STEAM', label: '스팀' },
]

const SORT_OPTIONS = [
  { value: 'latest', label: '최신순' },
  { value: 'popular', label: '인기순' },
  { value: 'views', label: '조회순' },
]

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '방금 전'
  if (mins < 60) return `${mins}분 전`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}시간 전`
  const days = Math.floor(hours / 24)
  return `${days}일 전`
}

function CategoryBadge({ category }) {
  const colors = {
    FREE: '#6366f1',
    NOTICE: '#ef4444',
    QUESTION: '#f59e0b',
    LOL: '#c89b3c',
    VALORANT: '#ff4655',
    PUBG: '#6b7280',
    OVERWATCH: '#f99e1a',
    CS2: '#4d9e4d',
    APEX: '#cd3333',
    BLIZZARD: '#2563eb',
    STEAM: '#0f172a',
  }

  return (
    <span
      className={styles.catBadge}
      style={{ '--bc': colors[category] || '#6366f1' }}
    >
      {CATEGORIES.find((item) => item.id === category)?.label || category}
    </span>
  )
}

export default function CommunityPage() {
  const { userId } = useAuth()
  const navigate = useNavigate()
  const [category, setCategory] = useState(null)
  const [sortBy, setSortBy] = useState('latest')
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  const fetchPosts = async () => {
    setLoading(true)
    try {
      const res = await getCommunityPosts(category, {
        page,
        size: 20,
        sortBy,
        keyword: keyword || undefined,
      })
      setData(res)
    } catch (error) {
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setPage(0)
  }, [category, sortBy, keyword])

  useEffect(() => {
    fetchPosts()
  }, [category, sortBy, keyword, page])

  const handleSearch = (event) => {
    event.preventDefault()
    setKeyword(searchInput)
  }

  const posts = data?.content || []
  const totalPages = data?.totalPages || 0

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.title}>커뮤니티</h1>
          <p className={styles.desc}>게임 정보와 질문, 자유 글을 카테고리별로 나눠서 확인해 보세요.</p>
        </div>
        {userId && (
          <button className={styles.writeBtn} onClick={() => navigate('/community/write')}>
            + 글쓰기
          </button>
        )}
      </div>

      <div className={styles.controls}>
        <div className={styles.catTabs}>
          {CATEGORIES.map((item) => (
            <button
              key={item.id ?? 'all'}
              className={`${styles.catTab} ${category === item.id ? styles.catTabActive : ''}`}
              onClick={() => setCategory(item.id)}
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className={styles.rightControls}>
          <form className={styles.searchForm} onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="검색어 입력"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
            <button type="submit">검색</button>
          </form>
          <select
            value={sortBy}
            onChange={(event) => setSortBy(event.target.value)}
            className={styles.sortSelect}
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className={styles.postList}>
        {loading && (
          <div className={styles.loadingState}>
            <span className={styles.spinner} />
          </div>
        )}

        {!loading && posts.length === 0 && (
          <div className={styles.emptyState}>
            <div>🗂</div>
            <p>게시글이 없습니다.</p>
          </div>
        )}

        {!loading && posts.map((post) => (
          <Link
            key={post.id}
            to={`/community/posts/${post.id}`}
            className={`${styles.postItem} ${post.notice ? styles.postNotice : ''}`}
          >
            <div className={styles.postLeft}>
              <CategoryBadge category={post.boardCategory} />
              {post.notice && <span className={styles.noticeBadge}>공지</span>}
              {post.popular && <span className={styles.hotBadge}>HOT</span>}
            </div>

            <div className={styles.postMain}>
              <span className={styles.postTitle}>{post.title}</span>
              {post.hashtags?.length > 0 && (
                <span className={styles.hashtags}>
                  {post.hashtags.slice(0, 3).map((tag) => (
                    <span key={tag} className={styles.hashtag}>#{tag}</span>
                  ))}
                </span>
              )}
            </div>

            <div className={styles.postMeta}>
              <span className={styles.metaAuthor}>{post.authorUsername}</span>
              <span className={styles.metaDot}>·</span>
              <span>{timeAgo(post.createdAt)}</span>
              <span className={styles.metaDot}>·</span>
              <span>조회 {post.viewCount}</span>
              <span className={styles.metaDot}>·</span>
              <span>좋아요 {post.likeCount}</span>
              <span className={styles.metaDot}>·</span>
              <span>댓글 {post.commentCount}</span>
            </div>
          </Link>
        ))}
      </div>

      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            className={styles.pageBtn}
            disabled={page === 0}
            onClick={() => setPage((prev) => prev - 1)}
          >
            이전
          </button>

          {Array.from({ length: Math.min(totalPages, 7) }, (_, index) => {
            const current = Math.max(0, Math.min(page - 3, totalPages - 7)) + index
            return (
              <button
                key={current}
                className={`${styles.pageBtn} ${current === page ? styles.pageBtnActive : ''}`}
                onClick={() => setPage(current)}
              >
                {current + 1}
              </button>
            )
          })}

          <button
            className={styles.pageBtn}
            disabled={page >= totalPages - 1}
            onClick={() => setPage((prev) => prev + 1)}
          >
            다음
          </button>
        </div>
      )}
    </div>
  )
}
