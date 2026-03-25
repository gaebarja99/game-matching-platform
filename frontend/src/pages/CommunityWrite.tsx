import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { useAlert } from '../contexts/AlertContext';
import {
  BOARD_CATEGORIES,
  BOARD_LABELS,
  addPostAttachment,
  createPostJson,
  createPostMultipart,
  fetchPostDetailForUser,
  parseHashtagInput,
  type BoardCategory,
  updatePost,
} from '../api/community';

export default function CommunityWrite() {
  const { postId: postIdParam } = useParams<{ postId?: string }>();
  const postId = postIdParam ? Number(postIdParam) : NaN;
  const isEdit = Number.isFinite(postId) && postId > 0;

  const { user, loading: authLoading } = useAuth();
  const navigate = useNavigate();
  const showAlert = useAlert();

  const [boardCategory, setBoardCategory] = useState<BoardCategory>('FREE');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [hashtagsRaw, setHashtagsRaw] = useState('');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [saving, setSaving] = useState(false);
  const [loadErr, setLoadErr] = useState<string | null>(null);

  const isAdmin = (user?.role || '').toUpperCase() === 'ADMIN';
  const allowedCategories = useMemo(
    () => BOARD_CATEGORIES.filter((category) => isAdmin || category !== 'NOTICE'),
    [isAdmin],
  );

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      navigate('/login', { replace: true, state: { from: isEdit ? `/community/write/${postId}` : '/community/write' } });
      return;
    }
    if (!isEdit) return;

    (async () => {
      const { ok, data, message } = await fetchPostDetailForUser(user.id, postId);
      if (!ok || !data) {
        setLoadErr(message || '게시글을 불러오지 못했습니다.');
        return;
      }
      if (data.authorId !== user.id) {
        setLoadErr('수정 권한이 없습니다.');
        return;
      }
      setBoardCategory(data.boardCategory);
      setTitle(data.title);
      setContent(data.content);
      setHashtagsRaw((data.hashtags || []).map((tag) => `#${tag}`).join(' '));
    })();
  }, [authLoading, user, isEdit, postId, navigate]);

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!user) return;

    const trimmedTitle = title.trim();
    const trimmedContent = content.trim();
    if (!trimmedTitle || !trimmedContent) {
      showAlert('제목과 내용을 입력해 주세요.');
      return;
    }

    setSaving(true);
    try {
      const hashtags = parseHashtagInput(hashtagsRaw);

      if (isEdit) {
        const updated = await updatePost(user.id, postId, {
          title: trimmedTitle,
          content: trimmedContent,
          hashtags: hashtags.length ? hashtags : undefined,
        });

        if (!updated.ok) {
          showAlert(updated.message || '게시글 수정에 실패했습니다.');
          return;
        }

        for (const file of selectedFiles) {
          const attachment = await addPostAttachment(user.id, postId, file);
          if (!attachment.ok) {
            showAlert(attachment.message || '이미지 업로드에 실패했습니다.');
            return;
          }
        }

        navigate(`/community/posts/${postId}`, { replace: true });
        return;
      }

      const payload = {
        boardCategory,
        title: trimmedTitle,
        content: trimmedContent,
        isNotice: false,
        hashtags: hashtags.length ? hashtags : undefined,
      };

      const created = selectedFiles.length
        ? await createPostMultipart(user.id, payload, selectedFiles)
        : await createPostJson(user.id, payload);

      if (!created.ok || !created.data) {
        showAlert(created.message || '게시글 등록에 실패했습니다.');
        return;
      }

      navigate(`/community/posts/${created.data.id}`, { replace: true });
    } finally {
      setSaving(false);
    }
  };

  if (authLoading) {
    return (
      <Layout>
        <p className="community-muted" style={{ padding: 24 }}>
          확인 중...
        </p>
      </Layout>
    );
  }

  if (loadErr) {
    return (
      <Layout>
        <div className="community-page" style={{ padding: 24 }}>
          <p>{loadErr}</p>
          <Link to="/community">목록으로</Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="community-page">
        <div className="community-page-head">
          <h1 className="community-page-title">{isEdit ? '글 수정' : '글쓰기'}</h1>
          <Link to="/community" className="community-link-back">
            목록으로
          </Link>
        </div>

        <form className="community-write-form" onSubmit={onSubmit}>
          {!isEdit && (
            <label className="community-field">
              <span>게시판</span>
              <select value={boardCategory} onChange={(event) => setBoardCategory(event.target.value as BoardCategory)}>
                {allowedCategories.map((category) => (
                  <option key={category} value={category}>
                    {BOARD_LABELS[category]}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="community-field">
            <span>제목</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={200} required />
          </label>

          <label className="community-field">
            <span>내용</span>
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={16}
              maxLength={10000}
              required
            />
          </label>

          <label className="community-field">
            <span>이미지 업로드</span>
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={(event) => setSelectedFiles(Array.from(event.target.files || []))}
            />
            {selectedFiles.length ? (
              <div className="community-upload-list">
                {selectedFiles.map((file) => (
                  <span key={`${file.name}-${file.size}`} className="community-upload-chip">
                    {file.name}
                  </span>
                ))}
              </div>
            ) : (
              <p className="community-muted">JPG, PNG, GIF 같은 이미지 파일을 여러 장 올릴 수 있습니다.</p>
            )}
          </label>

          <label className="community-field">
            <span>해시태그 (선택)</span>
            <input
              value={hashtagsRaw}
              onChange={(event) => setHashtagsRaw(event.target.value)}
              placeholder="#롤 #질문 #팁"
            />
          </label>

          <div className="community-form-actions">
            <button type="submit" className="community-btn-primary" disabled={saving}>
              {saving ? '처리 중...' : isEdit ? '수정하기' : '등록하기'}
            </button>
            <Link to="/community" className="community-btn-ghost">
              취소
            </Link>
          </div>
        </form>
      </div>
    </Layout>
  );
}
