import { useState } from 'react';
import StudioLayout from '../components/StudioLayout';

export default function StudioViewersBlocklist() {
  const [input, setInput] = useState('');
  const [search, setSearch] = useState('');

  return (
    <StudioLayout>
      <h1 className="page-title">활동 제한</h1>
        <div className="settings-card" style={{ marginBottom: 24 }}>
          <h2>활동 제한 추가</h2>
          <div className="settings-row">
            <div className="input-row">
              <input type="text" value={input} onChange={(e) => setInput(e.target.value)} placeholder="닉네임 또는 UID를 입력해주세요." style={{ flex: 1, minWidth: 200 }} />
              <button type="button" className="btn-copy" onClick={() => alert('추가는 준비 중입니다.')}>추가</button>
            </div>
            <p className="hint" style={{ color: 'var(--studio-accent)', marginTop: 8 }}>• 등록된 사용자는 내 채널 활동이 제한됩니다. 자세히 보기<br />• UID는 내 채널에서 확인 가능합니다. 자세히 보기<br />• 활동제한 시 송신 명의의 ID도 제한됩니다. 자세히 보기</p>
          </div>
        </div>
        <div className="settings-card">
          <h2>활동 제한 목록 0</h2>
          <div className="settings-row">
            <div className="input-row">
              <input type="text" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="닉네임을 입력해 주세요" style={{ flex: 1, minWidth: 200 }} />
              <button type="button" className="btn-copy">검색</button>
            </div>
          </div>
          <table className="data-table">
            <thead>
              <tr><th>활동 제한</th><th>등록자</th><th>등록일</th><th>제한 기간</th><th>제한 사유</th><th>액션</th></tr>
            </thead>
            <tbody>
              <tr><td colSpan={6} className="empty-msg">등록된 사용자가 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
    </StudioLayout>
  );
}
