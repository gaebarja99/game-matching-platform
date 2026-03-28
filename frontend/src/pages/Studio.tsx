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
      </div>
    </StudioLayout>
  );
}
