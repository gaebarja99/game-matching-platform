import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiUrl } from '../api/client';

interface MySubscriptionItem {
  userId: number;
  nickname: string;
  loginId: string;
  subscribedAt?: string;
}

function formatDate(isoStr: string | null | undefined): string {
  if (!isoStr) return '—';
  try {
    const d = new Date(isoStr.replace('T', ' ').replace(/-/g, '/'));
    return isNaN(d.getTime())
      ? isoStr
      : d.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  } catch {
    return isoStr;
  }
}

export default function ProfileSubscriptions() {
  const [list, setList] = useState<MySubscriptionItem[]>([]);

  useEffect(() => {
    fetch(apiUrl('api/subscription/my'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: MySubscriptionItem[] }) => setList(d.list ?? []))
      .catch(() => setList([]));
  }, []);

  return (
    <>
      <h1 className="profile-page-title">내 구독</h1>
      <section className="pang-history-section">
        <h2 className="section-title">구독 중인 스트리머</h2>
        <table className="pang-history-table" style={{ display: list.length ? 'table' : 'none' }}>
          <thead>
            <tr>
              <th>스트리머</th>
              <th>아이디</th>
              <th>구독 시작일</th>
              <th>방송 보기</th>
            </tr>
          </thead>
          <tbody>
            {list.map((s) => (
              <tr key={s.userId}>
                <td>{s.nickname || '-'}</td>
                <td>{s.loginId || '-'}</td>
                <td>{formatDate(s.subscribedAt)}</td>
                <td>
                  <Link to={`/channel?userId=${s.userId}`}>채널 이동</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="pang-history-empty" style={{ display: list.length ? 'none' : 'block' }}>
          구독 중인 스트리머가 없습니다.
        </div>
      </section>
    </>
  );
}

