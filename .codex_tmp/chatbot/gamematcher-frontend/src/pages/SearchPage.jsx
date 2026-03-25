import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { searchPlayer } from '../api/index.js'
import styles from './SearchPage.module.css'

const GAMES = [
  {
    id: 'lol',
    label: 'League of Legends',
    short: 'LoL',
    accent: '#2f80ed',
    fields: ['nickname', 'tag', 'count'],
    placeholders: { nickname: '소환사명', tag: 'KR1' },
    hint: '라이엇 계정 기준으로 닉네임과 태그를 입력해 주세요.',
    tagLabel: '태그',
  },
  {
    id: 'tft',
    label: 'Teamfight Tactics',
    short: 'TFT',
    accent: '#7c5cff',
    fields: ['nickname', 'tag', 'count'],
    placeholders: { nickname: '전략적 팀 전투 닉네임', tag: 'KR1' },
    hint: '라이엇 계정 기준 닉네임과 태그를 입력하면 최근 전적을 조회합니다.',
    tagLabel: '태그',
  },
  {
    id: 'valorant',
    label: 'Valorant',
    short: 'VAL',
    accent: '#ff4d67',
    fields: ['nickname', 'tag', 'valorant_region', 'count'],
    placeholders: { nickname: '플레이어명', tag: 'KR1' },
    hint: '닉네임, 태그, 지역을 선택해 발로란트 전적을 검색합니다.',
    tagLabel: '태그',
    regionOptions: [
      { value: 'kr', label: 'Korea' },
      { value: 'ap', label: 'Asia Pacific' },
      { value: 'na', label: 'North America' },
      { value: 'eu', label: 'Europe' },
      { value: 'latam', label: 'LATAM' },
      { value: 'br', label: 'Brazil' },
    ],
  },
  {
    id: 'pubg',
    label: 'PUBG',
    short: 'PUBG',
    accent: '#f0b429',
    fields: ['nickname', 'pubg_platform'],
    placeholders: { nickname: 'Steam 또는 Kakao 닉네임' },
    hint: '배틀그라운드는 플랫폼 선택이 중요합니다. 닉네임과 플랫폼을 같이 확인해 주세요.',
    platformOptions: [
      { value: 'steam', label: 'Steam' },
      { value: 'kakao', label: 'Kakao' },
    ],
  },
  {
    id: 'overwatch',
    label: 'Overwatch 2',
    short: 'OW2',
    accent: '#ff9b3d',
    fields: ['nickname', 'tag'],
    placeholders: { nickname: 'BattleTag 이름', tag: '1234' },
    hint: '오버워치는 BattleTag 이름과 숫자 태그를 함께 입력해 주세요.',
    tagLabel: '배틀태그',
  },
  {
    id: 'apex',
    label: 'Apex Legends',
    short: 'APEX',
    accent: '#ff6f61',
    fields: ['nickname', 'apex_platform'],
    placeholders: { nickname: 'EA 또는 Origin 닉네임' },
    hint: 'Apex는 Tracker API 승인 상태에 따라 조회가 제한될 수 있습니다.',
    platformOptions: [
      { value: 'PC', label: 'PC' },
      { value: 'PS4', label: 'PlayStation' },
      { value: 'X1', label: 'Xbox' },
    ],
  },
  {
    id: 'cs2',
    label: 'Counter-Strike 2',
    short: 'CS2',
    accent: '#61b15a',
    fields: ['nickname'],
    placeholders: { nickname: 'Steam64 ID 또는 Vanity URL' },
    hint: 'Steam 프로필 식별자를 입력하면 CS2 전적을 확인할 수 있습니다.',
  },
]

function formatWinRate(value) {
  if (value == null) return '-'
  const normalized = value > 1 ? value : value * 100
  return `${normalized.toFixed(0)}%`
}

function getInitials(name) {
  if (!name) return '?'
  return name.slice(0, 1).toUpperCase()
}

function WinBadge({ win }) {
  return (
    <span className={win ? styles.winBadge : styles.lossBadge}>
      {win ? 'WIN' : 'LOSS'}
    </span>
  )
}

function MatchRow({ match }) {
  const duration = match.playtime ? `${Math.floor(match.playtime / 60)}m` : null

  return (
    <div className={`${styles.matchRow} ${match.win ? styles.matchWin : styles.matchLoss}`}>
      <div className={styles.matchStatus}>
        <WinBadge win={match.win} />
        <span className={styles.matchMode}>{match.gameMode || 'Mode'}</span>
      </div>

      <div className={styles.matchCenter}>
        <strong className={styles.matchTitle}>{match.champion || match.agent || 'Unknown'}</strong>
        {match.kills != null && (
          <span className={styles.matchKda}>
            {match.kills} / {match.deaths} / {match.assists}
          </span>
        )}
      </div>

      <div className={styles.matchMeta}>
        {duration && <span className={styles.metaChip}>{duration}</span>}
        {match.cs != null && <span className={styles.metaChip}>CS {match.cs}</span>}
        {match.extras &&
          Object.entries(match.extras)
            .slice(0, 2)
            .map(([key, value]) => (
              <span key={key} className={styles.metaChip}>
                {key}: {value}
              </span>
            ))}
      </div>
    </div>
  )
}

function ResultPanel({ result, accent, title }) {
  const info = result.playerInfo || {}
  const stats = result.stats || {}

  const statCards = [
    { label: '총 게임', value: stats.totalGames ?? '-' },
    { label: '승률', value: formatWinRate(stats.winRate) },
    { label: '승리', value: stats.wins ?? '-' },
    { label: '평균 KDA', value: stats.avgKda != null ? Number(stats.avgKda).toFixed(2) : '-' },
    { label: '주력 픽', value: stats.mostUsedChampionOrAgent || '-' },
  ]

  return (
    <section className={styles.resultPanel} style={{ '--accent': accent }}>
      <div className={styles.resultHeader}>
        <div className={styles.profileBlock}>
          {info.avatarUrl ? (
            <img src={info.avatarUrl} alt="avatar" className={styles.avatar} />
          ) : (
            <div className={styles.avatarFallback}>{getInitials(info.gameName || result.nickname)}</div>
          )}

          <div className={styles.profileText}>
            <div className={styles.resultLabel}>{title}</div>
            <h2 className={styles.profileName}>
              {info.gameName || result.nickname || 'Unknown Player'}
              {info.tagLine && <span className={styles.tagLine}>#{info.tagLine}</span>}
            </h2>
            <div className={styles.rankRow}>
              {info.tier && <span className={styles.rankBadge}>{info.tier} {info.rank || ''}</span>}
              {info.lp != null && info.lp !== '' && <span className={styles.softBadge}>{info.lp} LP</span>}
              {info.summonerLevel && <span className={styles.softBadge}>Lv.{info.summonerLevel}</span>}
            </div>
          </div>
        </div>

        <div className={styles.resultStamp}>{result.game?.toUpperCase()}</div>
      </div>

      <div className={styles.statGrid}>
        {statCards.map((item) => (
          <div key={item.label} className={styles.statCard}>
            <span className={styles.statLabel}>{item.label}</span>
            <strong className={styles.statValue}>{item.value}</strong>
          </div>
        ))}
      </div>

      <div className={styles.resultBody}>
        <div className={styles.sectionCard}>
          <div className={styles.sectionHead}>
            <h3>플레이어 정보</h3>
          </div>
          <dl className={styles.infoList}>
            <div>
              <dt>게임명</dt>
              <dd>{info.gameName || '-'}</dd>
            </div>
            <div>
              <dt>태그</dt>
              <dd>{info.tagLine || '-'}</dd>
            </div>
            <div>
              <dt>티어</dt>
              <dd>{info.tier ? `${info.tier} ${info.rank || ''}`.trim() : '-'}</dd>
            </div>
            <div>
              <dt>레벨</dt>
              <dd>{info.summonerLevel || '-'}</dd>
            </div>
          </dl>
        </div>

        <div className={styles.sectionCard}>
          <div className={styles.sectionHead}>
            <h3>최근 매치</h3>
            <span>{result.matches?.length || 0} games</span>
          </div>
          {result.matches?.length ? (
            <div className={styles.matchList}>
              {result.matches.map((match, index) => (
                <MatchRow key={match.matchId || `${index}-${match.gameMode || 'match'}`} match={match} />
              ))}
            </div>
          ) : (
            <p className={styles.emptyMatches}>표시할 최근 전적이 없습니다.</p>
          )}
        </div>
      </div>
    </section>
  )
}

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialGame = searchParams.get('game') || 'lol'

  const [gameId, setGameId] = useState(initialGame)
  const [nickname, setNickname] = useState('')
  const [tagLine, setTagLine] = useState('')
  const [region, setRegion] = useState('kr')
  const [platform, setPlatform] = useState('')
  const [count, setCount] = useState(5)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const resultRef = useRef(null)

  const game = useMemo(() => GAMES.find((item) => item.id === gameId) || GAMES[0], [gameId])

  useEffect(() => {
    if (game.platformOptions?.length) {
      setPlatform(game.platformOptions[0].value)
    } else {
      setPlatform('')
    }
  }, [game])

  useEffect(() => {
    setSearchParams({ game: gameId }, { replace: true })
  }, [gameId, setSearchParams])

  const handleGameChange = (nextGameId) => {
    setGameId(nextGameId)
    setResult(null)
    setError(null)
  }

  const clearInputs = () => {
    setNickname('')
    setTagLine('')
    setError(null)
    setResult(null)
  }

  const handleSearch = async (event) => {
    event.preventDefault()
    if (!nickname.trim()) return

    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const request = {
        game: gameId,
        gameName: nickname.trim(),
        tagLine: game.fields.includes('tag') ? tagLine.trim() || undefined : undefined,
        region: game.fields.includes('valorant_region') ? region : 'kr',
        platform: game.fields.includes('pubg_platform') || game.fields.includes('apex_platform') ? platform : undefined,
        count: game.fields.includes('count') ? count : undefined,
      }

      const response = await searchPlayer(request)
      setResult(response)

      if (!response.success) {
        setError(response.errorMessage || '검색에 실패했습니다.')
      } else {
        setTimeout(() => {
          resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }, 120)
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || '서버 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <div className={styles.heroCopy}>
          <p className={styles.kicker}>DUO-STYLE SEARCH</p>
          <h1 className={styles.title}>게임 전적 검색을 더 빠르고 선명하게</h1>
          <p className={styles.description}>
            게임을 선택하고 닉네임을 입력하면, 매치 기록과 핵심 통계를 한 화면에서 바로 확인할 수 있습니다.
          </p>
        </div>

        <div className={styles.heroCard}>
          <div className={styles.heroMetaTop}>
            <span className={styles.heroBadge}>Live Search</span>
            <span className={styles.heroSubtle}>{game.label}</span>
          </div>
          <strong className={styles.heroHighlight}>{game.hint}</strong>
        </div>
      </section>

      <section className={styles.searchShell} style={{ '--accent': game.accent }}>
        <div className={styles.gameRail}>
          {GAMES.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => handleGameChange(item.id)}
              className={`${styles.gamePill} ${item.id === gameId ? styles.gamePillActive : ''}`}
            >
              <span className={styles.gameShort}>{item.short}</span>
              <span className={styles.gameLabel}>{item.label}</span>
            </button>
          ))}
        </div>

        <form className={styles.searchCard} onSubmit={handleSearch}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardKicker}>Search Center</p>
              <h2 className={styles.cardTitle}>{game.label}</h2>
            </div>
            <span className={styles.cardAccentChip}>{game.short}</span>
          </div>

          <div className={styles.searchGrid}>
            <label className={styles.primaryField}>
              <span>닉네임</span>
              <input
                type="text"
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                placeholder={game.placeholders.nickname}
                autoComplete="off"
                required
              />
            </label>

            {game.fields.includes('tag') && (
              <label className={styles.compactField}>
                <span>{game.tagLabel || '태그'}</span>
                <input
                  type="text"
                  value={tagLine}
                  onChange={(event) => setTagLine(event.target.value)}
                  placeholder={game.placeholders.tag || 'Tag'}
                  autoComplete="off"
                />
              </label>
            )}

            {game.fields.includes('valorant_region') && (
              <label className={styles.compactField}>
                <span>지역</span>
                <select value={region} onChange={(event) => setRegion(event.target.value)}>
                  {game.regionOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {(game.fields.includes('pubg_platform') || game.fields.includes('apex_platform')) && (
              <label className={styles.compactField}>
                <span>플랫폼</span>
                <select value={platform} onChange={(event) => setPlatform(event.target.value)}>
                  {game.platformOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {game.fields.includes('count') && (
              <label className={styles.compactField}>
                <span>매치 수</span>
                <select value={count} onChange={(event) => setCount(Number(event.target.value))}>
                  {[5, 10, 15, 20].map((option) => (
                    <option key={option} value={option}>
                      {option}게임
                    </option>
                  ))}
                </select>
              </label>
            )}
          </div>

          <div className={styles.searchFooter}>
            <p className={styles.inlineHint}>{game.hint}</p>
            <div className={styles.actions}>
              {(nickname || tagLine) && (
                <button type="button" className={styles.clearButton} onClick={clearInputs}>
                  초기화
                </button>
              )}
              <button type="submit" className={styles.searchButton} disabled={loading}>
                {loading ? '검색 중...' : '검색'}
              </button>
            </div>
          </div>
        </form>
      </section>

      {error && (
        <section className={styles.errorBox}>
          <strong>검색 실패</strong>
          {error.split('\n').map((line, index) => (
            <p key={`${line}-${index}`}>{line}</p>
          ))}
        </section>
      )}

      {result?.success && (
        <div ref={resultRef}>
          <ResultPanel result={result} accent={game.accent} title={`${game.label} Result`} />
        </div>
      )}
    </div>
  )
}
