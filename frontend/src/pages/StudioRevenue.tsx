import { useEffect, useMemo, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

const MIN_WITHDRAW_PANG = 10;
const WITHDRAW_STEP_PANG = 10;

function floorToWithdrawStep(value: number) {
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.floor(value / WITHDRAW_STEP_PANG) * WITHDRAW_STEP_PANG;
}

interface WithdrawalRow {
  id?: number;
  amountPang?: number;
  commissionPercent?: number;
  commissionPang?: number;
  netPang?: number;
  settlementWon?: number;
  createdAt?: string;
}

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export default function StudioRevenue() {
  const [donationPang, setDonationPang] = useState(0);
  const [requestablePang, setRequestablePang] = useState(0);
  const [subscriptionPang, setSubscriptionPang] = useState(0);
  const [subscriptionWithdrawablePang, setSubscriptionWithdrawablePang] = useState(0);
  const [subscriptionWon, setSubscriptionWon] = useState(0);
  const [withdrawn, setWithdrawn] = useState(0);
  const [commission, setCommission] = useState(30);
  const [withdrawalHistory, setWithdrawalHistory] = useState<WithdrawalRow[]>([]);
  const [amount, setAmount] = useState('');
  const [msg, setMsg] = useState('');
  const [loading, setLoading] = useState(true);

  const loadRevenue = () => {
    setLoading(true);
    fetch(apiUrl('api/revenue/me'), { credentials: 'include' })
      .then((r) => r.json())
      .then((d) => {
        setDonationPang(Number(d.donationPang ?? 0));
        setRequestablePang(Number(d.requestablePang ?? d.settlementTargetPang ?? 0));
        setSubscriptionPang(Number(d.subscriptionPang ?? 0));
        setSubscriptionWithdrawablePang(Number(d.subscriptionWithdrawablePang ?? 0));
        setSubscriptionWon(Number(d.subscriptionRevenueWon ?? 0));
        setWithdrawn(Number(d.totalWithdrawnPang ?? 0));
        setCommission(Number(d.commissionPercent ?? 30));
        setWithdrawalHistory(Array.isArray(d.withdrawalHistory) ? d.withdrawalHistory : []);
      })
      .catch(() => {
        setMsg('정산 정보를 불러오지 못했습니다.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadRevenue();
  }, []);

  const parsedAmount = useMemo(() => {
    const raw = Number.parseInt(amount, 10);
    return Number.isFinite(raw) ? raw : 0;
  }, [amount]);

  const normalizedAmount = useMemo(() => floorToWithdrawStep(parsedAmount), [parsedAmount]);
  const totalSettlementWon = useMemo(
    () => withdrawalHistory.reduce((sum, row) => sum + Number(row.settlementWon ?? 0), 0),
    [withdrawalHistory]
  );
  const net = normalizedAmount > 0 ? Math.floor(normalizedAmount * (1 - commission / 100)) : 0;
  const won = Math.round(net * 1.2);

  const handleWithdraw = () => {
    const targetAmount = normalizedAmount;
    if (!targetAmount || targetAmount < MIN_WITHDRAW_PANG) {
      setMsg('최소 10팡 이상 입력해 주세요.');
      return;
    }

    setMsg('');
    fetch(apiUrl('api/revenue/withdraw'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ amount: targetAmount }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d })))
      .then((res) => {
        if (res.ok) {
          setMsg(
            `환전 요청이 접수되었습니다.${res.data.netPang != null ? ` (정산 팡 ${Number(res.data.netPang || 0).toLocaleString()})` : ''}`
          );
          setAmount('');
          loadRevenue();
        } else {
          setMsg(res.data?.message || '환전 요청에 실패했습니다.');
        }
      })
      .catch(() => setMsg('네트워크 오류가 발생했습니다.'));
  };

  return (
    <StudioLayout>
      <h1 className="page-title">수익 창출</h1>

      <div className="revenue-panels">
        <section className="revenue-card revenue-card-half">
          <h3>후원 수익 현황</h3>
          <p className="revenue-fee">후원으로 발생한 수익 현황입니다.</p>
          {loading ? (
            <p className="revenue-fee">불러오는 중...</p>
          ) : (
            <div className="revenue-stats revenue-stats-compact">
              <div className="revenue-stat">
                <div className="label">후원 수익 팡</div>
                <div className="value">{donationPang.toLocaleString()}</div>
              </div>
              <div className="revenue-stat">
                <div className="label">이미 환전한 팡</div>
                <div className="value">{withdrawn.toLocaleString()}</div>
              </div>
              <div className="revenue-stat">
                <div className="label">환전 가능한 팡</div>
                <div className="value">{requestablePang.toLocaleString()}</div>
              </div>
            </div>
          )}
        </section>

        <section className="revenue-card revenue-card-half">
          <h3>구독권 수익 현황</h3>
          <p className="revenue-fee">구독권 수익의 원화 기준 금액과 환산 팡입니다.</p>
          {loading ? (
            <p className="revenue-fee">불러오는 중...</p>
          ) : (
            <div className="revenue-stats revenue-stats-compact">
              <div className="revenue-stat">
                <div className="label">구독권 수익(원)</div>
                <div className="value">{subscriptionWon.toLocaleString()}원</div>
              </div>
              <div className="revenue-stat">
                <div className="label">구독권 환산 팡</div>
                <div className="value">{subscriptionPang.toLocaleString()}</div>
              </div>
              <div className="revenue-stat">
                <div className="label">환전 가능한 팡</div>
                <div className="value">{subscriptionWithdrawablePang.toLocaleString()}</div>
              </div>
            </div>
          )}
        </section>
      </div>

      <div className="revenue-panels">
        <div className="revenue-card revenue-card-half">
          <h3>정산 신청</h3>
          <p className="revenue-fee">수수료 적용 후 정산됩니다. 일반 스트리머 30%, 파트너 스트리머 20%</p>
          <p className="revenue-fee">후원은 최소 100,000팡부터, 구독권 수익은 최소 10팡부터 10팡 단위로 정산 가능합니다.</p>
          {loading ? (
            <p className="revenue-fee">불러오는 중...</p>
          ) : (
            <>
              <div className="revenue-fee">
                적용 수수료: <strong>{commission}%</strong>
              </div>
              <div className="revenue-withdraw-row">
                <input
                  type="number"
                  min={MIN_WITHDRAW_PANG}
                  step={WITHDRAW_STEP_PANG}
                  placeholder={`환전할 팡 (최소 ${MIN_WITHDRAW_PANG.toLocaleString()}, 10단위)`}
                  value={amount}
                  onChange={(e) => {
                    const next = e.target.value.replace(/[^\d]/g, '');
                    setAmount(next);
                  }}
                />
                <button type="button" className="btn-withdraw" onClick={handleWithdraw}>
                  환전 신청
                </button>
                <button
                  type="button"
                  className="btn-withdraw-all"
                  onClick={() => setAmount(String(floorToWithdrawStep(requestablePang)))}
                >
                  전체환전
                </button>
              </div>
              <p className="revenue-fee">
                실제 신청 금액은 10단위로 자동 맞춤됩니다:
                <strong> {normalizedAmount.toLocaleString()}팡</strong>
              </p>
              <p className="revenue-expected">
                수수료 제외 예상 정산: <strong>{net.toLocaleString()}</strong>팡(<strong>{won.toLocaleString()}</strong>원)
              </p>
              {msg && (
                <p className={`revenue-msg ${msg.includes('실패') || msg.includes('오류') || msg.includes('최소') ? 'err' : 'ok'}`}>
                  {msg}
                </p>
              )}
            </>
          )}
        </div>

        <div className="revenue-card revenue-card-half">
          <h3>총 정산 금액</h3>
          <p className="revenue-fee">지금까지 정산 완료된 누적 금액입니다.</p>
          {loading ? (
            <p className="revenue-fee">불러오는 중...</p>
          ) : (
            <div className="revenue-stats revenue-stats-compact">
              <div className="revenue-stat">
                <div className="label">누적 정산 금액</div>
                <div className="value">{totalSettlementWon.toLocaleString()}원</div>
              </div>
              <div className="revenue-stat">
                <div className="label">정산 완료 건수</div>
                <div className="value">{withdrawalHistory.length.toLocaleString()}건</div>
              </div>
              <div className="revenue-stat">
                <div className="label">최근 정산 금액</div>
                <div className="value">{Number(withdrawalHistory[0]?.settlementWon ?? 0).toLocaleString()}원</div>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="revenue-card revenue-history-card">
        <h3>정산 내역</h3>
        {loading ? (
          <p className="revenue-fee">불러오는 중...</p>
        ) : withdrawalHistory.length === 0 ? (
          <p className="revenue-fee">정산 요청 내역이 없습니다.</p>
        ) : (
          <div className="revenue-history-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>요청일</th>
                  <th>요청 팡</th>
                  <th>수수료</th>
                  <th>실정산 팡</th>
                  <th>정산 금액</th>
                </tr>
              </thead>
              <tbody>
                {withdrawalHistory.map((row) => (
                  <tr key={row.id ?? `${row.createdAt}-${row.amountPang}`}>
                    <td>{formatDate(row.createdAt)}</td>
                    <td>{Number(row.amountPang ?? 0).toLocaleString()}팡</td>
                    <td>
                      {Number(row.commissionPang ?? 0).toLocaleString()}팡 ({Number(row.commissionPercent ?? 0)}%)
                    </td>
                    <td>{Number(row.netPang ?? 0).toLocaleString()}팡</td>
                    <td>{Number(row.settlementWon ?? 0).toLocaleString()}원</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </StudioLayout>
  );
}
