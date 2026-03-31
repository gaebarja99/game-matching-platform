import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ChannelLayout from '../components/ChannelLayout';
import { useAuth } from '../contexts/AuthContext';
import { useAlert } from '../contexts/AlertContext';

interface ChannelCommunityPost {
  id: number;
  userId: number;
  title: string;
  content: string;
  createdAt: string;
}

const CHANNEL_COMMUNITY_STORAGE_KEY = 'gamematcher-channel-community-posts';

function loadChannelCommunityPosts(): ChannelCommunityPost[] {
  try {
    const raw = localStorage.getItem(CHANNEL_COMMUNITY_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as ChannelCommunityPost[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export default function ChannelWrite() {
  const { user, loading } = useAuth();
  const navigate = useNavigate();
  const showAlert = useAlert();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [saving, setSaving] = useState(false);

  if (loading) {
    return (
      <ChannelLayout>
        <div className="channel-empty">
          <p className="channel-empty-title">작성 화면을 불러오는 중입니다.</p>
        </div>
      </ChannelLayout>
    );
  }

  if (!user) {
    return (
      <ChannelLayout>
        <div className="channel-login-msg">
          <p>채널 커뮤니티 글쓰기는 로그인 후 이용할 수 있습니다.</p>
          <Link to="/login">로그인하러 가기</Link>
        </div>
      </ChannelLayout>
    );
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    const trimmedTitle = title.trim();
    const trimmedContent = content.trim();

    if (!trimmedTitle || !trimmedContent) {
      showAlert('제목과 내용을 모두 입력해 주세요.');
      return;
    }

    setSaving(true);
    try {
      const posts = loadChannelCommunityPosts();
      const nextPost: ChannelCommunityPost = {
        id: Date.now(),
        userId: user.id,
        title: trimmedTitle,
        content: trimmedContent,
        createdAt: new Date().toISOString(),
      };

      localStorage.setItem(CHANNEL_COMMUNITY_STORAGE_KEY, JSON.stringify([nextPost, ...posts]));
      showAlert('채널 커뮤니티 글이 등록되었습니다.');
      navigate('/channel?tab=community', { replace: true });
    } finally {
      setSaving(false);
    }
  };

  return (
    <ChannelLayout>
      <div className="channel-info-stack">
        <section className="channel-panel">
          <div className="channel-panel-header">
            <div>
              <h2>채널 커뮤니티 글쓰기</h2>
              <p>팬들과 나눌 공지, 소식, 일상을 작성해 보세요.</p>
            </div>
            <Link to="/channel?tab=community" className="channel-panel-action channel-panel-action--ghost">
              목록으로
            </Link>
          </div>

          <form className="community-write-form" onSubmit={handleSubmit}>
            <label className="community-field">
              <span>제목</span>
              <input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={200} required />
            </label>

            <label className="community-field">
              <span>내용</span>
              <textarea
                value={content}
                onChange={(event) => setContent(event.target.value)}
                rows={12}
                maxLength={10000}
                required
              />
            </label>

            <div className="community-form-actions">
              <button type="submit" className="community-btn-primary" disabled={saving}>
                {saving ? '등록 중...' : '등록하기'}
              </button>
              <Link to="/channel?tab=community" className="community-btn-ghost">
                취소
              </Link>
            </div>
          </form>
        </section>
      </div>
    </ChannelLayout>
  );
}
