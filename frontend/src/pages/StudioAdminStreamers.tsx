import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import StudioLayout from '../components/StudioLayout';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl } from '../api/client';

/** 스트리머 관리: 운영자(ADMIN) 전용 탭. 비운영자 접근 시 /studio로 리다이렉트 */

interface StreamerRow {
  id: number;
  loginId?: string;
  displayName?: string;
  streamerTier?: 'GENERAL' | 'PARTNER';
  totalReceivedPang?: number;
}

export default function StudioAdminStreamers() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [list, setList] = useState<StreamerRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      navigate('/studio', { replace: true });
      return;
    }
    if (user.role !== 'ADMIN') {
      navigate('/studio', { replace: true });
      return;
    }
    loadStreamers();
  }, [user, navigate]);

  const loadStreamers = () => {
    setLoading(true);
    fetch(apiUrl('api/admin/streamers'), { credentials: 'include' })
      .then((r) => {
        if (r.status === 403) {
          navigate('/studio', { replace: true });
          return [];
        }
        return r.json();
      })
      .then((data) => {
        setList(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  const setTier = (userId: number, tier: 'GENERAL' | 'PARTNER') => {
    fetch(apiUrl(`api/admin/streamers/${userId}/tier`), {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ tier }),
    })
      .then((r) => r.json())
      .then(() => loadStreamers())
      .catch(() => {});
  };

  if (!user || user.role !== 'ADMIN') {
    return null;
  }

  return (
    <StudioLayout>
      <h1 className="page-title">스트리머 관리</h1>
      <p style={{ color: 'var(--studio-text-muted)', marginBottom: 20 }}>
        일반 스트리머(팡 수수료 30%) / 파트너 스트리머(팡 수수료 20%). 운영자만 구분 변경 가능합니다.
      </p>
      <div className="admin-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>아이디</th>
              <th>이름</th>
              <th>구분</th>
              <th>받은 팡</th>
              <th>변경</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5} className="empty-msg">불러오는 중...</td></tr>
            ) : list.length === 0 ? (
              <tr><td colSpan={5} className="empty-msg">스트리머가 없습니다.</td></tr>
            ) : (
              list.map((s) => {
                const tier = s.streamerTier || 'GENERAL';
                const tierLabel = tier === 'PARTNER' ? '파트너 스트리머 (20%)' : '일반 스트리머 (30%)';
                return (
                  <tr key={s.id}>
                    <td>{s.loginId || '-'}</td>
                    <td>{s.displayName || '-'}</td>
                    <td><span className={`tier-badge ${tier}`}>{tierLabel}</span></td>
                    <td>{(s.totalReceivedPang ?? 0).toLocaleString()}</td>
                    <td>
                      <button type="button" className={`btn-tier ${tier === 'GENERAL' ? 'active' : ''}`} onClick={() => setTier(s.id, 'GENERAL')}>일반</button>
                      <button type="button" className={`btn-tier ${tier === 'PARTNER' ? 'active' : ''}`} onClick={() => setTier(s.id, 'PARTNER')}>파트너</button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </StudioLayout>
  );
}
