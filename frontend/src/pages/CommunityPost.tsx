import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { useAlert } from '../contexts/AlertContext';
import {
  BOARD_LABELS,
  type CommentNode,
  type PostDetail,
  attachmentUrl,
  createComment,
  deleteComment,
  deletePost,
  fetchComments,
  fetchPostDetailForUser,
  fetchPostDetailPublic,
  recommendPost,
  toggleBookmark,
  toggleLike,
} from '../api/community';

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' });
  } catch {
    return iso;
  }
}

function CommentBlock({
  node,
  userId,
  postId,
  onDeleted,
  onReply,
  replyParentId,
}: {
  node: CommentNode;
  userId: number | null;
  postId: number;
  onDeleted: (commentDelta?: number) => void;
  onReply: (parentId: number) => void;
  replyParentId: number | null;
}) {
  const showAlert = useAlert();
  const [replyText, setReplyText] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleDelete = async () => {
    if (!userId || node.authorId !== userId) return;
    if (!window.confirm('댓글을 삭제할까요?')) return;
    const { ok, message } = await deleteComment(userId, node.id);
    if (!ok) showAlert(message || '삭제에 실패했습니다.');
    else onDeleted(-1);
  };

  const submitReply = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userId) return;
    const t = replyText.trim();
    if (!t) return;
    setSubmitting(true);
    try {
      const { ok, message } = await createComment(userId, postId, t, node.id);
      if (!ok) showAlert(message || '답글 등록에 실패했습니다.');
      else {
        setReplyText('');
        onDeleted(1);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <li className={`community-comment-item ${node.parentId ? 'is-reply' : ''}`}>
      <div className="community-comment-head">
        <strong>{node.authorUsername}</strong>
        <span className="community-muted">{formatTime(node.createdAt)}</span>
        {userId === node.authorId && !node.isDeleted && (
          <button type="button" className="community-comment-del" onClick={handleDelete}>
            삭제
          </button>
        )}
      </div>
      <p className="community-comment-body">{node.content}</p>
      {userId && !node.isDeleted && (
        <button type="button" className="community-comment-reply-btn" onClick={() => onReply(node.id)}>
          답글
        </button>
      )}
      {replyParentId === node.id && userId && (
        <form className="community-reply-form" onSubmit={submitReply}>
          <textarea
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            rows={2}
            maxLength={2000}
            placeholder="답글을 입력하세요"
          />
          <button type="submit" disabled={submitting}>
            등록
          </button>
        </form>
      )}
      {node.replies && node.replies.length > 0 && (
        <ul className="community-comment-replies">
          {node.replies.map((r) => (
            <CommentBlock
              key={r.id}
              node={r}
              userId={userId}
              postId={postId}
              onDeleted={onDeleted}
              onReply={onReply}
              replyParentId={replyParentId}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

export default function CommunityPost() {
  const { postId: idParam } = useParams<{ postId: string }>();
  const postId = Number(idParam);
  const navigate = useNavigate();
  const { user } = useAuth();
  const showAlert = useAlert();

  const [post, setPost] = useState<PostDetail | null>(null);
  const [comments, setComments] = useState<CommentNode[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [replyParentId, setReplyParentId] = useState<number | null>(null);
  const [commentBusy, setCommentBusy] = useState(false);

  const reload = useCallback(async () => {
    if (!Number.isFinite(postId) || postId <= 0) {
      setNotFound(true);
      setLoading(false);
      return;
    }
    setLoading(true);
    const detailRes = user
      ? await fetchPostDetailForUser(user.id, postId)
      : await fetchPostDetailPublic(postId);
    if (!detailRes.ok || !detailRes.data) {
      setNotFound(detailRes.status === 404);
      setPost(null);
      setLoading(false);
      return;
    }
    setPost(detailRes.data);
    const cRes = await fetchComments(postId);
    if (cRes.ok && Array.isArray(cRes.data)) setComments(cRes.data);
    else setComments([]);
    setNotFound(false);
    setLoading(false);
  }, [postId, user]);

  useEffect(() => {
    reload();
  }, [reload]);

  const refreshCommentsOnly = useCallback(async (commentDelta?: number) => {
    const cRes = await fetchComments(postId);
    if (cRes.ok && Array.isArray(cRes.data)) setComments(cRes.data);
    if (commentDelta)
      setPost((p) => (p ? { ...p, commentCount: Math.max(0, p.commentCount + commentDelta) } : p));
  }, [postId]);

  const onToggleLike = async () => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }
    const { ok, message } = await toggleLike(user.id, post.id);
    if (!ok) showAlert(message || '처리에 실패했습니다.');
    else reload();
  };

  const onToggleBookmark = async () => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }
    const { ok, message } = await toggleBookmark(user.id, post.id);
    if (!ok) showAlert(message || '처리에 실패했습니다.');
    else reload();
  };

  const onRecommend = async (type: 'RECOMMEND' | 'NOT_RECOMMEND') => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }
    const { ok, message } = await recommendPost(user.id, post.id, type);
    if (!ok) showAlert(message || '처리에 실패했습니다.');
    else reload();
  };

  const onDeletePost = async () => {
    if (!user || !post) return;
    if (!window.confirm('게시글을 삭제할까요?')) return;
    const { ok, message } = await deletePost(user.id, post.id);
    if (!ok) showAlert(message || '삭제에 실패했습니다.');
    else navigate('/community', { replace: true });
  };

  const onSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !post) {
      showAlert('로그인 후 댓글을 작성할 수 있습니다.');
      return;
    }
    const t = commentText.trim();
    if (!t) return;
    setCommentBusy(true);
    try {
      const { ok, message } = await createComment(user.id, post.id, t);
      if (!ok) showAlert(message || '댓글 등록에 실패했습니다.');
      else {
        setCommentText('');
        await refreshCommentsOnly(1);
      }
    } finally {
      setCommentBusy(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <p className="community-muted" style={{ padding: 24 }}>
          불러오는 중…
        </p>
      </Layout>
    );
  }

  if (notFound || !post) {
    return (
      <Layout>
        <div className="community-page" style={{ padding: 24 }}>
          <p>게시글을 찾을 수 없습니다.</p>
          <Link to="/community">목록으로</Link>
        </div>
      </Layout>
    );
  }

  const isAuthor = user && post.authorId === user.id;

  return (
    <Layout>
      <div className="community-page community-post-page">
        <div className="community-post-breadcrumb">
          <Link to="/community">커뮤니티</Link>
          <span className="community-muted"> / </span>
          <span>{BOARD_LABELS[post.boardCategory]}</span>
        </div>

        <article className="community-article">
          <header className="community-article-head">
            <h1 className="community-article-title">
              {post.isNotice && <span className="community-badge-notice">공지</span>}
              {post.title}
            </h1>
            <div className="community-article-meta">
              <span>{post.authorUsername}</span>
              <span className="community-muted">·</span>
              <span>{formatTime(post.createdAt)}</span>
              <span className="community-muted">·</span>
              <span>조회 {post.viewCount}</span>
            </div>
            {post.hashtags?.length ? (
              <div className="community-hashtags">
                {post.hashtags.map((h) => (
                  <span key={h} className="community-tag">
                    #{h}
                  </span>
                ))}
              </div>
            ) : null}
          </header>

          <div className="community-article-body">{post.content}</div>

          {post.attachments?.length ? (
            <div className="community-attachments">
              <strong>첨부</strong>
              <ul>
                {post.attachments.map((a) => (
                  <li key={a.id}>
                    <a href={attachmentUrl(a.filePath)} target="_blank" rel="noreferrer">
                      {a.fileName}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          <div className="community-article-actions">
            <button type="button" className={post.liked ? 'active' : ''} onClick={onToggleLike}>
              좋아요 {post.likeCount}
            </button>
            <button type="button" className={post.bookmarked ? 'active' : ''} onClick={onToggleBookmark}>
              북마크
            </button>
            <button
              type="button"
              className={post.myRecommend === 1 ? 'active' : ''}
              onClick={() => onRecommend('RECOMMEND')}
            >
              추천 {post.recommendCount}
            </button>
            <button
              type="button"
              className={post.myRecommend === -1 ? 'active' : ''}
              onClick={() => onRecommend('NOT_RECOMMEND')}
            >
              비추천 {post.notRecommendCount}
            </button>
            {isAuthor && (
              <>
                <Link to={`/community/write/${post.id}`} className="community-btn-ghost">
                  수정
                </Link>
                <button type="button" className="community-btn-danger" onClick={onDeletePost}>
                  삭제
                </button>
              </>
            )}
          </div>
        </article>

        <section className="community-comments-section">
          <h2>댓글 {post.commentCount}</h2>
          {user ? (
            <form className="community-comment-form" onSubmit={onSubmitComment}>
              <textarea
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                rows={3}
                maxLength={2000}
                placeholder="댓글을 입력하세요"
              />
              <button type="submit" disabled={commentBusy}>
                등록
              </button>
            </form>
          ) : (
            <p className="community-muted">
              <Link to="/login">로그인</Link> 후 댓글을 작성할 수 있습니다.
            </p>
          )}

          <ul className="community-comment-list">
            {comments.map((c) => (
              <CommentBlock
                key={c.id}
                node={c}
                userId={user?.id ?? null}
                postId={post.id}
                onDeleted={refreshCommentsOnly}
                onReply={(pid) => setReplyParentId((cur) => (cur === pid ? null : pid))}
                replyParentId={replyParentId}
              />
            ))}
          </ul>
        </section>
      </div>
    </Layout>
  );
}
