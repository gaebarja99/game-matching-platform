import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { useAlert } from '../contexts/AlertContext';
import {
  BOARD_LABELS,
  attachmentUrl,
  createComment,
  deleteComment,
  deletePost,
  fetchComments,
  fetchPostDetailForUser,
  fetchPostDetailPublic,
  recommendPost,
  toggleBookmark,
  type CommentNode,
  type PostDetail,
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
  onChanged,
  replyParentId,
  onReply,
}: {
  node: CommentNode;
  userId: number | null;
  postId: number;
  onChanged: (commentDelta?: number) => void;
  replyParentId: number | null;
  onReply: (parentId: number) => void;
}) {
  const showAlert = useAlert();
  const [replyText, setReplyText] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleDelete = async () => {
    if (!userId || node.authorId !== userId) return;
    if (!window.confirm('댓글을 삭제할까요?')) return;

    const { ok, message } = await deleteComment(userId, node.id);
    if (!ok) {
      showAlert(message || '댓글 삭제에 실패했습니다.');
      return;
    }

    onChanged(-1);
  };

  const submitReply = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!userId) return;
    const content = replyText.trim();
    if (!content) return;

    setSubmitting(true);
    try {
      const { ok, message } = await createComment(userId, postId, content, node.id);
      if (!ok) {
        showAlert(message || '답글 등록에 실패했습니다.');
        return;
      }
      setReplyText('');
      onChanged(1);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <li className={`community-comment-item ${node.parentId ? 'is-reply' : ''}`}>
      <div className="community-comment-head">
        <strong>{node.authorUsername}</strong>
        <span className="community-muted">{formatTime(node.createdAt)}</span>
        {userId === node.authorId && !node.isDeleted ? (
          <button type="button" className="community-comment-del" onClick={handleDelete}>
            삭제
          </button>
        ) : null}
      </div>
      <p className="community-comment-body">{node.content}</p>
      {userId && !node.isDeleted ? (
        <button type="button" className="community-comment-reply-btn" onClick={() => onReply(node.id)}>
          답글
        </button>
      ) : null}

      {replyParentId === node.id && userId ? (
        <form className="community-reply-form" onSubmit={submitReply}>
          <textarea
            value={replyText}
            onChange={(event) => setReplyText(event.target.value)}
            rows={2}
            maxLength={2000}
            placeholder="답글을 입력해 주세요."
          />
          <button type="submit" disabled={submitting}>
            등록
          </button>
        </form>
      ) : null}

      {node.replies && node.replies.length > 0 ? (
        <ul className="community-comment-replies">
          {node.replies.map((reply) => (
            <CommentBlock
              key={reply.id}
              node={reply}
              userId={userId}
              postId={postId}
              onChanged={onChanged}
              replyParentId={replyParentId}
              onReply={onReply}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

export default function CommunityPost() {
  const { postId: postIdParam } = useParams<{ postId: string }>();
  const postId = Number(postIdParam);
  const navigate = useNavigate();
  const { user } = useAuth();
  const showAlert = useAlert();

  const [post, setPost] = useState<PostDetail | null>(null);
  const [comments, setComments] = useState<CommentNode[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [commentBusy, setCommentBusy] = useState(false);
  const [replyParentId, setReplyParentId] = useState<number | null>(null);

  const reload = useCallback(async () => {
    if (!Number.isFinite(postId) || postId <= 0) {
      setNotFound(true);
      setLoading(false);
      return;
    }

    setLoading(true);
    const detailResponse = user
      ? await fetchPostDetailForUser(user.id, postId)
      : await fetchPostDetailPublic(postId);

    if (!detailResponse.ok || !detailResponse.data) {
      setNotFound(detailResponse.status === 404);
      setPost(null);
      setLoading(false);
      return;
    }

    setPost(detailResponse.data);

    const commentResponse = await fetchComments(postId);
    if (commentResponse.ok && Array.isArray(commentResponse.data)) {
      setComments(commentResponse.data);
    } else {
      setComments([]);
    }

    setNotFound(false);
    setLoading(false);
  }, [postId, user]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const refreshCommentsOnly = useCallback(
    async (commentDelta?: number) => {
      const response = await fetchComments(postId);
      if (response.ok && Array.isArray(response.data)) {
        setComments(response.data);
      }
      if (commentDelta) {
        setPost((current) =>
          current ? { ...current, commentCount: Math.max(0, current.commentCount + commentDelta) } : current,
        );
      }
    },
    [postId],
  );

  const onToggleSave = async () => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }

    const { ok, message } = await toggleBookmark(user.id, post.id);
    if (!ok) {
      showAlert(message || '저장 처리에 실패했습니다.');
      return;
    }

    void reload();
  };

  const onReact = async (type: 'RECOMMEND' | 'NOT_RECOMMEND') => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }

    const { ok, message } = await recommendPost(user.id, post.id, type);
    if (!ok) {
      showAlert(message || '반응 처리에 실패했습니다.');
      return;
    }

    void reload();
  };

  const onDeletePost = async () => {
    if (!user || !post) return;
    if (!window.confirm('게시글을 삭제할까요?')) return;

    const { ok, message } = await deletePost(user.id, post.id);
    if (!ok) {
      showAlert(message || '게시글 삭제에 실패했습니다.');
      return;
    }

    navigate('/community', { replace: true });
  };

  const onSubmitComment = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!user || !post) {
      showAlert('로그인 후 댓글을 작성할 수 있습니다.');
      return;
    }

    const content = commentText.trim();
    if (!content) return;

    setCommentBusy(true);
    try {
      const { ok, message } = await createComment(user.id, post.id, content);
      if (!ok) {
        showAlert(message || '댓글 등록에 실패했습니다.');
        return;
      }

      setCommentText('');
      await refreshCommentsOnly(1);
    } finally {
      setCommentBusy(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <p className="community-muted" style={{ padding: 24 }}>
          불러오는 중입니다.
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

  const isAuthor = !!user && post.authorId === user.id;
  const isBlindPost = post.status === 'BLIND';
  const isDeletedPost = post.status === 'DELETED_BY_USER' || post.status === 'DELETED_BY_ADMIN';
  const contentNotice = isBlindPost
    ? '블라인드 처리된 글입니다.'
    : isDeletedPost
      ? '삭제된 글입니다.'
      : null;

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
            <h1 className="community-article-title">{contentNotice ?? post.title}</h1>
            <div className="community-article-meta">
              <span>{post.authorUsername}</span>
              <span className="community-muted">·</span>
              <span>{formatTime(post.createdAt)}</span>
              <span className="community-muted">·</span>
              <span>조회 {post.viewCount}</span>
            </div>
            {post.hashtags?.length ? (
              <div className="community-hashtags">
                {post.hashtags.map((hashtag) => (
                  <span key={hashtag} className="community-tag">
                    #{hashtag}
                  </span>
                ))}
              </div>
            ) : null}
          </header>

          <div className={`community-article-body ${contentNotice ? 'is-muted-state' : ''}`}>
            {contentNotice ?? post.content}
          </div>

          {!contentNotice && post.attachments?.length ? (
            <div className="community-attachments">
              <strong>첨부 파일</strong>
              <ul>
                {post.attachments.map((attachment) => (
                  <li key={attachment.id}>
                    <a href={attachmentUrl(attachment.filePath)} target="_blank" rel="noreferrer">
                      {attachment.fileName}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          {!contentNotice ? (
            <div className="community-article-actions">
              <button type="button" className={post.bookmarked ? 'active' : ''} onClick={onToggleSave}>
                저장
              </button>
              <button
                type="button"
                className={post.myRecommend === 1 ? 'active' : ''}
                onClick={() => onReact('RECOMMEND')}
              >
                버프 {post.recommendCount}
              </button>
              <button
                type="button"
                className={post.myRecommend === -1 ? 'active' : ''}
                onClick={() => onReact('NOT_RECOMMEND')}
              >
                너프 {post.notRecommendCount}
              </button>
              {isAuthor ? (
                <>
                  <Link to={`/community/write/${post.id}`} className="community-btn-ghost">
                    수정
                  </Link>
                  <button type="button" className="community-btn-danger" onClick={onDeletePost}>
                    삭제
                  </button>
                </>
              ) : null}
            </div>
          ) : null}
        </article>

        <section className="community-comments-section">
          <h2>댓글 {post.commentCount}</h2>
          {!contentNotice && user ? (
            <form className="community-comment-form" onSubmit={onSubmitComment}>
              <textarea
                value={commentText}
                onChange={(event) => setCommentText(event.target.value)}
                rows={3}
                maxLength={2000}
                placeholder="댓글을 입력하세요"
              />
              <button type="submit" disabled={commentBusy}>
                등록
              </button>
            </form>
          ) : null}

          {comments.length === 0 ? (
            <p className="community-muted">첫 댓글을 남겨 보세요.</p>
          ) : (
            <ul className="community-comment-list">
              {comments.map((comment) => (
                <CommentBlock
                  key={comment.id}
                  node={comment}
                  userId={user?.id ?? null}
                  postId={post.id}
                  onChanged={refreshCommentsOnly}
                  replyParentId={replyParentId}
                  onReply={setReplyParentId}
                />
              ))}
            </ul>
          )}
        </section>
      </div>
    </Layout>
  );
}
