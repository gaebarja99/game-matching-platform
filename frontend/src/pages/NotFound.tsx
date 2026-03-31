import { Link } from 'react-router-dom';
import Layout from '../components/Layout';

/**
 * SPA에서 정의되지 않은 경로(클라이언트 404) 시 표시
 */
export default function NotFound() {
  return (
    <Layout>
      <div className="not-found-page">
        <p className="not-found-code" aria-hidden>
          404
        </p>
        <h1 className="not-found-title">페이지를 찾을 수 없습니다</h1>
        <p className="not-found-desc">주소가 잘못 입력되었거나, 삭제·이동된 페이지일 수 있습니다.</p>
        <Link to="/" className="not-found-home-btn">
          홈으로 돌아가기
        </Link>
      </div>
    </Layout>
  );
}
