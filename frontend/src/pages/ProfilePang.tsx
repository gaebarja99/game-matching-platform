import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl } from '../api/client';

interface ChargeRow {
  id?: number;
  createdAt?: string;
  pangAmount?: number;
  priceWon?: number;
  orderId?: string;
  impUid?: string;
  refundable?: boolean;
  refunded?: boolean;
  detail?: string;
}

interface UsageRow {
  id?: number;
  createdAt?: string;
  amount?: number;
  streamId?: number;
  message?: string;
}

function formatDate(isoStr: string | null | undefined): string {
  if (!isoStr) return '--';
  const d = new Date(isoStr.replace('T', ' ').replace(/-/g, '/'));
  if (Number.isNaN(d.getTime())) return isoStr;
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const PRICE_PER_PANG = 1.2;
const PAGE_SIZE = 10;

export default function ProfilePang() {
  const { user, refreshUser } = useAuth();

  const [pangTab, setPangTab] = useState<'charge' | 'usage'>('charge');
  const [chargeList, setChargeList] = useState<ChargeRow[]>([]);
  const [usageList, setUsageList] = useState<UsageRow[]>([]);
  const [chargePage, setChargePage] = useState(0);
  const [usagePage, setUsagePage] = useState(0);
  const [chargeTotal, setChargeTotal] = useState(0);
  const [usageTotal, setUsageTotal] = useState(0);

  const [chargeOpen, setChargeOpen] = useState(false);
  const [chargeAmount, setChargeAmount] = useState(10000);
  const [chargeError, setChargeError] = useState('');

  const [historyError, setHistoryError] = useState('');
  const [refundingRowId, setRefundingRowId] = useState<number | null>(null);

  const pangBalance = user?.pangBalance ?? 0;

  const fetchChargeHistory = useCallback((page: number) => {
    fetch(apiUrl(`api/pang/charges?page=${page}&size=${PAGE_SIZE}`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { items: [], total: 0 }))
      .then((d: { items?: ChargeRow[]; total?: number }) => {
        setChargeList(d.items ?? []);
        setChargeTotal(typeof d.total === 'number' ? d.total : 0);
      })
      .catch(() => {
        setChargeList([]);
        setChargeTotal(0);
      });
  }, []);

  useEffect(() => {
    fetchChargeHistory(chargePage);
  }, [chargePage, fetchChargeHistory]);

  useEffect(() => {
    fetch(apiUrl(`api/donate/me?page=${usagePage}&size=${PAGE_SIZE}`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { items: [], total: 0 }))
      .then((d: { items?: UsageRow[]; total?: number }) => {
        setUsageList(d.items ?? []);
        setUsageTotal(typeof d.total === 'number' ? d.total : 0);
      })
      .catch(() => {
        setUsageList([]);
        setUsageTotal(0);
      });
  }, [usagePage]);

  const handlePangCharge = () => {
    setChargeError('');

    if (!chargeAmount || chargeAmount < 100) {
      setChargeError('100팡 이상 충전해 주세요.');
      return;
    }

    fetch(apiUrl('api/payment/orders'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ pangAmount: chargeAmount }),
    })
      .then((r) => r.json().then((d) => ({ status: r.status, data: d as any })))
      .then((res) => {
        if (res.status !== 200 || !res.data?.orderId || res.data?.amount == null) {
          setChargeError(res.data?.message ?? '주문 생성에 실패했습니다.');
          return;
        }

        const { orderId, amount, storeId, pg, payMethod } = res.data;
        if (!storeId || typeof window.IMP === 'undefined') {
          setChargeError('결제 모듈을 사용할 수 없습니다.');
          return;
        }

        window.IMP.init(storeId);
        window.IMP.request_pay(
          {
            pg: pg || 'html5_inicis.INIpayTest',
            pay_method: payMethod || 'card',
            merchant_uid: orderId,
            amount,
            name: `GameMatcher 팡 ${chargeAmount.toLocaleString()}개 충전`,
            buyer_name: user?.nickname || user?.username || undefined,
          },
          (response) => {
            if (!response.success) {
              setChargeError(response.error_msg || '결제가 취소되었거나 실패했습니다.');
              return;
            }

            const impUid = response.imp_uid || (response as any).payment_id || (response as any).paymentId;
            if (!impUid) {
              setChargeError('결제 식별값을 받지 못했습니다.');
              return;
            }

            fetch(apiUrl('api/payment/confirm'), {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              credentials: 'include',
              body: JSON.stringify({ orderId, impUid, paymentId: impUid }),
            })
              .then((r) => r.json().then((d) => ({ ok: r.ok, data: d as any })))
              .then((confirmRes) => {
                if (confirmRes.ok && confirmRes.data?.success) {
                  refreshUser();
                  setChargeOpen(false);
                  setChargePage(0);
                  fetchChargeHistory(0);
                } else {
                  setChargeError(confirmRes.data?.message ?? '결제 확인에 실패했습니다.');
                }
              })
              .catch(() => setChargeError('결제 확인 중 오류가 발생했습니다.'));
          },
        );
      })
      .catch(() => setChargeError('네트워크 오류가 발생했습니다.'));
  };

  const isRefundableRow = (row: ChargeRow) => {
    const hasOrderRef = Boolean((row.orderId && row.orderId.trim()) || (row.impUid && row.impUid.trim()));
    const positiveCharge = (row.pangAmount ?? 0) > 0;
    return hasOrderRef && positiveCharge;
  };

  const isEventGrantRow = (row: ChargeRow) => {
    const hasOrderRef = Boolean((row.orderId && row.orderId.trim()) || (row.impUid && row.impUid.trim()));
    const positiveCharge = (row.pangAmount ?? 0) > 0;
    return positiveCharge && !hasOrderRef;
  };

  const handleRefund = (row: ChargeRow) => {
    const orderId = row.orderId?.trim() || undefined;
    const impUid = row.impUid?.trim() || undefined;
    if (!orderId && !impUid) {
      setHistoryError('환불 대상 주문 정보가 없습니다.');
      return;
    }

    setHistoryError('');
    setRefundingRowId(row.id ?? null);
    fetch(apiUrl('api/payment/refund'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        orderId,
        impUid,
        reason: '사용자 요청 환불',
      }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d as any })))
      .then((res) => {
        if (!res.ok || !res.data?.success) {
          setHistoryError(res.data?.message ?? '환불 처리에 실패했습니다.');
          return;
        }
        refreshUser();
        setChargePage(0);
        fetchChargeHistory(0);
      })
      .catch(() => setHistoryError('환불 처리 중 오류가 발생했습니다.'))
      .finally(() => setRefundingRowId(null));
  };

  const renderPagination = (page: number, total: number, setPage: (p: number) => void) => {
    const totalPages = total > 0 ? Math.ceil(total / PAGE_SIZE) : 1;
    if (totalPages <= 1) return null;

    const current = page + 1;
    const pages: number[] = [];
    const maxButtons = 15;
    let start = Math.max(1, current - Math.floor(maxButtons / 2));
    let end = start + maxButtons - 1;

    if (end > totalPages) {
      end = totalPages;
      start = Math.max(1, end - maxButtons + 1);
    }

    for (let p = start; p <= end; p += 1) pages.push(p);

    return (
      <div className="table-pagination">
        <button type="button" disabled={page === 0} onClick={() => setPage(0)}>
          처음
        </button>
        <button type="button" disabled={page === 0} onClick={() => setPage(Math.max(0, page - 1))}>
          &lt;
        </button>
        {pages.map((p) => (
          <button key={p} type="button" className={p === current ? 'active' : ''} onClick={() => setPage(p - 1)}>
            {p}
          </button>
        ))}
        <button type="button" disabled={current >= totalPages} onClick={() => setPage(Math.min(totalPages - 1, page + 1))}>
          &gt;
        </button>
        <button type="button" disabled={current >= totalPages} onClick={() => setPage(totalPages - 1)}>
          마지막
        </button>
      </div>
    );
  };

  return (
    <>
      <h1 className="profile-page-title">내 팡</h1>

      <section className="pang-balance-section">
        <div className="profile-stats">
          <div className="profile-stat-card">
            <div className="label">보유중인 팡</div>
            <div className="value">{Number(pangBalance).toLocaleString()}</div>
            <div className="sub">후원 받은 팡까지 포함한 현재 사용 가능한 팡입니다.</div>
            <button
              type="button"
              className="profile-pang-charge-btn"
              onClick={() => {
                setChargeAmount(10000);
                setChargeError('');
                setChargeOpen(true);
              }}
            >
              팡 충전
            </button>
          </div>
        </div>
      </section>

      <section className="pang-history-section">
        <h2 className="section-title">팡 내역</h2>
        <div className="pang-history-tabs">
          <button type="button" className={pangTab === 'charge' ? 'active' : ''} onClick={() => setPangTab('charge')}>
            충전 내역
          </button>
          <button type="button" className={pangTab === 'usage' ? 'active' : ''} onClick={() => setPangTab('usage')}>
            사용 내역
          </button>
        </div>

        <div className="pang-history-table-wrap">
          {pangTab === 'charge' ? (
            <>
              <table className="pang-history-table" style={{ display: chargeList.length ? 'table' : 'none' }}>
                <thead>
                  <tr>
                    <th>일시</th>
                    <th>충전량</th>
                    <th>결제 금액</th>
                    <th>상세</th>
                  </tr>
                </thead>
                <tbody>
                  {chargeList.map((r) => {
                    const pang = r.pangAmount ?? 0;
                    const won = r.priceWon != null ? r.priceWon : Math.round(pang * PRICE_PER_PANG * 10) / 10;
                    const refunded = r.refunded === true;
                    const refundable = !refunded && isRefundableRow(r);
                    const detailText = r.detail?.trim() || '--';
                    const isEventGrant = detailText === '이벤트';
                    const isRefunding = refundingRowId != null && r.id != null && refundingRowId === r.id;
                    return (
                      <tr key={r.id ?? `${r.createdAt}-${pang}`}>
                        <td className="col-date">{formatDate(r.createdAt)}</td>
                        <td className="col-amount">{pang > 0 ? '+' : ''}{pang.toLocaleString()} 팡</td>
                        <td>{!isEventGrant && won !== 0 ? `${won.toLocaleString()}원` : '--'}</td>
                        <td>
                          {isEventGrant ? (
                            '이벤트'
                          ) : refunded ? (
                            <button type="button" className="pang-refund-btn done" disabled>
                              환불완료
                            </button>
                          ) : refundable ? (
                            <button
                              type="button"
                              className="pang-refund-btn"
                              disabled={isRefunding}
                              onClick={() => handleRefund(r)}
                            >
                              {isRefunding ? '처리중' : '환불'}
                            </button>
                          ) : (
                            detailText
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              <div className="pang-history-empty" style={{ display: chargeList.length ? 'none' : 'block' }}>
                충전 내역이 없습니다.
              </div>
              {chargeList.length > 0 && renderPagination(chargePage, chargeTotal, setChargePage)}
              {historyError && (
                <p className="profile-edit-msg err" style={{ margin: '10px 12px 12px' }}>
                  {historyError}
                </p>
              )}
            </>
          ) : (
            <>
              <table className="pang-history-table" style={{ display: usageList.length ? 'table' : 'none' }}>
                <thead>
                  <tr>
                    <th>일시</th>
                    <th>사용량</th>
                    <th>방송</th>
                    <th>메시지</th>
                  </tr>
                </thead>
                <tbody>
                  {usageList.map((r) => (
                    <tr key={r.id ?? `${r.createdAt}-${r.streamId}-${r.amount}`}>
                      <td className="col-date">{formatDate(r.createdAt)}</td>
                      <td className="col-amount">-{(r.amount ?? 0).toLocaleString()} 팡</td>
                      <td>
                        <Link to={`/watch/${r.streamId ?? ''}`}>방송 #{r.streamId ?? ''}</Link>
                      </td>
                      <td>{(r.message && r.message.length > 30 ? `${r.message.slice(0, 30)}...` : r.message) ?? '--'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="pang-history-empty" style={{ display: usageList.length ? 'none' : 'block' }}>
                사용 내역이 없습니다.
              </div>
              {usageList.length > 0 && renderPagination(usagePage, usageTotal, setUsagePage)}
            </>
          )}
        </div>
      </section>

      <div className={`modal-backdrop ${chargeOpen ? 'show' : ''}`} onClick={() => setChargeOpen(false)} role="dialog" aria-modal="true">
        <div className="profile-edit-modal" onClick={(e) => e.stopPropagation()}>
          <h2>팡 충전</h2>
          <p className="pang-charge-desc">1팡 = 1.2원</p>
          <div className="profile-edit-field">
            <label>충전할 팡 개수</label>
            <input
              type="number"
              min={100}
              value={chargeAmount}
              onChange={(e) => setChargeAmount(Number(e.target.value) || 0)}
              placeholder="100 이상 입력"
            />
            <div className="pang-charge-quick">
              {[1000, 5000, 10000, 50000, 100000].map((amt) => (
                <button key={amt} type="button" className="pang-charge-quick-btn" onClick={() => setChargeAmount((prev) => (prev || 0) + amt)}>
                  {amt.toLocaleString()}
                </button>
              ))}
            </div>
          </div>

          <p className="pang-charge-total">
            {chargeAmount > 0
              ? `${chargeAmount.toLocaleString()}팡 = ${(chargeAmount * PRICE_PER_PANG).toLocaleString(undefined, {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 2,
                })}원`
              : ''}
          </p>

          {chargeError && (
            <p className="profile-edit-msg err" style={{ marginTop: 8 }}>
              {chargeError}
            </p>
          )}

          <div className="profile-edit-actions">
            <button type="button" className="btn-save-profile" onClick={handlePangCharge}>
              충전하기
            </button>
            <button type="button" className="btn-cancel-profile" onClick={() => setChargeOpen(false)}>
              취소
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
