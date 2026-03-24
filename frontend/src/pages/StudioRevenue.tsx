import { useState, useEffect } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

/** 팡 정산 최소 금액 */
const MIN_WITHDRAW_PANG = 100_000;
/** 팡 정산 단위 */
const WITHDRAW_STEP_PANG = 10;

export default function StudioRevenue() {
  const [total, setTotal] = useState(0);
  const [withdrawn, setWithdrawn] = useState(0);
  const [withdrawable, setWithdrawable] = useState(0);
  const [commission, setCommission] = useState(30);
  const [amount, setAmount] = useState('');
  const [msg, setMsg] = useState('');
  const [loading, setLoading] = useState(true);

  const loadRevenue = () => {
    fetch(apiUrl('api/revenue/me'), { credentials: 'include' })
      .then((r) => r.json())
      .then((d) => {
        setTotal(d.totalReceivedPang ?? 0);
        setWithdrawn(d.totalWithdrawnPang ?? 0);
        setWithdrawable(d.withdrawablePang ?? 0);
        setCommission(d.commissionPercent ?? 30);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    loadRevenue();
  }, []);

  const net = amount && parseInt(amount, 10) > 0 ? Math.floor(parseInt(amount, 10) * (1 - commission / 100)) : 0;
  const won = Math.round(net * 1.2 * 10) / 10;

  const handleWithdraw = () => {
    const a = parseInt(amount, 10);
    if (!a || a < 1) {
      setMsg('1 팡 이상 입력해 주세요.');
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
          setMsg(`환전 신청되었습니다.${res.data.netPang != null ? ` (정산 팡: ${(res.data.netPang || 0).toLocaleString()})` : ''}`);
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
        <div className="revenue-card">
          <h3>팡 수익 현황</h3>
          <p className="revenue-fee">시청자 후원으로 받은 팡을 환전할 수 있습니다. 수수료 적용 후 정산됩니다. (일반 스트리머 30%, 파트너 스트리머 20%)</p>
          <p className="revenue-fee">정산은 최소 10만 팡부터 10팡 단위로 가능합니다.</p>
          {loading ? (
            <p className="revenue-fee">불러오는 중...</p>
          ) : (
            <>
              <div className="revenue-stats">
                <div className="revenue-stat">
                  <div className="label">총 받은 팡</div>
                  <div className="value">{total.toLocaleString()}</div>
                </div>
                <div className="revenue-stat">
                  <div className="label">이미 환전한 팡</div>
                  <div className="value">{withdrawn.toLocaleString()}</div>
                </div>
                <div className="revenue-stat">
                  <div className="label">환전 가능 팡</div>
                  <div className="value">{withdrawable.toLocaleString()}</div>
                </div>
              </div>
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
                <button type="button" className="btn-withdraw-all" onClick={() => setAmount(String(Math.floor(withdrawable / WITHDRAW_STEP_PANG) * WITHDRAW_STEP_PANG))}>전체환전</button>
              </div>
              <p className="revenue-expected">수수료 제외 예상 정산: <strong>{net.toLocaleString()}</strong> 팡 (<strong>{won.toLocaleString()}</strong>원)</p>
              {msg && <p className={`revenue-msg ${msg.includes('실패') || msg.includes('오류') || msg.includes('이상') ? 'err' : 'ok'}`}>{msg}</p>}
            </>
          )}
        </div>
    </StudioLayout>
  );
}
