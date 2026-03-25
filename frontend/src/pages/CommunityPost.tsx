import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { useAlert } from '../contexts/AlertContext';
import {
  BOARD_LABELS,
  COMMUNITY_REPORT_REASONS,
  attachmentUrl,
  createComment,
  deleteComment,
  deletePost,
  fetchComments,
  fetchPostDetailForUser,
  fetchPostDetailPublic,
  recommendPost,
  submitCommunityReport,
  toggleBookmark,
  toggleLike,
  type CommentNode,
  type PostDetail,
  type ReportReason,
} from '../api/community';

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' });
  } catch {
    return iso;
  }
}

function ReportModal({
  open,
  targetLabel,
  reason,
  description,
  submitting,
  onReasonChange,
  onDescriptionChange,
  onSubmit,
  onClose,
}: {
  open: boolean;
  targetLabel: string;
  reason: ReportReason;
  description: string;
  submitting: boolean;
  onReasonChange: (value: ReportReason) => void;
  onDescriptionChange: (value: string) => void;
  onSubmit: () => void;
  onClose: () => void;
}) {
  if (!open) return null;

  return (
    <div className="community-report-backdrop" onClick={onClose} role="dialog" aria-modal="true">
      <div className="community-report-modal" onClick={(event) => event.stopPropagation()}>
        <h2>{targetLabel} 신고</h2>
        <p className="community-muted">신고 사유를 선택하면 운영 검토 대상으로 접수됩니다.</p>

        <label className="community-report-field">
          <span>신고 사유</span>
          <select value={reason} onChange={(event) => onReasonChange(event.target.value as ReportReason)}>
            {COMMUNITY_REPORT_REASONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="community-report-field">
          <span>상세 설명</span>
          <textarea
            rows={4}
            maxLength={500}
            value={description}
            onChange={(event) => onDescriptionChange(event.target.value)}
            placeholder="운영진이 확인해야 할 내용을 적어 주세요."
          />
        </label>

        <div className="community-report-actions">
          <button type="button" className="community-btn-ghost" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="button" className="community-btn-danger" onClick={onSubmit} disabled={submitting}>
            {submitting ? '신고 중...' : '신고 접수'}
          </button>
        </div>
      </div>
    </div>
  );
}

function CommentBlock({
  node,
  userId,
  postId,
  onDeleted,
  onReply,
  replyParentId,
  onReport,
}: {
  node: CommentNode;
  userId: number | null;
  postId: number;
  onDeleted: (commentDelta?: number) => void;
  onReply: (parentId: number) => void;
  replyParentId: number | null;
  onReport: (payload: { targetType: 'COMMENT'; commentId: number }) => void;
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
    onDeleted(-1);
  };

  const submitReply = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!userId) return;

    const text = replyText.trim();
    if (!text) return;

    setSubmitting(true);
    try {
      const { ok, message } = await createComment(userId, postId, text, node.id);
      if (!ok) {
        showAlert(message || '답글 등록에 실패했습니다.');
        return;
      }
      setReplyText('');
      onDeleted(1);
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
        <div className="community-comment-tools">
          <button type="button" className="community-comment-reply-btn" onClick={() => onReply(node.id)}>
            답글
          </button>
          {userId !== node.authorId ? (
            <button type="button" className="community-comment-report-btn" onClick={() => onReport({ targetType: 'COMMENT', commentId: node.id })}>
              신고
            </button>
          ) : null}
        </div>
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
              onDeleted={onDeleted}
              onReply={onReply}
              replyParentId={replyParentId}
              onReport={onReport}
            />
          ))}
        </ul>
      ) : null}
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
  const [reportTarget, setReportTarget] = useState<{ targetType: 'POST' | 'COMMENT'; commentId?: number } | null>(null);
  const [reportReason, setReportReason] = useState<ReportReason>('SPAM');
  const [reportDescription, setReportDescription] = useState('');
  const [reportBusy, setReportBusy] = useState(false);

  const reload = useCallback(async () => {
    if (!Number.isFinite(postId) || postId <= 0) {
      setNotFound(true);
      setLoading(false);
      return;
    }

    setLoading(true);
    const detailRes = user ? await fetchPostDetailForUser(user.id, postId) : await fetchPostDetailPublic(postId);
    if (!detailRes.ok || !detailRes.data) {
      setNotFound(detailRes.status === 404);
      setPost(null);
      setLoading(false);
      return;
    }

    setPost(detailRes.data);
    const commentRes = await fetchComments(postId);
    setComments(commentRes.ok && Array.isArray(commentRes.data) ? commentRes.data : []);
    setNotFound(false);
    setLoading(false);
  }, [postId, user]);

  useEffect(() => {
    reload();
  }, [reload]);

  const refreshCommentsOnly = useCallback(
    async (commentDelta?: number) => {
      const commentRes = await fetchComments(postId);
      if (commentRes.ok && Array.isArray(commentRes.data)) {
        setComments(commentRes.data);
      }
      if (commentDelta) {
        setPost((current) => (current ? { ...current, commentCount: Math.max(0, current.commentCount + commentDelta) } : current));
      }
    },
    [postId],
  );

  const openReportModal = (payload: { targetType: 'POST' | 'COMMENT'; commentId?: number }) => {
    if (!user) {
      showAlert('로그인 후 신고할 수 있습니다.');
      return;
    }
    setReportTarget(payload);
    setReportReason('SPAM');
    setReportDescription('');
  };

  const closeReportModal = () => {
    if (reportBusy) return;
    setReportTarget(null);
    setReportDescription('');
    setReportReason('SPAM');
  };

  const submitReport = async () => {
    if (!user || !reportTarget || !post) return;

    setReportBusy(true);
    try {
      const { ok, message } = await submitCommunityReport(user.id, {
        targetType: reportTarget.targetType,
        postId: post.id,
        commentId: reportTarget.commentId,
        reason: reportReason,
        description: reportDescription.trim() || undefined,
      });

      if (!ok) {
        showAlert(message || '신고 접수에 실패했습니다.');
        return;
      }

      showAlert('신고가 접수되었습니다.');
      closeReportModal();
    } finally {
      setReportBusy(false);
    }
  };

  const onToggleLike = async () => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }
    const { ok, message } = await toggleLike(user.id, post.id);
    if (!ok) {
      showAlert(message || '처리에 실패했습니다.');
      return;
    }
    reload();
  };

  const onToggleBookmark = async () => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }
    const { ok, message } = await toggleBookmark(user.id, post.id);
    if (!ok) {
      showAlert(message || '처리에 실패했습니다.');
      return;
    }
    reload();
  };

  const onRecommend = async (type: 'RECOMMEND' | 'NOT_RECOMMEND') => {
    if (!user || !post) {
      showAlert('로그인 후 이용할 수 있습니다.');
      return;
    }
    const { ok, message } = await recommendPost(user.id, post.id, type);
    if (!ok) {
      showAlert(message || '처리에 실패했습니다.');
      return;
    }
    reload();
  };

  const onDeletePost = async () => {
    if (!user || !post) return;
    if (!window.confirm('게시글을 삭제할까요?')) return;

    const { ok, message } = await deletePost(user.id, post.id);
    if (!ok) {
      showAlert(message || '삭제에 실패했습니다.');
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

    const text = commentText.trim();
    if (!text) return;

    setCommentBusy(true);
    try {
      const { ok, message } = await createComment(user.id, post.id, text);
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
          불러오는 중...
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

  const isAuthor = Boolean(user && post.authorId === user.id);

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
              {post.isNotice ? <span className="community-badge-notice">공지</span> : null}
              {post.title}
            </h1>
            <div className="community-article-meta">
              <span>{post.authorUsername}</span>
              <span className="community-muted">쨌</span>
              <span>{formatTime(post.createdAt)}</span>
              <span className="community-muted">쨌</span>
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

          <div className="community-article-body">{post.content}</div>

          {post.attachments?.length ? (
            <div className="community-attachments">
              <strong>첨부 파일</strong>
              <div className="community-attachment-grid">
                {post.attachments.map((attachment) =>
                  attachment.contentType?.startsWith('image/') ? (
                    <a
                      key={attachment.id}
                      href={attachmentUrl(attachment.filePath)}
                      target="_blank"
                      rel="noreferrer"
                      className="community-attachment-image-link"
                    >
                      <img src={attachmentUrl(attachment.filePath)} alt={attachment.fileName} className="community-attachment-image" />
                    </a>
                  ) : (
                    <a key={attachment.id} href={attachmentUrl(attachment.filePath)} target="_blank" rel="noreferrer">
                      {attachment.fileName}
                    </a>
                  ),
                )}
              </div>
            </div>
          ) : null}

          <div className="community-article-actions">
            <button type="button" className={post.liked ? 'active' : ''} onClick={onToggleLike}>
              좋아요 {post.likeCount}
            </button>
            <button type="button" className={post.bookmarked ? 'active' : ''} onClick={onToggleBookmark}>
              북마크
            </button>
            <button type="button" className={post.myRecommend === 1 ? 'active' : ''} onClick={() => onRecommend('RECOMMEND')}>
              추천 {post.recommendCount}
            </button>
            <button type="button" className={post.myRecommend === -1 ? 'active' : ''} onClick={() => onRecommend('NOT_RECOMMEND')}>
              비추천 {post.notRecommendCount}
            </button>
            {!isAuthor ? (
              <button type="button" className="community-btn-danger" onClick={() => openReportModal({ targetType: 'POST' })}>
                신고
              </button>
            ) : null}
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
        </article>

        <section className="community-comments-section">
          <h2>댓글 {post.commentCount}</h2>
          <form className="community-comment-form" onSubmit={onSubmitComment}>
            <textarea
              value={commentText}
              onChange={(event) => setCommentText(event.target.value)}
              rows={3}
              maxLength={2000}
              placeholder="댓글을 입력해 주세요."
            />
            <button type="submit" disabled={commentBusy}>
              {commentBusy ? '등록 중...' : '댓글 등록'}
            </button>
          </form>

          <ul className="community-comment-list">
            {comments.map((comment) => (
              <CommentBlock
                key={comment.id}
                node={comment}
                userId={user?.id ?? null}
                postId={postId}
                onDeleted={refreshCommentsOnly}
                onReply={setReplyParentId}
                replyParentId={replyParentId}
                onReport={openReportModal}
              />
            ))}
          </ul>
        </section>
      </div>

      <ReportModal
        open={reportTarget != null}
        targetLabel={reportTarget?.targetType === 'COMMENT' ? '댓글' : '게시글'}
        reason={reportReason}
        description={reportDescription}
        submitting={reportBusy}
        onReasonChange={setReportReason}
        onDescriptionChange={setReportDescription}
        onSubmit={submitReport}
        onClose={closeReportModal}
      />
    </Layout>
  );
}
