import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { useAlert } from '../contexts/AlertContext';
import {
  addPostAttachment,
  BOARD_CATEGORIES,
  BOARD_LABELS,
  type BoardCategory,
  createPostJson,
  createPostWithFiles,
  fetchPostDetailForUser,
  parseHashtagInput,
  updatePost,
} from '../api/community';

const MAX_IMAGE_COUNT = 8;

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
  const allowedCategories = BOARD_CATEGORIES.filter((category) => isAdmin || category !== 'NOTICE');

  const previewItems = useMemo(
    () =>
      selectedFiles.map((file) => ({
        key: `${file.name}-${file.size}-${file.lastModified}`,
        name: file.name,
        url: URL.createObjectURL(file),
      })),
    [selectedFiles]
  );

  useEffect(() => {
    return () => {
      previewItems.forEach((item) => URL.revokeObjectURL(item.url));
    };
  }, [previewItems]);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      navigate('/login', {
        replace: true,
        state: { from: isEdit ? `/community/write/${postId}` : '/community/write' },
      });
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
      setHashtagsRaw((data.hashtags || []).map((tag) => `#${tag}`).join(' '));
    })();
  }, [authLoading, isEdit, navigate, postId, user]);

  const handleSelectFiles = (e: React.ChangeEvent<HTMLInputElement>) => {
    const pickedFiles = Array.from(e.target.files || []).filter((file) =>
      file.type.startsWith('image/')
    );
    if (!pickedFiles.length) return;

    setSelectedFiles((prev) => {
      const merged = [...prev];
      pickedFiles.forEach((file) => {
        const exists = merged.some(
          (current) =>
            current.name === file.name &&
            current.size === file.size &&
            current.lastModified === file.lastModified
        );
        if (!exists && merged.length < MAX_IMAGE_COUNT) {
          merged.push(file);
        }
      });
      return merged;
    });

    e.target.value = '';
  };

  const handleRemoveFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, fileIndex) => fileIndex !== index));
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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
        const { ok, message } = await updatePost(user.id, postId, {
          title: trimmedTitle,
          content: trimmedContent,
          hashtags: hashtags.length ? hashtags : undefined,
        });
        if (!ok) {
          showAlert(message || '수정에 실패했습니다.');
          return;
        }

        if (selectedFiles.length) {
          for (const file of selectedFiles) {
            const uploadResult = await addPostAttachment(user.id, postId, file);
            if (!uploadResult.ok) {
              showAlert(uploadResult.message || '이미지 업로드에 실패했습니다.');
              return;
            }
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

      const { ok, data, message } = selectedFiles.length
        ? await createPostWithFiles(user.id, payload, selectedFiles)
        : await createPostJson(user.id, payload);

      if (!ok || !data) {
        showAlert(message || '등록에 실패했습니다.');
        return;
      }

      navigate(`/community/posts/${data.id}`, { replace: true });
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
      <div className="community-page community-write-page">
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
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
              required
            />
          </label>

          <label className="community-field">
            <span>내용</span>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={16}
              maxLength={10000}
              required
            />
          </label>

          <div className="community-field">
            <span>이미지 첨부</span>
            <div className="community-upload-panel">
              <label className="community-upload-trigger">
                <input type="file" accept="image/*" multiple onChange={handleSelectFiles} />
                이미지 선택
              </label>
              <p className="community-upload-help">
                JPG, PNG, WEBP 이미지를 최대 {MAX_IMAGE_COUNT}장까지 첨부할 수 있습니다.
              </p>
              {!!previewItems.length && (
                <div className="community-image-preview-grid">
                  {previewItems.map((item, index) => (
                    <div className="community-image-preview-card" key={item.key}>
                      <img src={item.url} alt={item.name} />
                      <div className="community-image-preview-meta">
                        <span title={item.name}>{item.name}</span>
                        <button
                          type="button"
                          className="community-image-remove"
                          onClick={() => handleRemoveFile(index)}
                        >
                          제거
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <label className="community-field">
            <span>해시태그 (선택, 공백·쉼표로 구분)</span>
            <input
              value={hashtagsRaw}
              onChange={(e) => setHashtagsRaw(e.target.value)}
              placeholder="#듀오 #팁"
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
