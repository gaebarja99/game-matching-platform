import { useEffect, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

const MIN_WITHDRAW_PANG = 100_000;
const WITHDRAW_STEP_PANG = 10;

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
  const [donationWithdrawablePang, setDonationWithdrawablePang] = useState(0);
  const [subscriptionPang, setSubscriptionPang] = useState(0);
  const [subscriptionWon, setSubscriptionWon] = useState(0);
  const [total, setTotal] = useState(0);
  const [settlementTargetPang, setSettlementTargetPang] = useState(0);
  const [withdrawn, setWithdrawn] = useState(0);
  const [withdrawable, setWithdrawable] = useState(0);
  const [commission, setCommission] = useState(30);
  const [withdrawalHistory, setWithdrawalHistory] = useState<WithdrawalRow[]>([]);
  const [amount, setAmount] = useState('');
  const [msg, setMsg] = useState('');
  const [loading, setLoading] = useState(true);

  const loadRevenue = () => {
    fetch(apiUrl('api/revenue/me'), { credentials: 'include' })
      .then((r) => r.json())
      .then((d) => {
        setDonationPang(d.donationPang ?? 0);
        setDonationWithdrawablePang(d.donationWithdrawablePang ?? 0);
        setSubscriptionPang(d.subscriptionPang ?? 0);
        setSubscriptionWon(d.subscriptionRevenueWon ?? 0);
        setTotal(d.totalReceivedPang ?? 0);
        setSettlementTargetPang(d.settlementTargetPang ?? 0);
        setWithdrawn(d.totalWithdrawnPang ?? 0);
        setWithdrawable(d.withdrawablePang ?? 0);
        setCommission(d.commissionPercent ?? 30);
        setWithdrawalHistory(Array.isArray(d.withdrawalHistory) ? d.withdrawalHistory : []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    loadRevenue();
  }, []);

  const net = amount && parseInt(amount, 10) > 0 ? Math.floor(parseInt(amount, 10) * (1 - commission / 100)) : 0;
  const won = Math.round(net * 1.2);

  const handleWithdraw = () => {
    const a = parseInt(amount, 10);
    if (!a || a < 1) {
      setMsg('1 이상 입력해 주세요.');
      return;
    }
    setMsg('');
    fetch(apiUrl('api/revenue/withdraw'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ amount: a }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d })))
      .then((res) => {
        if (res.ok) {
          setMsg(`환전 신청되었습니다.${res.data.netPang != null ? ` (정산 팡 ${Number(res.data.netPang || 0).toLocaleString()})` : ''}`);
          setAmount('');
          loadRevenue();
        } else {
          setMsg(res.data?.message || '환전 신청에 실패했습니다.');
        }
      })
      .catch(() => setMsg('네트워크 오류'));
  };

  return (
    <StudioLayout>
      <h1 className="page-title">수익 창출</h1>

      <div className="revenue-panels">
        <section className="revenue-card revenue-card-half">
          <h3>팡 수익 현황</h3>
          <p className="revenue-fee">시청자 후원으로 발생한 팡 수익 현황입니다.</p>
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
                <div className="label">환전 가능 팡</div>
                <div className="value">{donationWithdrawablePang.toLocaleString()}</div>
              </div>
            </div>
          )}
        </section>

        <section className="revenue-card revenue-card-half">
          <h3>구독권 수익 현황</h3>
          <p className="revenue-fee">구독권 수익의 팡 환산값과 원화 기준 금액입니다.</p>
          {loading ? (
            <p className="revenue-fee">불러오는 중...</p>
          ) : (
            <div className="revenue-stats revenue-stats-compact">
              <div className="revenue-stat">
                <div className="label">구독권 환산 팡</div>
                <div className="value">{subscriptionPang.toLocaleString()}</div>
              </div>
              <div className="revenue-stat">
                <div className="label">구독권 수익(원)</div>
                <div className="value">{subscriptionWon.toLocaleString()}원</div>
              </div>
              <div className="revenue-stat">
                <div className="label">총 정산 대상 팡</div>
                <div className="value">{settlementTargetPang.toLocaleString()}</div>
              </div>
            </div>
          )}
        </section>
      </div>

      <div className="revenue-card">
        <h3>정산 신청</h3>
        <p className="revenue-fee">수수료 적용 후 정산됩니다. 일반 스트리머 30%, 파트너 스트리머 20%</p>
        <p className="revenue-fee">정산은 최소 10만 팡부터 10팡 단위로 가능합니다.</p>
        {loading ? (
          <p className="revenue-fee">불러오는 중...</p>
        ) : (
          <>
            <div className="revenue-fee">적용 수수료: <strong>{commission}%</strong></div>
            <div className="revenue-withdraw-row">
              <input
                type="number"
                min={MIN_WITHDRAW_PANG}
                step={WITHDRAW_STEP_PANG}
                placeholder={`환전할 팡 (최소 ${MIN_WITHDRAW_PANG.toLocaleString()}, 10단위)`}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
              <button type="button" className="btn-withdraw" onClick={handleWithdraw}>환전 신청</button>
              <button
                type="button"
                className="btn-withdraw-all"
                onClick={() => setAmount(String(Math.floor(withdrawable / WITHDRAW_STEP_PANG) * WITHDRAW_STEP_PANG))}
              >
                전체환전
              </button>
            </div>
            <p className="revenue-expected">
              수수료 제외 예상 정산: <strong>{net.toLocaleString()}</strong> 팡 (<strong>{won.toLocaleString()}</strong>원)
            </p>
            {msg && <p className={`revenue-msg ${msg.includes('실패') || msg.includes('오류') || msg.includes('이상') ? 'err' : 'ok'}`}>{msg}</p>}
          </>
        )}
      </div>

      <div className="revenue-card revenue-history-card">
        <h3>정산 내역</h3>
        {loading ? (
          <p className="revenue-fee">불러오는 중...</p>
        ) : withdrawalHistory.length === 0 ? (
          <p className="revenue-fee">정산 신청 내역이 없습니다.</p>
        ) : (
          <div className="revenue-history-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>신청일</th>
                  <th>신청 팡</th>
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
                    <td>{Number(row.commissionPang ?? 0).toLocaleString()}팡 ({Number(row.commissionPercent ?? 0)}%)</td>
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
