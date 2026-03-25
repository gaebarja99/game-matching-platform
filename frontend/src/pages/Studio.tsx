import { Link } from 'react-router-dom';
import StudioLayout from '../components/StudioLayout';

export default function Studio() {
  return (
    <StudioLayout>
      <div className="dashboard-grid">
        <div className="dashboard-card">
          <h3>방송 시작하기</h3>
          <div className="step">
            <div className="step-num">1단계</div>
            <div className="step-desc">스트리밍 소프트웨어를 다운로드 하세요.</div>
            <div className="step-options">
              <a
                href="https://obsproject.com/ko"
                target="_blank"
                rel="noopener noreferrer"
                className="step-option"
              >
                <span className="opt-icon">OBS</span>
                Open Broadcaster Software
              </a>
            </div>
          </div>
          <div className="step">
            <div className="step-num">2단계</div>
            <div className="step-desc">스트림 키를 소프트웨어에 붙여 넣어주세요.</div>
            <div className="step-desc" style={{ fontSize: '0.85rem' }}>
              스트림 키는 방송 관리 &gt; 방송하기에서 방송 등록 후 확인 가능합니다.
            </div>
          </div>
          <div className="step">
            <div className="step-num">3단계</div>
            <div className="step-desc">
              스트리밍 소프트웨어에서 방송을 시작하면 라이브 방송이 진행됩니다. 방송 시작과 종료를
              스트리밍 소프트웨어에서 진행해주세요.
            </div>
          </div>
          <Link to="/studio/live" className="btn-primary">
            방송하기
          </Link>
        </div>

        <div className="dashboard-card highlight">
          <h3>방송 정보 설정 가이드</h3>
          <div className="guide-item">
            <div className="gi-icon">✏️</div>
            <div>
              <div className="gi-title">방송 제목</div>
              <div className="gi-desc">
                매력적인 제목으로 시청자의 관심을 유도해보세요. 시청자가 방송을 찾을 때 사용할 만한
                키워드를 넣는 것이 좋습니다.
              </div>
            </div>
          </div>
          <div className="guide-item">
            <div className="gi-icon">📦</div>
            <div>
              <div className="gi-title">카테고리</div>
              <div className="gi-desc">
                시청자가 쉽게 찾을 수 있도록 진행 중인 방송 카테고리(게임)를 추가하세요.
              </div>
            </div>
          </div>
          <div className="guide-item">
            <div className="gi-icon">🖼️</div>
            <div>
              <div className="gi-title">미리보기 이미지</div>
              <div className="gi-desc">
                진행 중인 방송을 설명할 수 있는 사진을 업로드하세요. 시청자의 관심을 끄는 이미지가
                좋습니다.
              </div>
            </div>
          </div>
        </div>

        <div className="dashboard-card">
          <h3>동영상 업로드하기</h3>
          <p className="step-desc" style={{ marginBottom: 16 }}>
            내 동영상을 간편하게 업로드해보세요. 게시된 동영상이 얼마나 인기 있는지 분석해드립니다.
          </p>
          <button
            type="button"
            className="btn-primary"
            onClick={() => alert('동영상 업로드 기능은 준비 중입니다.')}
          >
            동영상 업로드
          </button>
        </div>

        <div className="dashboard-card">
          <h3>채팅 설정</h3>
          <p className="step-desc" style={{ marginBottom: 12 }}>
            내 방송 스타일에 맞게 채팅 기능을 설정할 수 있습니다.
          </p>
          <p className="step-desc" style={{ fontSize: '0.85rem', marginBottom: 16 }}>
            설정 가능 항목: 채팅 규칙, 금칙어 설정
          </p>
          <div className="btn-group">
            <Link to="/studio/chat" className="btn-secondary">
              채팅 설정
            </Link>
            <Link to="/studio/chat" className="btn-secondary">
              금칙어 설정
            </Link>
          </div>
        </div>

        <div className="dashboard-card" style={{ gridColumn: '1 / -1' }}>
          <h3>공지사항</h3>
          <ul className="notice-list">
            <li>
              GameMatcher 스튜디오 이용 안내 <span className="notice-date">2025.03</span>
            </li>
            <li>
              방송 시작하기 가이드 <span className="notice-date">2025.03</span>
            </li>
            <li>
              OBS 연동 방법 <span className="notice-date">2025.03</span>
            </li>
          </ul>
        </div>
      </div>
    </StudioLayout>
  );
}
