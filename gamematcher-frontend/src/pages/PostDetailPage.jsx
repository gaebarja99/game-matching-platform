import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createComment,
  deleteComment,
  deletePost,
  getPostComments,
  getPostDetail,
  recommendPost,
  toggleBookmark,
  toggleLike,
} from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './PostDetailPage.module.css'

const CATEGORY_LABELS = {
  FREE: '자유',
  NOTICE: '공지',
  QUESTION: '질문',
  LOL: '롤',
  VALORANT: '발로란트',
  PUBG: '배그',
  OVERWATCH: '오버워치',
  CS2: 'CS2',
  APEX: '에이펙스',
  BLIZZARD: '블리자드',
  STEAM: '스팀',
}

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

function CommentItem({ comment, userId, onDelete }) {
  return (
    <div className={`${styles.comment} ${comment.parentId ? styles.reply : ''}`}>
      <div className={styles.commentHeader}>
        <span className={styles.commentAuthor}>{comment.authorUsername}</span>
        <span className={styles.commentTime}>{timeAgo(comment.createdAt)}</span>
        {userId === comment.authorId && !comment.deleted && (
          <button className={styles.deleteBtn} onClick={() => onDelete(comment.id)}>
            삭제
          </button>
        )}
      </div>
      <div className={`${styles.commentBody} ${comment.deleted ? styles.deletedText : ''}`}>
        {comment.content}
      </div>
      {comment.replies?.length > 0 && (
        <div className={styles.replies}>
          {comment.replies.map((reply) => (
            <CommentItem key={reply.id} comment={reply} userId={userId} onDelete={onDelete} />
          ))}
        </div>
      )}
    </div>
  )
}

export default function PostDetailPage() {
  const { postId } = useParams()
  const navigate = useNavigate()
  const { userId, user } = useAuth()

  const [post, setPost] = useState(null)
  const [comments, setComments] = useState([])
  const [loading, setLoading] = useState(true)
  const [commentText, setCommentText] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [actioning, setActioning] = useState(false)
  const canManagePost = userId === post?.authorId || user?.role === 'ADMIN'

  const loadPost = async () => {
    try {
      const [detail, commentList] = await Promise.all([
        getPostDetail(postId),
        getPostComments(postId),
      ])
      setPost(detail)
      setComments(commentList)
    } catch (error) {
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPost()
  }, [postId])

  const handleLike = async () => {
    if (!userId || actioning) return
    setActioning(true)
    try {
      await toggleLike(userId, postId)
      await loadPost()
    } finally {
      setActioning(false)
    }
  }

  const handleBookmark = async () => {
    if (!userId || actioning) return
    setActioning(true)
    try {
      await toggleBookmark(userId, postId)
      await loadPost()
    } finally {
      setActioning(false)
    }
  }

  const handleRecommend = async (type) => {
    if (!userId || actioning) return
    setActioning(true)
    try {
      await recommendPost(userId, postId, type)
      await loadPost()
    } finally {
      setActioning(false)
    }
  }

  const handleCommentSubmit = async (event) => {
    event.preventDefault()
    if (!userId || !commentText.trim() || submitting) return

    setSubmitting(true)
    try {
      await createComment(userId, postId, { content: commentText.trim() })
      setCommentText('')
      const latestComments = await getPostComments(postId)
      setComments(latestComments)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDeleteComment = async (commentId) => {
    if (!userId) return
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return

    try {
      await deleteComment(userId, commentId)
      const latestComments = await getPostComments(postId)
      setComments(latestComments)
    } catch (error) {
      console.error(error)
    }
  }

  const handleDeletePost = async () => {
    if (!userId) return
    if (!window.confirm('게시글을 삭제하시겠습니까?')) return

    try {
      await deletePost(userId, postId)
      navigate('/community')
    } catch (error) {
      console.error(error)
    }
  }

  if (loading) {
    return (
      <div className={styles.loadingWrap}>
        <span className={styles.spinner} />
      </div>
    )
  }

  if (!post) {
    return (
      <div className={styles.notFound}>
        <div>🔎</div>
        <p>게시글을 찾을 수 없습니다.</p>
        <Link to="/community" className={styles.backLink}>목록으로</Link>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <div className={styles.breadcrumb}>
        <Link to="/community" className={styles.breadLink}>커뮤니티</Link>
        <span className={styles.breadSep}>/</span>
        <span>{CATEGORY_LABELS[post.boardCategory] || post.boardCategory}</span>
      </div>

      <article className={styles.postCard}>
        <div className={styles.postHeader}>
          <div className={styles.postMeta}>
            <span className={styles.catBadge}>{CATEGORY_LABELS[post.boardCategory] || post.boardCategory}</span>
            {post.notice && <span className={styles.noticeBadge}>공지</span>}
          </div>
          <h1 className={styles.postTitle}>{post.title}</h1>
          <div className={styles.postInfo}>
            <span className={styles.author}>{post.authorUsername}</span>
            <span className={styles.dot}>·</span>
            <span className={styles.time}>{timeAgo(post.createdAt)}</span>
            <span className={styles.dot}>·</span>
            <span>조회 {post.viewCount}</span>
            <span className={styles.dot}>·</span>
            <span>댓글 {post.commentCount}</span>
            {post.hashtags?.length > 0 && (
              <>
                <span className={styles.dot}>·</span>
                <span className={styles.hashtags}>
                  {post.hashtags.map((tag) => (
                    <span key={tag} className={styles.hashtag}>#{tag}</span>
                  ))}
                </span>
              </>
            )}
            {canManagePost && (
              <div className={styles.ownerActions}>
                <Link to={`/community/write?edit=${post.id}`} className={styles.editBtn}>수정</Link>
                <button className={styles.deletePostBtn} onClick={handleDeletePost}>
                  {user?.role === 'ADMIN' && userId !== post.authorId ? '관리자 삭제' : '삭제'}
                </button>
              </div>
            )}
          </div>
        </div>

        <div className={styles.postContent}>{post.content}</div>

        {post.attachments?.length > 0 && (
          <div className={styles.attachments}>
            <div className={styles.attachTitle}>첨부 파일</div>
            {post.attachments.map((attachment) => (
              <a
                key={attachment.id}
                href={attachment.url}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.attachItem}
              >
                📎 {attachment.fileName}
              </a>
            ))}
          </div>
        )}

        <div className={styles.actions}>
          <button
            className={`${styles.actionBtn} ${post.liked ? styles.actionActive : ''}`}
            onClick={handleLike}
            disabled={!userId}
            title={userId ? '좋아요' : '로그인이 필요합니다.'}
          >
            ❤️ {post.likeCount}
          </button>

          <button
            className={`${styles.actionBtn} ${post.bookmarked ? styles.actionActive : ''}`}
            onClick={handleBookmark}
            disabled={!userId}
          >
            🔖 북마크
          </button>

          <div className={styles.recommendGroup}>
            <button
              className={`${styles.actionBtn} ${post.myRecommend === 1 ? styles.actionActive : ''}`}
              onClick={() => handleRecommend('RECOMMEND')}
              disabled={!userId}
            >
              👍 {post.recommendCount}
            </button>
            <button
              className={`${styles.actionBtn} ${styles.actionDanger} ${post.myRecommend === -1 ? styles.actionDangerActive : ''}`}
              onClick={() => handleRecommend('NOT_RECOMMEND')}
              disabled={!userId}
            >
              👎 {post.notRecommendCount}
            </button>
          </div>
        </div>

        {!userId && (
          <p className={styles.loginHint}>
            좋아요, 북마크, 추천은 <strong>로그인 후</strong> 사용할 수 있습니다.
          </p>
        )}
      </article>

      <section className={styles.commentsSection}>
        <h2 className={styles.commentsTitle}>
          댓글 <span>{post.commentCount}</span>
        </h2>

        {userId ? (
          <form className={styles.commentForm} onSubmit={handleCommentSubmit}>
            <textarea
              placeholder="댓글을 입력해 주세요."
              value={commentText}
              onChange={(event) => setCommentText(event.target.value)}
              rows={3}
              required
            />
            <button type="submit" disabled={submitting || !commentText.trim()}>
              {submitting ? '등록 중...' : '댓글 등록'}
            </button>
          </form>
        ) : (
          <div className={styles.commentLoginHint}>
            댓글을 작성하려면 로그인이 필요합니다.
          </div>
        )}

        <div className={styles.commentList}>
          {comments.length === 0 ? (
            <div className={styles.noComments}>첫 번째 댓글을 남겨보세요.</div>
          ) : (
            comments.map((comment) => (
              <CommentItem
                key={comment.id}
                comment={comment}
                userId={userId}
                onDelete={handleDeleteComment}
              />
            ))
          )}
        </div>
      </section>

      <Link to="/community" className={styles.backBtn}>목록으로 돌아가기</Link>
    </div>
  )
}
