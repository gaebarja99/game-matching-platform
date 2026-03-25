import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { sendChatMessage } from '../api/index.js'
import styles from './ChatbotPage.module.css'

const QUICK_ACTIONS = [
  { title: '전적 검색', prompt: '전적 검색은 어떻게 하나요?', description: '게임별 입력값과 검색 흐름 안내' },
  { title: '계정 연동', prompt: '계정 연동은 어디서 하나요?', description: 'Discord, Steam, Blizzard, Riot 연결 안내' },
  { title: '커뮤니티', prompt: '커뮤니티 글 쓰는 방법 알려줘', description: '게시글 작성과 카테고리 이용법 안내' },
  { title: '신고·차단', prompt: '신고와 차단 기능은 어디서 쓰나요?', description: '안전 기능 위치와 사용 흐름 안내' },
]

const GUIDE_ITEMS = [
  { label: '전적 검색', text: '게임을 고르고 닉네임, 태그, 플랫폼 같은 필드를 채우면 최근 경기와 통계를 볼 수 있어요.' },
  { label: '계정 연동', text: '외부 플랫폼 계정을 연결해 프로필 확장과 기능 연동을 준비할 수 있어요.' },
  { label: '커뮤니티', text: '공지, 질문, 자유 글을 보고 작성하면서 게임별 정보를 나눌 수 있어요.' },
]

function formatTime() {
  return new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

export default function ChatbotPage() {
  const messagesEndRef = useRef(null)
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      text: '안녕하세요. GameMatcher 챗봇입니다. 전적 검색, 계정 연동, 커뮤니티 사용법을 빠르게 안내해드릴게요.',
      time: formatTime(),
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, loading])

  const statusText = useMemo(
    () => (loading ? '로컬 AI가 답변을 정리하는 중입니다.' : 'Ollama 기반 로컬 챗봇이 응답합니다.'),
    [loading]
  )

  const submitMessage = async (rawMessage) => {
    const message = rawMessage.trim()
    if (!message || loading) return

    setMessages((current) => [
      ...current,
      { role: 'user', text: message, time: formatTime() },
    ])
    setInput('')
    setLoading(true)
    setError(null)

    try {
      const response = await sendChatMessage(message)
      setMessages((current) => [
        ...current,
        {
          role: 'assistant',
          text: response.reply || '답변을 받지 못했습니다.',
          time: formatTime(),
        },
      ])
    } catch (err) {
      const nextError = err.response?.data?.message || '챗봇 응답을 불러오지 못했습니다.'
      setError(nextError)
      setMessages((current) => [
        ...current,
        { role: 'assistant', text: nextError, time: formatTime() },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    await submitMessage(input)
  }

  const handleKeyDown = async (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      await submitMessage(input)
    }
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}>AI ASSISTANT</p>
          <h1 className={styles.title}>웹 안에서 바로 쓰는 GameMatcher 챗봇</h1>
          <p className={styles.description}>
            어디를 눌러야 할지 헷갈릴 때, 검색 입력값이 애매할 때, 계정 연동이나 커뮤니티 사용법이 궁금할 때 바로 물어보세요.
          </p>
          <div className={styles.heroMeta}>
            <span className={styles.metaPill}>Local AI</span>
            <span className={styles.metaPill}>Ollama</span>
            <span className={styles.metaPill}>GameMatcher Guide</span>
          </div>
        </div>

        <div className={styles.heroPanel}>
          <div className={styles.panelHeader}>
            <strong>빠른 시작</strong>
            <span>{statusText}</span>
          </div>
          <div className={styles.guideGrid}>
            {GUIDE_ITEMS.map((item) => (
              <article key={item.label} className={styles.guideCard}>
                <span className={styles.guideLabel}>{item.label}</span>
                <p>{item.text}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className={styles.workspace}>
        <aside className={styles.sidebar}>
          <div className={styles.sidebarCard}>
            <div className={styles.sidebarHead}>
              <span className={styles.sidebarBadge}>추천 질문</span>
              <strong>바로 눌러서 시작</strong>
            </div>
            <div className={styles.actionList}>
              {QUICK_ACTIONS.map((item) => (
                <button
                  key={item.title}
                  type="button"
                  className={styles.quickAction}
                  onClick={() => submitMessage(item.prompt)}
                  disabled={loading}
                >
                  <strong>{item.title}</strong>
                  <span>{item.description}</span>
                </button>
              ))}
            </div>
          </div>

          <div className={styles.sidebarCard}>
            <div className={styles.sidebarHead}>
              <span className={styles.sidebarBadge}>바로 가기</span>
              <strong>주요 메뉴</strong>
            </div>
            <div className={styles.linkRail}>
              <Link to="/search" className={styles.routeLink}>전적 검색 가기</Link>
              <Link to="/community" className={styles.routeLink}>커뮤니티 가기</Link>
              <Link to="/connections" className={styles.routeLink}>계정 연동 가기</Link>
              <Link to="/me" className={styles.routeLink}>내 정보 가기</Link>
            </div>
          </div>
        </aside>

        <section className={styles.chatShell}>
          <div className={styles.chatHeader}>
            <div>
              <p className={styles.chatEyebrow}>GM BOT</p>
              <h2>서비스 안내 챗봇</h2>
            </div>
            <div className={styles.chatStatus}>
              <span className={styles.statusDot} />
              <span>{loading ? '답변 생성 중' : '응답 가능'}</span>
            </div>
          </div>

          <div className={styles.messages}>
            {messages.map((message, index) => (
              <article
                key={`${message.role}-${index}-${message.time}`}
                className={`${styles.bubble} ${message.role === 'user' ? styles.userBubble : styles.assistantBubble}`}
              >
                <div className={styles.bubbleTop}>
                  <span className={styles.role}>{message.role === 'user' ? '나' : 'GM BOT'}</span>
                  <span className={styles.time}>{message.time}</span>
                </div>
                <p>{message.text}</p>
              </article>
            ))}

            {loading && (
              <article className={`${styles.bubble} ${styles.assistantBubble}`}>
                <div className={styles.bubbleTop}>
                  <span className={styles.role}>GM BOT</span>
                  <span className={styles.time}>...</span>
                </div>
                <div className={styles.typingRow}>
                  <span className={styles.typingDot} />
                  <span className={styles.typingDot} />
                  <span className={styles.typingDot} />
                </div>
                <p className={styles.loadingText}>질문에 맞는 안내를 정리하고 있습니다.</p>
              </article>
            )}
            <div ref={messagesEndRef} />
          </div>

          <form className={styles.composer} onSubmit={handleSubmit}>
            <textarea
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="예: 발로란트 전적 검색할 때 닉네임이랑 태그는 어디에 넣나요?"
              rows={4}
            />
            <div className={styles.composerFooter}>
              <div className={styles.helperBlock}>
                <span className={styles.helperTitle}>입력 팁</span>
                <span className={styles.helperText}>Enter로 전송, Shift + Enter로 줄바꿈</span>
              </div>
              <button type="submit" className={styles.sendButton} disabled={loading || !input.trim()}>
                {loading ? '생성 중...' : '보내기'}
              </button>
            </div>
          </form>

          {error && <div className={styles.errorBox}>{error}</div>}
        </section>
      </section>
    </div>
  )
}
