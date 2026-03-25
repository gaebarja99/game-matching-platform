import React from 'react'
import { Link } from 'react-router-dom'
import styles from './HomePage.module.css'

const GAMES = [
  { id: 'lol',       name: 'League of Legends', short: 'LoL',   color: '#c89b3c', emoji: '⚔️' },
  { id: 'tft',       name: 'Teamfight Tactics',  short: 'TFT',   color: '#9b59b6', emoji: '🎲' },
  { id: 'valorant',  name: 'Valorant',            short: 'VAL',   color: '#ff4655', emoji: '🎯' },
  { id: 'pubg',      name: '배틀그라운드',         short: 'PUBG',  color: '#f5a623', emoji: '🪖' },
  { id: 'overwatch', name: '오버워치',             short: 'OW2',   color: '#f99e1a', emoji: '🦸' },
  { id: 'apex',      name: 'Apex Legends',        short: 'APEX',  color: '#cd3333', emoji: '🔥' },
  { id: 'cs2',       name: 'Counter-Strike 2',    short: 'CS2',   color: '#4d9e4d', emoji: '💣' },
]

export default function HomePage() {
  return (
    <div className={styles.page}>
      {/* Hero */}
      <section className={styles.hero}>
        <div className={styles.heroGlow} />
        <h1 className={styles.heroTitle}>
          당신의 게임 전적을<br />
          <span>한 번에 검색하세요</span>
        </h1>
        <p className={styles.heroDesc}>
          LoL, TFT, Valorant, Steam, Blizzard — 모든 게임의 전적과 통계를 통합 조회
        </p>
        <div className={styles.heroActions}>
          <Link to="/search" className={styles.ctaPrimary}>
            전적 검색 시작 →
          </Link>
          <Link to="/community" className={styles.ctaSecondary}>
            커뮤니티 보기
          </Link>
        </div>
      </section>

      {/* Supported Games */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>지원 게임</h2>
        <div className={styles.gameGrid}>
          {GAMES.map(game => (
            <Link
              key={game.id}
              to={`/search?game=${game.id}`}
              className={styles.gameCard}
              style={{ '--card-color': game.color }}
            >
              <span className={styles.gameEmoji}>{game.emoji}</span>
              <span className={styles.gameName}>{game.name}</span>
              <span className={styles.gameShort}>{game.short}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>주요 기능</h2>
        <div className={styles.featureGrid}>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>🔍</div>
            <h3>통합 전적 검색</h3>
            <p>닉네임 하나로 여러 게임의 전적을 동시에 조회합니다.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>📊</div>
            <h3>상세 통계 분석</h3>
            <p>KDA, 승률, 최근 매치 히스토리를 한눈에 확인하세요.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>💬</div>
            <h3>게임 커뮤니티</h3>
            <p>게임별 게시판에서 정보를 공유하고 팀원을 찾아보세요.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>⚡</div>
            <h3>배치 검색</h3>
            <p>여러 플레이어를 한 번에 검색하는 배치 기능을 지원합니다.</p>
          </div>
        </div>
      </section>
    </div>
  )
}
