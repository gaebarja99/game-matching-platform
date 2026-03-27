import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl } from '../api/client';

interface MileagePurchaseRow {
  id?: number;
  type?: 'ADMIN_GIFT' | 'PANG' | 'SUBSCRIPTION_TICKET' | 'AD_FREE_30_DAYS' | string;
  mileageCost?: number;
  pangAmount?: number;
  targetUserId?: number;
  targetUserNickname?: string;
  createdAt?: string;
}

interface StreamerSearchItem {
  id: number;
  username?: string;
  nickname?: string;
  profileImageUrl?: string;
}

function formatDate(isoStr: string | null | undefined): string {
  if (!isoStr) return '-';
  try {
    const d = new Date(isoStr.replace('T', ' ').replace(/-/g, '/'));
    return isNaN(d.getTime())
      ? isoStr
      : d.toLocaleString('ko-KR', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
        });
  } catch {
    return isoStr;
  }
}

const PAGE_SIZE = 10;

export default function ProfileMileageShop() {
  const { user, refreshUser } = useAuth();
  const [shopPangAmount, setShopPangAmount] = useState(10000);
  const [streamerQuery, setStreamerQuery] = useState('');
  const [streamerResults, setStreamerResults] = useState<StreamerSearchItem[]>([]);
  const [selectedStreamer, setSelectedStreamer] = useState<StreamerSearchItem | null>(null);
  const [shopPrices, setShopPrices] = useState<{ pangMileageCostPerPang?: number; subscriptionTicketCost?: number; adFree30DaysCost?: number }>({});
  const [shopHistory, setShopHistory] = useState<MileagePurchaseRow[]>([]);
  const [shopMsg, setShopMsg] = useState('');
  const [shopLoading, setShopLoading] = useState(false);
  const [historyTab, setHistoryTab] = useState<'charge' | 'usage'>('charge');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);

  const loadPrices = () => {
    fetch(apiUrl('api/mileage-shop/prices'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : {}))
      .then((d: { pangMileageCostPerPang?: number; subscriptionTicketCost?: number; adFree30DaysCost?: number }) => setShopPrices(d))
      .catch(() => setShopPrices({}));
  };

  const pangMileageCostPerPang = shopPrices.pangMileageCostPerPang ?? 2;

  const loadHistory = (pageIndex: number) => {
    fetch(apiUrl(`api/mileage-shop/history?page=${pageIndex}&size=${PAGE_SIZE}`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { items: [], total: 0 }))
      .then((d: { items?: MileagePurchaseRow[]; total?: number }) => {
        const items = d.items ?? [];
        setTotal(typeof d.total === 'number' ? d.total : 0);
        setShopHistory(items);
      })
      .catch(() => {
        setTotal(0);
        setShopHistory([]);
      });
  };

  const chargeHistory = shopHistory.filter((item) => item.type === 'ADMIN_GIFT');
  const usageHistory = shopHistory.filter((item) => item.type !== 'ADMIN_GIFT');

  useEffect(() => {
    loadPrices();
    loadHistory(0);
  }, []);

  useEffect(() => {
    loadHistory(page);
  }, [page]);

  useEffect(() => {
    const q = streamerQuery.trim();
    if (q.length < 2 || (selectedStreamer && q === (selectedStreamer.nickname || selectedStreamer.username || ''))) {
      setStreamerResults([]);
      return;
    }
    const timer = setTimeout(() => {
      fetch(apiUrl(`api/mileage-shop/streamers/search?q=${encodeURIComponent(q)}`), { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : []))
        .then((d: StreamerSearchItem[]) => setStreamerResults(Array.isArray(d) ? d : []))
        .catch(() => setStreamerResults([]));
    }, 250);
    return () => clearTimeout(timer);
  }, [streamerQuery, selectedStreamer]);

  const buyMileagePang = () => {
    setShopMsg('');
    const amount = Math.floor(Number(shopPangAmount) || 0);
    if (amount < 1000) {
      setShopMsg('마일리지 팡 구매는 최소 1,000팡부터 가능합니다.');
      return;
    }
    setShopLoading(true);
    fetch(apiUrl('api/mileage-shop/pang'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ pangAmount: amount }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        setShopMsg(res.data?.message ?? (res.ok ? '구매가 완료되었습니다.' : '구매에 실패했습니다.'));
        if (res.ok) {
          refreshUser();
          setPage(0);
        }
      })
      .catch(() => setShopMsg('요청에 실패했습니다.'))
      .finally(() => setShopLoading(false));
  };

  const buyMileageSubscription = () => {
    setShopMsg('');
    const uid = selectedStreamer?.id;
    if (!uid) {
      setShopMsg('구독권을 받을 스트리머를 검색해서 선택해 주세요.');
      return;
    }
    setShopLoading(true);
    fetch(apiUrl('api/mileage-shop/subscription'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
              body: JSON.stringify({ userId: uid }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        setShopMsg(res.data?.message ?? (res.ok ? '구매가 완료되었습니다.' : '구매에 실패했습니다.'));
        if (res.ok) {
          refreshUser();
          setPage(0);
        }
      })
      .catch(() => setShopMsg('요청에 실패했습니다.'))
      .finally(() => setShopLoading(false));
  };

  const buyAdFree = () => {
    setShopMsg('');
    setShopLoading(true);
    fetch(apiUrl('api/mileage-shop/adfree'), {
      method: 'POST',
      credentials: 'include',
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        setShopMsg(res.data?.message ?? (res.ok ? '구매가 완료되었습니다.' : '구매에 실패했습니다.'));
        if (res.ok) {
          refreshUser();
          setPage(0);
        }
      })
      .catch(() => setShopMsg('요청에 실패했습니다.'))
      .finally(() => setShopLoading(false));
  };

  const renderPagination = () => {
    if (!shopHistory.length) return null;
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
          «
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
          »
        </button>
      </div>
    );
  };

  return (
    <>
      <h1 className="profile-page-title">마일리지 상점</h1>

      <section className="pang-history-section">
        <div className="pang-history-table-wrap mileage-shop-layout">
          <div className="profile-edit-field mileage-shop-item">
            <label>팡 구매 (1팡 = {pangMileageCostPerPang}M)</label>
            <p className="pang-charge-desc">최소 1,000팡부터 구매할 수 있습니다.</p>
            <div className="mileage-shop-row">
              <input
                type="number"
                min={1000}
                value={shopPangAmount}
                onChange={(e) => setShopPangAmount(Number(e.target.value) || 0)}
              />
            </div>
            <div className="mileage-shop-actions">
              <button type="button" className="btn-save-profile" onClick={buyMileagePang} disabled={shopLoading}>
                마일리지로 팡 구매
              </button>
            </div>
          </div>

          <div className="profile-edit-field mileage-shop-item">
            <label>구독권 구매 (8,200M / 30일)</label>
            <p className="pang-charge-desc">스트리머 닉네임/아이디를 검색해서 선택한 뒤 구매할 수 있습니다.</p>
            <div className="mileage-shop-row">
              <input
                type="text"
                placeholder="스트리머 닉네임 또는 아이디"
                value={streamerQuery}
                onChange={(e) => {
                  setStreamerQuery(e.target.value);
                  setSelectedStreamer(null);
                }}
              />
            </div>
            {!!streamerResults.length && (
              <ul className="mileage-streamer-search-list">
                {streamerResults.map((s) => {
                  const displayName = s.nickname || s.username || `user#${s.id}`;
                  return (
                    <li key={s.id}>
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedStreamer(s);
                          setStreamerQuery(displayName);
                          setStreamerResults([]);
                        }}
                      >
                        <span className="name">{displayName}</span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
            {selectedStreamer && (
              <p className="pang-charge-desc" style={{ marginTop: 10, marginBottom: 0 }}>
                선택됨: <strong>{selectedStreamer.nickname || selectedStreamer.username}</strong>
              </p>
            )}
            <div className="mileage-shop-actions">
              <button type="button" className="btn-save-profile" onClick={buyMileageSubscription} disabled={shopLoading}>
                구독권 구매
              </button>
            </div>
          </div>

          <div className="profile-edit-field mileage-shop-item">
            <label>광고 제거권 (14,900M / 30일)</label>
            <p className="pang-charge-desc">
              현재 만료: {user?.adFreeUntil ? formatDate(user.adFreeUntil) : '미적용'}
            </p>
            <div className="mileage-shop-actions">
              <button type="button" className="btn-save-profile" onClick={buyAdFree} disabled={shopLoading}>
                광고 제거권 구매
              </button>
            </div>
          </div>

          {shopMsg && (
            <p className="profile-edit-msg" style={{ marginTop: 2 }}>
              {shopMsg}
            </p>
          )}

          <div>
            <h3 style={{ margin: '8px 0' }}>마일리지 내역</h3>
            <div className="pang-history-tabs">
              <button type="button" className={historyTab === 'charge' ? 'active' : ''} onClick={() => { setHistoryTab('charge'); setPage(0); }}>
                충전 내역
              </button>
              <button type="button" className={historyTab === 'usage' ? 'active' : ''} onClick={() => { setHistoryTab('usage'); setPage(0); }}>
                사용 내역
              </button>
            </div>

            {historyTab === 'charge' ? (
              <>
                <table className="pang-history-table" style={{ display: chargeHistory.length ? 'table' : 'none' }}>
                  <thead>
                    <tr>
                      <th>일시</th>
                      <th>구분</th>
                      <th>적립 마일리지</th>
                      <th>상세</th>
                    </tr>
                  </thead>
                  <tbody>
                    {chargeHistory.map((h, idx) => (
                      <tr key={h.id ?? idx}>
                        <td className="col-date">{formatDate(h.createdAt)}</td>
                        <td>이벤트 지급</td>
                        <td>{`+${(h.mileageCost ?? 0).toLocaleString()} M`}</td>
                        <td>운영자 지급</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="pang-history-empty" style={{ display: chargeHistory.length ? 'none' : 'block' }}>
                  마일리지 충전 내역이 없습니다.
                </div>
              </>
            ) : (
              <>
                <table className="pang-history-table" style={{ display: usageHistory.length ? 'table' : 'none' }}>
                  <thead>
                    <tr>
                      <th>일시</th>
                      <th>상품</th>
                      <th>사용 마일리지</th>
                      <th>상세</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usageHistory.map((h, idx) => (
                      <tr key={h.id ?? idx}>
                        <td className="col-date">{formatDate(h.createdAt)}</td>
                        <td>
                          {h.type === 'PANG'
                            ? '팡 구매'
                            : h.type === 'SUBSCRIPTION_TICKET'
                              ? '구독권'
                              : h.type === 'AD_FREE_30_DAYS'
                                ? '광고 제거 30일'
                                : h.type}
                        </td>
                        <td>{`-${(h.mileageCost ?? 0).toLocaleString()} M`}</td>
                        <td>
                          {h.type === 'PANG'
                            ? `${(h.pangAmount ?? 0).toLocaleString()}팡`
                            : h.type === 'SUBSCRIPTION_TICKET'
                              ? h.targetUserNickname || (h.targetUserId ? `스트리머 #${h.targetUserId}` : '-')
                              : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="pang-history-empty" style={{ display: usageHistory.length ? 'none' : 'block' }}>
                  마일리지 사용 내역이 없습니다.
                </div>
              </>
            )}
            {renderPagination()}
          </div>
        </div>
      </section>
    </>
  );
}
