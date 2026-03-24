import RandomMatchChatBody from './RandomMatchChatBody';

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function RandomMatchChatModal({ open, onClose }: Props) {
  if (!open) return null;

  return (
    <div className="random-match-chat-modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="random-match-chat-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="random-match-chat-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="random-match-chat-modal-header">
          <h2 id="random-match-chat-modal-title">랜덤채팅</h2>
          <button type="button" className="random-match-chat-modal-close" aria-label="닫기" onClick={onClose}>
            <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden>
              <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
            </svg>
          </button>
        </header>
        <RandomMatchChatBody enabled={open} onClose={onClose} />
      </div>
    </div>
  );
}
