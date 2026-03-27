import { useEffect, useRef, useState } from 'react';
import type { FormEvent, KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import Layout from '../components/Layout';
import ChatbotMessageContent from '../components/ChatbotMessageContent';
import { sendChatMessage } from '../api/chat';
import { CHATBOT_STARTER_PROMPTS } from '../constants/chatbotStarterPrompts';
import { CHATBOT_SUGGESTION_CATEGORIES } from '../constants/chatbotSuggestions';
import { deriveChatbotActions, type ChatbotAction } from '../utils/chatbotActions';
import './Chatbot.css';

type Message = {
  role: 'assistant' | 'user';
  text: string;
  time: string;
  actions?: ChatbotAction[];
};

function formatTime() {
  return new Date().toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function Chatbot() {
  const endRef = useRef<HTMLDivElement | null>(null);
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      text: '필요한 기능만 보내 주세요.\n\n📌 바로 도와드릴 수 있는 내용\n- 전적 검색\n- 계정 연동\n- 채팅과 매칭\n- 후원과 스튜디오',
      time: formatTime(),
      actions: [
        { label: '전적 검색으로 이동', to: '/records' },
        { label: '외부 계정 연동', to: '/profile/account-links' },
      ],
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, loading]);

  const submitMessage = async (rawMessage: string) => {
    const message = rawMessage.trim();
    if (!message || loading) return;

    setMessages((current) => [...current, { role: 'user', text: message, time: formatTime() }]);
    setInput('');
    setError('');
    setLoading(true);

    try {
      const response = await sendChatMessage(message);
      const reply = response.reply || '답변을 생성하지 못했습니다.';
      setMessages((current) => [
        ...current,
        {
          role: 'assistant',
          text: reply,
          time: formatTime(),
          actions: deriveChatbotActions(`${message}\n${reply}`),
        },
      ]);
    } catch (err) {
      const messageText = err instanceof Error ? err.message : '챗봇 응답을 불러오지 못했습니다.';
      setError(messageText);
      setMessages((current) => [
        ...current,
        {
          role: 'assistant',
          text: messageText,
          time: formatTime(),
          actions: deriveChatbotActions(message),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await submitMessage(input);
  };

  const handleKeyDown = async (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      await submitMessage(input);
    }
  };

  return (
    <Layout>
      <div className="chatbot-page">
        <section className="chatbot-hero">
          <div className="chatbot-hero-copy">
            <span className="chatbot-eyebrow">AI ASSISTANT</span>
            <h1>GameMatcher 사용을 빠르게 도와주는 챗봇</h1>
            <p>
              전적 검색, 매칭, 후원, 계정 연동, 커뮤니티 이용법까지 서비스 흐름 기준으로 바로 안내합니다.
              필요한 메뉴가 있으면 답변 아래에서 바로 이동할 수 있습니다.
            </p>
            <div className="chatbot-pill-row">
              <span>고정 질문 박스</span>
              <span>실무형 안내</span>
              <span>바로가기 추천</span>
            </div>
          </div>

          <div className="chatbot-hero-panel">
            <strong>바로 가기</strong>
            <div className="chatbot-link-list">
              <Link to="/">홈으로 이동</Link>
              <Link to="/records">전적 페이지 보기</Link>
              <Link to="/profile/account-links">외부 계정 연동</Link>
              <Link to="/group-chat">단체채팅 보기</Link>
            </div>
          </div>
        </section>

        <section className="chatbot-body">
          <aside className="chatbot-sidebar">
            <section className="chatbot-card">
              <div className="chatbot-card-head">
                <span>QUICK LINKS</span>
                <strong>자주 찾는 메뉴</strong>
              </div>
              <div className="chatbot-action-list">
                <Link to="/records" className="chatbot-action-button">
                  <strong>전적 검색</strong>
                  <span>LoL, TFT, Valorant, PUBG, Overwatch 2, CS2 전적 확인</span>
                </Link>
                <Link to="/profile/account-links" className="chatbot-action-button">
                  <strong>외부 계정 연동</strong>
                  <span>Discord, Steam, Blizzard, Riot 연동 상태 확인과 연결</span>
                </Link>
                <Link to="/dm" className="chatbot-action-button">
                  <strong>친구와 1:1 채팅</strong>
                  <span>친구 기반 DM, 최근 대화, 읽지 않은 메시지 확인</span>
                </Link>
                <Link to="/studio" className="chatbot-action-button">
                  <strong>스튜디오</strong>
                  <span>방송 시작, 알림, 채팅, 수익 관련 화면 이동</span>
                </Link>
              </div>
            </section>

            <section className="chatbot-card">
              <div className="chatbot-card-head">
                <span>QUESTION SET</span>
                <strong>기능별 추천 질문</strong>
              </div>
              <div className="chatbot-category-list">
                {CHATBOT_SUGGESTION_CATEGORIES.map((category) => (
                  <article key={category.id} className="chatbot-category-card">
                    <div className="chatbot-category-head">
                      <strong>{category.title}</strong>
                      <span>{category.description}</span>
                    </div>
                    <div className="chatbot-category-prompts">
                      {category.prompts.map((item) => (
                        <button
                          key={`${category.id}-${item.label}`}
                          type="button"
                          className="chatbot-prompt-chip"
                          onClick={() => submitMessage(item.prompt)}
                          disabled={loading}
                        >
                          {item.label}
                        </button>
                      ))}
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </aside>

          <section className="chatbot-shell">
            <div className="chatbot-shell-header">
              <div>
                <span className="chatbot-shell-label">GM MATE</span>
                <h2>서비스 안내 채팅</h2>
              </div>
              <div className="chatbot-status">
                <span className={`chatbot-status-dot ${loading ? 'is-loading' : ''}`} />
                <span>{loading ? '응답 생성 중' : '대기 중'}</span>
              </div>
            </div>

            <div className="chatbot-messages">
              {messages.map((message, index) => (
                <article
                  key={`${message.role}-${message.time}-${index}`}
                  className={`chatbot-bubble ${message.role === 'user' ? 'is-user' : 'is-assistant'}`}
                >
                  <div className="chatbot-bubble-top">
                    <span>{message.role === 'user' ? '사용자' : 'GM MATE'}</span>
                    <span>{message.time}</span>
                  </div>
                  <ChatbotMessageContent text={message.text} className="chatbot-message-content" />
                  {message.role === 'assistant' && index === 0 ? (
                    <div className="chatbot-starter-grid">
                      {CHATBOT_STARTER_PROMPTS.map((item) => (
                        <button
                          key={item.label}
                          type="button"
                          className="chatbot-starter-card"
                          onClick={() => submitMessage(item.prompt)}
                          disabled={loading}
                        >
                          <strong>{item.label}</strong>
                          <span>바로 질문하기</span>
                        </button>
                      ))}
                    </div>
                  ) : null}
                  {message.role === 'assistant' && message.actions?.length ? (
                    <div className="chatbot-message-actions">
                      {message.actions.map((action) => (
                        <Link key={`${action.to}-${action.label}`} to={action.to} className="chatbot-message-action-link">
                          {action.label}
                        </Link>
                      ))}
                    </div>
                  ) : null}
                </article>
              ))}

              {loading ? (
                <article className="chatbot-bubble is-assistant">
                  <div className="chatbot-bubble-top">
                    <span>GM MATE</span>
                    <span>...</span>
                  </div>
                  <ChatbotMessageContent text="질문에 맞는 안내를 정리하고 있습니다." className="chatbot-message-content" />
                </article>
              ) : null}
              <div ref={endRef} />
            </div>

            <form className="chatbot-composer" onSubmit={handleSubmit}>
              <textarea
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="예: 발로란트 태그는 어떻게 입력해?, 매칭은 어디서 시작해?, 후원은 어떻게 해?"
                rows={4}
              />
              <div className="chatbot-composer-footer">
                <span>Enter 전송, Shift + Enter 줄바꿈</span>
                <button type="submit" disabled={loading || !input.trim()}>
                  {loading ? '전송 중...' : '보내기'}
                </button>
              </div>
            </form>

            {error ? <div className="chatbot-error">{error}</div> : null}
          </section>
        </section>
      </div>
    </Layout>
  );
}
