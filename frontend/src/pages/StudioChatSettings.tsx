import StudioLayout from '../components/StudioLayout';

const ITEMS: { icon: string; title: string; value: string; link?: boolean }[] = [
  { icon: '🤖', title: '클린봇', value: '켜짐' },
  { icon: '💬', title: '채팅 이용 권한', value: '모든 사용자', link: true },
  { icon: '😀', title: '이모티콘 모드', value: '사용 안 함', link: true },
  { icon: '🕐', title: '저속 모드', value: '사용 안 함', link: true },
  { icon: '🔓', title: '활동 제한 해제 요청', value: '0건' },
  { icon: '📊', title: '주간 후원 랭킹', value: '노출', link: true },
];

export default function StudioChatSettings() {
  return (
    <StudioLayout>
      <div className="chat-settings-card">
          <div className="card-head">
            <h1 className="card-title">채팅</h1>
            <div className="card-actions">
              <button type="button" className="card-action-btn" title="더보기" onClick={() => alert('더보기 메뉴는 준비 중입니다.')}>⋮</button>
              <button type="button" className="card-action-btn" title="새 창에서 열기" onClick={() => window.open('/studio/chat', '_blank')}>⤴</button>
            </div>
          </div>
          <ul className="chat-settings-list">
            {ITEMS.map((item) => (
              <li key={item.title}>
                {item.link ? (
                  <a
                    href="#"
                    className="row-link"
                    onClick={(e) => {
                      e.preventDefault();
                      alert(`${item.title} 설정은 준비 중입니다.`);
                    }}
                  >
                    <span className="row-icon" aria-hidden="true">{item.icon}</span>
                    <div className="row-body">
                      <div className="row-title">{item.title}</div>
                      <div className="row-value">{item.value}</div>
                    </div>
                    <span className="row-arrow">›</span>
                  </a>
                ) : (
                  <>
                    <span className="row-icon" aria-hidden="true">{item.icon}</span>
                    <div className="row-body">
                      <div className="row-title">{item.title}</div>
                      <div className="row-value">{item.value}</div>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>
        </div>
    </StudioLayout>
  );
}
