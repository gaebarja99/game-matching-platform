import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl } from '../api/client';

declare global {
  interface Window {
    IMP?: {
      init: (storeId: string) => void;
      request_pay: (
        params: { pg: string; pay_method: string; merchant_uid: string; amount: number; name: string; buyer_name?: string; m_redirect_url?: string },
        callback: (r: { success?: boolean; imp_uid?: string; merchant_uid?: string; error_msg?: string }) => void
      ) => void;
    };
  }
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

const AD_FREE_PRICE_WON = 8900;

export default function ProfileAdFree() {
  const { user, refreshUser } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleBuyAdFree = () => {
    setError('');
    if (submitting) return;
    setSubmitting(true);

    fetch(apiUrl('api/payment/adfree/orders'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({}),
    })
      .then((r) =>
        r.json().then((d: { orderId?: string; amount?: number; orderName?: string; storeId?: string; pg?: string; payMethod?: string; message?: string }) => ({
          status: r.status,
          data: d,
        })),
      )
      .then((res) => {
        if (res.status !== 200 || !res.data?.orderId || res.data?.amount == null) {
          setError(res.data?.message ?? '주문 생성에 실패했습니다.');
          setSubmitting(false);
          return;
        }
        const { orderId, amount, orderName, storeId, pg, payMethod } = res.data;
        if (!storeId || typeof window.IMP === 'undefined') {
          setError('결제를 사용할 수 없습니다. 관리자에게 문의하세요.');
          setSubmitting(false);
          return;
        }
        window.IMP.init(storeId);
        window.IMP.request_pay(
          {
            pg: pg || 'html5_inicis.INIpayTest',
            pay_method: payMethod || 'card',
            merchant_uid: orderId,
            amount,
            name: orderName || '광고 제거 30일권',
            buyer_name: user?.nickname || user?.username || undefined,
          },
          (response) => {
            if (response.success && response.imp_uid) {
              fetch(apiUrl('api/payment/adfree/confirm'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ orderId, impUid: response.imp_uid }),
              })
                .then((r) => r.json().then((d: { success?: boolean; message?: string }) => ({ ok: r.ok, data: d })))
                .then((confirmRes) => {
                  setSubmitting(false);
                  if (confirmRes.ok && confirmRes.data?.success) {
                    refreshUser();
                  } else {
                    setError(confirmRes.data?.message ?? '결제 확인에 실패했습니다.');
                  }
                })
                .catch(() => {
                  setSubmitting(false);
                  setError('결제 확인 중 오류가 발생했습니다.');
                });
            } else {
              setSubmitting(false);
              setError(response.error_msg || '결제가 취소되었거나 실패했습니다.');
            }
          },
        );
      })
      .catch(() => {
        setSubmitting(false);
        setError('네트워크 오류가 발생했습니다.');
      });
  };

  return (
    <>
      <h1 className="profile-page-title">광고제거</h1>
      <section className="pang-history-section">
        <div className="profile-stat-card">
          <div className="label">광고 제거 상태</div>
          <div className="value">{user?.adFreeUntil && new Date(user.adFreeUntil) > new Date() ? '적용 중' : '미적용'}</div>
          <div className="sub">만료일: {user?.adFreeUntil ? formatDate(user.adFreeUntil) : '—'}</div>
        </div>
        <div className="profile-edit-field" style={{ marginTop: 24 }}>
          <label>광고 제거 30일권 현금 구매</label>
          <p style={{ marginTop: 4, marginBottom: 12 }}>가격: {AD_FREE_PRICE_WON.toLocaleString()}원 (결제 금액의 10% 마일리지 적립)</p>
          <button type="button" className="btn-save-profile" onClick={handleBuyAdFree} disabled={submitting}>
            {submitting ? '결제 처리 중…' : `${AD_FREE_PRICE_WON.toLocaleString()}원으로 구매하기`}
          </button>
          {error && (
            <p className="profile-edit-msg err" style={{ marginTop: 8 }}>
              {error}
            </p>
          )}
        </div>
      </section>
    </>
  );
}
