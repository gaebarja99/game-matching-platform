import { Link } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';

const quickLinks = [
  {
    to: '/admin/streamers',
    kicker: '권한',
    title: '스트리머 관리',
    description: '일반/파트너 등급을 조정하고 가입일, 받은 팡 기준으로 운영 대상을 빠르게 확인합니다.',
  },
  {
    to: '/admin/reports',
    kicker: '제재',
    title: '신고 관리',
    description: '신고 접수, 메모, 계정 정지와 해제 흐름을 같은 화면에서 처리합니다.',
  },
  {
    to: '/admin/members',
    kicker: '회원',
    title: '회원 관리',
    description: '정지 기간 선택, 상세 정보, DB 저장 이력까지 운영 화면에서 바로 다룹니다.',
  },
  {
    to: '/admin/settlements',
    kicker: '정산',
    title: '정산 관리',
    description: '결제와 정산 상태를 운영자가 직접 변경하고 최근 신청 순으로 확인합니다.',
  },
  {
    to: '/admin/revenue',
    kicker: '매출',
    title: '매출 관리',
    description: '회사 매출, 완료 금액, 정산 대기 금액, 플랫폼 수수료 추정치를 한 번에 봅니다.',
  },
];

export default function Admin() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <AdminLayout
        title="관리자 대시보드"
        description="관리자 정보를 확인하는 중입니다."
      >
        <section className="admin-panel">
          <p className="admin-subtext">관리자 화면을 불러오는 중...</p>
        </section>
      </AdminLayout>
    );
  }

  if (!user || user.role !== 'ADMIN') {
    return null;
  }

  return (
    <AdminLayout
      title="관리자 대시보드"
      description="신고, 회원, 정산, 매출 관리를 운영 전용 화면에서 이어서 처리할 수 있게 정리했습니다."
    >
      <section className="admin-overview-grid">
        <article className="admin-panel admin-panel-hero">
          <p className="admin-panel-kicker">운영 개요</p>
          <h2>관리자 운영 흐름을 /admin 아래에 한곳으로 모았습니다.</h2>
          <p>
            회원 정지 기간 관리, 신고 메모, DB 저장 이력, 회사 매출 확인까지 분리된 도구처럼 쓸 수 있도록 구조를 맞췄습니다.
          </p>
          <div className="admin-hero-actions">
            <Link to="/admin/members" className="admin-cta primary">
              회원 관리 열기
            </Link>
            <Link to="/admin/revenue" className="admin-cta secondary">
              매출 관리 보기
            </Link>
          </div>
        </article>

        {quickLinks.slice(0, 2).map((item) => (
          <article key={item.to} className="admin-panel">
            <p className="admin-panel-kicker">{item.kicker}</p>
            <h3>{item.title}</h3>
            <p>{item.description}</p>
            <Link to={item.to} className="admin-inline-link">
              바로 이동
            </Link>
          </article>
        ))}
      </section>

      <section className="admin-feature-grid">
        {quickLinks.map((item) => (
          <article key={item.to} className="admin-panel admin-panel-compact">
            <p className="admin-panel-kicker">{item.kicker}</p>
            <h3>{item.title}</h3>
            <p>{item.description}</p>
            <Link to={item.to} className="admin-inline-link">
              화면 열기
            </Link>
          </article>
        ))}
      </section>
    </AdminLayout>
  );
}
