import StudioLayout from '../components/StudioLayout';

export default function StudioViewersSubscribers() {
  return (
    <StudioLayout>
      <h1 className="page-title">구독자</h1>
        <div className="viewers-card">
          <div className="summary">내 채널 구독자 / 0</div>
          <div className="search-row">
            <input type="text" placeholder="닉네임을 입력해 주세요" />
            <button type="button" className="btn-search">검색</button>
          </div>
          <table className="data-table">
            <thead>
              <tr><th>닉네임</th><th>구독 시작일</th><th>기간</th><th>액션</th></tr>
            </thead>
            <tbody>
              <tr><td colSpan={4} className="empty-msg">아직은 고요합니다.</td></tr>
            </tbody>
          </table>
        </div>
    </StudioLayout>
  );
}
