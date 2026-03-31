import { Link } from 'react-router-dom';
import { springOrigin } from '../config';

export default function HomePage() {
  return (
    <div style={{ maxWidth: 520, margin: '0 auto', padding: 48, textAlign: 'center' }}>
      <h1
        style={{
          fontSize: '2rem',
          marginBottom: 24,
          background: 'linear-gradient(90deg, #a78bfa, #60a5fa)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          backgroundClip: 'text',
        }}
      >
        GameMatcher
      </h1>
      <p style={{ color: '#a1a1aa', marginBottom: 28 }}>React 프론트엔드 (Vite)</p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'center' }}>
        <Link
          to="/profile"
          style={{
            display: 'inline-block',
            padding: '14px 28px',
            background: '#6366f1',
            color: 'white',
            borderRadius: 8,
            fontWeight: 500,
            textDecoration: 'none',
          }}
        >
          프로필 (React)
        </Link>
        <a
          href={`${springOrigin}/stats/index.html`}
          style={{
            display: 'inline-block',
            padding: '14px 28px',
            background: '#27272a',
            color: '#e4e4e7',
            borderRadius: 8,
            fontWeight: 500,
          }}
        >
          전적 검색 (기존 정적)
        </a>
        <a
          href={`${springOrigin}/community/index.html`}
          style={{
            display: 'inline-block',
            padding: '14px 28px',
            background: '#27272a',
            color: '#e4e4e7',
            borderRadius: 8,
            fontWeight: 500,
          }}
        >
          커뮤니티 (기존 정적)
        </a>
      </div>
      <p style={{ marginTop: 32, fontSize: '0.8rem', color: '#52525b' }}>
        API는 `VITE_API_URL`·CORS(배포 시 같은 도메인)로 연결됩니다.
      </p>
    </div>
  );
}
