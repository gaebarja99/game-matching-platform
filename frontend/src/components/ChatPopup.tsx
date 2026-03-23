import type { ReactNode } from 'react';
import './ChatPopup.css';

export type ChatPopupProps = {
  title: string;
  onClose: () => void;
  children: ReactNode;
  /** 접근성용 id (헤더 제목과 연결) */
  titleId?: string;
};

/**
 * 우하단 플로팅 위젯용 공통 규격 팝업 셸.
 * 스타일은 `.chat-popup-container` (ChatPopup.css)에서 정의합니다.
 */
export default function ChatPopup({ title, onClose, children, titleId = 'chat-popup-title' }: ChatPopupProps) {
  return (
    <div className="chat-popup-container" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <header className="chat-popup-header">
        <h2 id={titleId} className="chat-popup-title">
          {title}
        </h2>
        <button type="button" className="chat-popup-close" aria-label="닫기" onClick={onClose}>
          <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden>
            <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
          </svg>
        </button>
      </header>
      <div className="chat-popup-body">{children}</div>
    </div>
  );
}
