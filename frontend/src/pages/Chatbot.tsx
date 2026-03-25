import { useEffect, useRef, useState } from 'react';
import type { FormEvent, KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import Layout from '../components/Layout';
import { sendChatMessage } from '../api/chat';
import './Chatbot.css';

type Message = {
  role: 'assistant' | 'user';
  text: string;
  time: string;
};

const QUICK_ACTIONS = [
  {
    title: '전적 검색',
    prompt: '전적 검색은 어떻게 하면 돼?',
    description: '게임별로 어떤 정보를 입력해야 하는지 물어보기',
  },
  {
    title: '계정 연동',
    prompt: '계정 연동은 어디에서 할 수 있어?',
    description: 'Discord, Steam, Riot 같은 계정 연결 방법 보기',
  },
  {
    title: '커뮤니티',
    prompt: '커뮤니티에서 글 쓰는 방법 알려줘',
    description: '게시글 작성과 이용 흐름 안내받기',
  },
  {
    title: '신고/차단',
    prompt: '신고나 차단 기능은 어디에 있어?',
    description: '안전 기능 위치와 사용 흐름 확인하기',
  },
];

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
      text: '안녕하세요. GameMatcher 챗봇입니다. 전적 검색, 계정 연동, 커뮤니티 사용법처럼 서비스 이용 중 막히는 부분을 편하게 물어보세요.',
      time: formatTime(),
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
      setMessages((current) => [
        ...current,
        {
          role: 'assistant',
          text: response.reply || '답변을 생성하지 못했습니다.',
          time: formatTime(),
        },
      ]);
    } catch (err) {
      const messageText = err instanceof Error ? err.message : '챗봇 응답을 불러오지 못했습니다.';
      setError(messageText);
      setMessages((current) => [...current, { role: 'assistant', text: messageText, time: formatTime() }]);
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
            <h1>GameMatcher 이용을 빠르게 도와주는 챗봇</h1>
            <p>
              어디에서 무엇을 눌러야 하는지, 전적 검색에 어떤 값을 넣어야 하는지,
              계정 연동이나 신고 기능이 어디 있는지 한 번에 물어볼 수 있습니다.
            </p>
            <div className="chatbot-pill-row">
              <span>Ollama 연동</span>
              <span>빠른 안내</span>
              <span>서비스 사용 가이드</span>
            </div>
          </div>

          <div className="chatbot-hero-panel">
            <strong>바로 가기</strong>
            <div className="chatbot-link-list">
              <Link to="/">홈으로 이동</Link>
              <Link to="/records">전적 페이지 보기</Link>
              <Link to="/profile">내 프로필 보기</Link>
              <Link to="/group-chat">단체채팅 보기</Link>
            </div>
          </div>
        </section>

        <section className="chatbot-body">
          <aside className="chatbot-sidebar">
            <div className="chatbot-card">
              <div className="chatbot-card-head">
                <span>추천 질문</span>
                <strong>바로 시작하기</strong>
              </div>
              <div className="chatbot-action-list">
                {QUICK_ACTIONS.map((item) => (
                  <button
                    key={item.title}
                    type="button"
                    className="chatbot-action-button"
                    onClick={() => submitMessage(item.prompt)}
                    disabled={loading}
                  >
                    <strong>{item.title}</strong>
                    <span>{item.description}</span>
                  </button>
                ))}
              </div>
            </div>
          </aside>

          <section className="chatbot-shell">
            <div className="chatbot-shell-header">
              <div>
                <span className="chatbot-shell-label">GM BOT</span>
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
                    <span>{message.role === 'user' ? '나' : 'GM BOT'}</span>
                    <span>{message.time}</span>
                  </div>
                  <p>{message.text}</p>
                </article>
              ))}

              {loading ? (
                <article className="chatbot-bubble is-assistant">
                  <div className="chatbot-bubble-top">
                    <span>GM BOT</span>
                    <span>...</span>
                  </div>
                  <p>질문에 맞는 안내를 정리하고 있습니다.</p>
                </article>
              ) : null}
              <div ref={endRef} />
            </div>

            <form className="chatbot-composer" onSubmit={handleSubmit}>
              <textarea
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="예: 발로란트 전적 검색할 때 어떤 값을 넣어야 해?"
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
