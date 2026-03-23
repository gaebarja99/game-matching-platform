import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { useAlert } from '../contexts/AlertContext';
import {
  BOARD_CATEGORIES,
  BOARD_LABELS,
  type BoardCategory,
  createPostJson,
  fetchPostDetailForUser,
  parseHashtagInput,
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
  const [saving, setSaving] = useState(false);
  const [loadErr, setLoadErr] = useState<string | null>(null);

  const isAdmin = (user?.role || '').toUpperCase() === 'ADMIN';
  const allowedCategories = BOARD_CATEGORIES.filter((c) => isAdmin || c !== 'NOTICE');

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
        setLoadErr(message || '게시글을 불러올 수 없습니다.');
        return;
      }
      if (data.authorId !== user.id) {
        setLoadErr('수정 권한이 없습니다.');
        return;
      }
      setBoardCategory(data.boardCategory);
      setTitle(data.title);
      setContent(data.content);
      setHashtagsRaw((data.hashtags || []).map((h) => `#${h}`).join(' '));
    })();
  }, [authLoading, user, isEdit, postId, navigate]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    const t = title.trim();
    const c = content.trim();
    if (!t || !c) {
      showAlert('제목과 내용을 입력해 주세요.');
      return;
    }
    setSaving(true);
    try {
      if (isEdit) {
        const hashtags = parseHashtagInput(hashtagsRaw);
        const { ok, message } = await updatePost(user.id, postId, {
          title: t,
          content: c,
          hashtags: hashtags.length ? hashtags : undefined,
        });
        if (!ok) {
          showAlert(message || '수정에 실패했습니다.');
          return;
        }
        navigate(`/community/posts/${postId}`, { replace: true });
      } else {
        const hashtags = parseHashtagInput(hashtagsRaw);
        const { ok, data, message } = await createPostJson(user.id, {
          boardCategory,
          title: t,
          content: c,
          isNotice: false,
          hashtags: hashtags.length ? hashtags : undefined,
        });
        if (!ok || !data) {
          showAlert(message || '등록에 실패했습니다.');
          return;
        }
        navigate(`/community/posts/${data.id}`, { replace: true });
      }
    } finally {
      setSaving(false);
    }
  };

  if (authLoading) {
    return (
      <Layout>
        <p className="community-muted" style={{ padding: 24 }}>
          확인 중…
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
            ← 목록
          </Link>
        </div>

        <form className="community-write-form" onSubmit={onSubmit}>
          {!isEdit && (
            <label className="community-field">
              <span>게시판</span>
              <select
                value={boardCategory}
                onChange={(e) => setBoardCategory(e.target.value as BoardCategory)}
              >
                {allowedCategories.map((c) => (
                  <option key={c} value={c}>
                    {BOARD_LABELS[c]}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="community-field">
            <span>제목</span>
            <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} required />
          </label>

          <label className="community-field">
            <span>내용</span>
            <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={16} maxLength={10000} required />
          </label>

          <label className="community-field">
            <span>해시태그 (선택, 공백·쉼표로 구분)</span>
            <input
              value={hashtagsRaw}
              onChange={(e) => setHashtagsRaw(e.target.value)}
              placeholder="#롤 #듀오"
            />
          </label>

          <div className="community-form-actions">
            <button type="submit" className="community-btn-primary" disabled={saving}>
              {saving ? '처리 중…' : isEdit ? '수정하기' : '등록하기'}
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
