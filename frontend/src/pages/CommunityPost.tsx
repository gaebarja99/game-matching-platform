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
        <h2>{targetLabel} {'\uC2E0\uACE0'}</h2>
        <p className="community-muted">{'\uC2E0\uACE0 \uC0AC\uC720\uB97C \uC120\uD0DD\uD558\uBA74 \uC6B4\uC601 \uAC80\uD1A0 \uB300\uC0C1\uC73C\uB85C \uC811\uC218\uB429\uB2C8\uB2E4.'}</p>

        <label className="community-report-field">
          <span>{'\uC2E0\uACE0 \uC0AC\uC720'}</span>
          <select value={reason} onChange={(event) => onReasonChange(event.target.value as ReportReason)}>
            {COMMUNITY_REPORT_REASONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="community-report-field">
          <span>{'\uC0C1\uC138 \uC124\uBA85'}</span>
          <textarea
            rows={4}
            maxLength={500}
            value={description}
            onChange={(event) => onDescriptionChange(event.target.value)}
            placeholder={'\uC6B4\uC601\uC9C4\uC774 \uD655\uC778\uD574\uC57C \uD560 \uB0B4\uC6A9\uC744 \uC801\uC5B4 \uC8FC\uC138\uC694.'}
          />
        </label>

        <div className="community-report-actions">
          <button type="button" className="community-btn-ghost" onClick={onClose} disabled={submitting}>
            {'\uCDE8\uC18C'}
          </button>
          <button type="button" className="community-btn-danger" onClick={onSubmit} disabled={submitting}>
            {submitting ? '\uC2E0\uACE0 \uC911...' : '\uC2E0\uACE0 \uC811\uC218'}
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
    if (!window.confirm('\uB313\uAE00\uC744 \uC0AD\uC81C\uD560\uAE4C\uC694?')) return;

    const { ok, message } = await deleteComment(userId, node.id);
    if (!ok) {
      showAlert(message || '\uB313\uAE00 \uC0AD\uC81C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
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
        showAlert(message || '\uB2F5\uAE00 \uB4F1\uB85D\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
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
        <span className="community-muted">{'\u00B7 '}{formatTime(node.createdAt)}</span>
        {userId === node.authorId && !node.isDeleted ? (
          <button type="button" className="community-comment-del" onClick={handleDelete}>
            {'\uC0AD\uC81C'}
          </button>
        ) : null}
      </div>

      <p className="community-comment-body">{node.content}</p>

      {userId && !node.isDeleted ? (
        <div className="community-comment-tools">
          <button type="button" className="community-comment-reply-btn" onClick={() => onReply(node.id)}>
            {'\uB2F5\uAE00'}
          </button>
          {userId !== node.authorId ? (
            <button type="button" className="community-comment-report-btn" onClick={() => onReport({ targetType: 'COMMENT', commentId: node.id })}>
              {'\uC2E0\uACE0'}
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
            placeholder={'\uB2F5\uAE00\uC744 \uC785\uB825\uD574 \uC8FC\uC138\uC694.'}
          />
          <button type="submit" disabled={submitting}>
            {'\uB4F1\uB85D'}
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
      showAlert('\uB85C\uADF8\uC778 \uD6C4 \uC2E0\uACE0\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.');
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
        showAlert(message || '\uC2E0\uACE0 \uC811\uC218\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
        return;
      }

      showAlert('\uC2E0\uACE0\uAC00 \uC811\uC218\uB418\uC5C8\uC2B5\uB2C8\uB2E4.');
      closeReportModal();
    } finally {
      setReportBusy(false);
    }
  };

  const onToggleBookmark = async () => {
    if (!user || !post) {
      showAlert('\uB85C\uADF8\uC778 \uD6C4 \uC774\uC6A9\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.');
      return;
    }
    const { ok, message } = await toggleBookmark(user.id, post.id);
    if (!ok) {
      showAlert(message || '\uCC98\uB9AC\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
      return;
    }
    reload();
  };

  const onRecommend = async (type: 'RECOMMEND' | 'NOT_RECOMMEND') => {
    if (!user || !post) {
      showAlert('\uB85C\uADF8\uC778 \uD6C4 \uC774\uC6A9\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.');
      return;
    }
    const { ok, message } = await recommendPost(user.id, post.id, type);
    if (!ok) {
      showAlert(message || '\uCC98\uB9AC\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
      return;
    }
    reload();
  };

  const onDeletePost = async () => {
    if (!user || !post) return;
    if (!window.confirm('\uAC8C\uC2DC\uAE00\uC744 \uC0AD\uC81C\uD560\uAE4C\uC694?')) return;

    const { ok, message } = await deletePost(user.id, post.id);
    if (!ok) {
      showAlert(message || '\uC0AD\uC81C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
      return;
    }
    navigate('/community', { replace: true });
  };

  const onSubmitComment = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!user || !post) {
      showAlert('\uB85C\uADF8\uC778 \uD6C4 \uB313\uAE00\uC744 \uC791\uC131\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.');
      return;
    }

    const text = commentText.trim();
    if (!text) return;

    setCommentBusy(true);
    try {
      const { ok, message } = await createComment(user.id, post.id, text);
      if (!ok) {
        showAlert(message || '\uB313\uAE00 \uB4F1\uB85D\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
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
          {'\uBD88\uB7EC\uC624\uB294 \uC911...'}
        </p>
      </Layout>
    );
  }

  if (notFound || !post) {
    return (
      <Layout>
        <div className="community-page" style={{ padding: 24 }}>
          <p>{"\uC874\uC7AC\uD558\uC9C0 \uC54A\uAC70\uB098 \uC0AD\uC81C\uB41C \uAC8C\uC2DC\uAE00\uC785\uB2C8\uB2E4."}</p>
          <Link to="/community">{"\uCEE4\uBBA4\uB2C8\uD2F0\uB85C \uB3CC\uC544\uAC00\uAE30"}</Link>
        </div>
      </Layout>
    );
  }

  const isAuthor = Boolean(user && post.authorId === user.id);

  return (
    <Layout>
      <div className="community-page community-post-page">
        <div className="community-post-breadcrumb">
          <Link to="/community">{"\uCEE4\uBBA4\uB2C8\uD2F0"}</Link>
          <span className="community-muted"> / </span>
          <span>{BOARD_LABELS[post.boardCategory]}</span>
        </div>

        <article className="community-article">
          <header className="community-article-head">
            <h1 className="community-article-title">
              {post.isNotice ? <span className="community-badge-notice">{"\uACF5\uC9C0"}</span> : null}
              {post.title}
            </h1>
            <div className="community-article-meta">
              <span>{post.authorUsername}</span>
              <span className="community-muted">{" \u00B7 "}</span>
              <span>{formatTime(post.createdAt)}</span>
              <span className="community-muted">{" \u00B7 "}</span>
              <span>{"\uC870\uD68C "}{post.viewCount}</span>
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
              <strong>{"\uCCA8\uBD80 \uD30C\uC77C"}</strong>
              <div className="community-attachment-grid">
                {post.attachments.map((attachment) =>
                  attachment.contentType?.startsWith("image/") ? (
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
            <button type="button" className={post.bookmarked ? "active" : ""} onClick={onToggleBookmark}>
              {"\uBD81\uB9C8\uD06C"}
            </button>
            <button type="button" className={post.myRecommend === 1 ? "active" : ""} onClick={() => onRecommend("RECOMMEND")}>
              {"\uCD94\uCC9C "}{post.recommendCount}
            </button>
            <button type="button" className={post.myRecommend === -1 ? "active" : ""} onClick={() => onRecommend("NOT_RECOMMEND")}>
              {"\uBE44\uCD94\uCC9C "}{post.notRecommendCount}
            </button>
            {!isAuthor ? (
              <button type="button" className="community-btn-danger" onClick={() => openReportModal({ targetType: "POST" })}>
                {"\uC2E0\uACE0"}
              </button>
            ) : null}
            {isAuthor ? (
              <>
                <Link to={`/community/write/${post.id}`} className="community-btn-ghost">
                  {"\uC218\uC815"}
                </Link>
                <button type="button" className="community-btn-danger" onClick={onDeletePost}>
                  {"\uC0AD\uC81C"}
                </button>
              </>
            ) : null}
          </div>
        </article>

        <section className="community-comments-section">
          <h2>{"\uB313\uAE00 "}{post.commentCount}</h2>
          <form className="community-comment-form" onSubmit={onSubmitComment}>
            <textarea
              value={commentText}
              onChange={(event) => setCommentText(event.target.value)}
              rows={3}
              maxLength={2000}
              placeholder={"\uB313\uAE00\uC744 \uC785\uB825\uD574 \uC8FC\uC138\uC694."}
            />
            <button type="submit" disabled={commentBusy}>
              {commentBusy ? "\uB4F1\uB85D \uC911..." : "\uB313\uAE00 \uB4F1\uB85D"}
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
        targetLabel={reportTarget?.targetType === "COMMENT" ? "\uB313\uAE00" : "\uAC8C\uC2DC\uAE00"}
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

