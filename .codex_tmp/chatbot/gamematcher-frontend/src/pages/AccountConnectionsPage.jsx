import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  getAccountConnections,
  linkRiotAccountManually,
  startOAuthConnection,
  unlinkAccountConnection,
} from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './AccountConnectionsPage.module.css'

const PROVIDER_META = {
  discord: { label: 'Discord', tone: '#5865F2', icon: 'DS' },
  steam: { label: 'Steam', tone: '#1b2838', icon: 'ST' },
  blizzard: { label: 'Blizzard', tone: '#00A4E4', icon: 'BZ' },
  riot: { label: 'Riot', tone: '#D13639', icon: 'RT' },
}

export default function AccountConnectionsPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [loading, setLoading] = useState(true)
  const [busyProvider, setBusyProvider] = useState(null)
  const [connections, setConnections] = useState([])
  const [error, setError] = useState(null)
  const [riotForm, setRiotForm] = useState({ gameName: '', tagLine: '' })
  const popupRef = useRef(null)

  const flashMessage = useMemo(() => {
    const provider = searchParams.get('provider')
    const result = searchParams.get('result')
    const message = searchParams.get('message')
    return provider && result && message ? { provider, result, message } : null
  }, [searchParams])

  useEffect(() => {
    if (!user) {
      navigate('/login', { replace: true })
      return
    }
    void loadConnections()
  }, [user, navigate])

  useEffect(() => {
    const handleMessage = async (event) => {
      const data = event.data
      if (!data || data.type !== 'ACCOUNT_LINK_RESULT') {
        return
      }

      const next = new URLSearchParams(searchParams)
      next.set('provider', data.provider)
      next.set('result', data.result)
      next.set('message', data.message)
      setSearchParams(next, { replace: true })
      setBusyProvider(null)
      await loadConnections()
    }

    window.addEventListener('message', handleMessage)
    return () => window.removeEventListener('message', handleMessage)
  }, [searchParams, setSearchParams])

  const loadConnections = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getAccountConnections()
      setConnections(data.connections || [])
    } catch (err) {
      setError(err.response?.data?.message || '연동 상태를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleOAuthConnect = async (provider) => {
    setBusyProvider(provider)
    setError(null)
    try {
      const data = await startOAuthConnection(provider)
      const popup = window.open(
        data.authorizationUrl,
        `account-link-${provider}`,
        'width=560,height=760,menubar=no,toolbar=no,status=no'
      )

      if (!popup) {
        setError('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해 주세요.')
        setBusyProvider(null)
        return
      }

      popupRef.current = popup

      const checkClosed = window.setInterval(() => {
        if (!popup || popup.closed) {
          window.clearInterval(checkClosed)
          popupRef.current = null
          setBusyProvider((current) => (current === provider ? null : current))
          void loadConnections()
        }
      }, 500)
    } catch (err) {
      setError(err.response?.data?.message || `${PROVIDER_META[provider].label} 연동을 시작하지 못했습니다.`)
      setBusyProvider(null)
    }
  }

  const handleDisconnect = async (provider) => {
    setBusyProvider(provider)
    setError(null)
    try {
      await unlinkAccountConnection(provider)
      await loadConnections()
    } catch (err) {
      setError(err.response?.data?.message || `${PROVIDER_META[provider].label} 연동을 해제하지 못했습니다.`)
    } finally {
      setBusyProvider(null)
    }
  }

  const handleRiotLink = async (event) => {
    event.preventDefault()
    setBusyProvider('riot')
    setError(null)
    try {
      await linkRiotAccountManually(riotForm)
      setRiotForm({ gameName: '', tagLine: '' })
      await loadConnections()
    } catch (err) {
      setError(err.response?.data?.message || 'Riot 계정 연동에 실패했습니다.')
    } finally {
      setBusyProvider(null)
    }
  }

  const dismissFlash = () => {
    const next = new URLSearchParams(searchParams)
    next.delete('provider')
    next.delete('result')
    next.delete('message')
    setSearchParams(next, { replace: true })
    void loadConnections()
  }

  if (!user) {
    return null
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>External Connections</p>
          <h1 className={styles.title}>게임 계정 연동</h1>
          <p className={styles.description}>
            GameMatcher 회원 계정에 Discord, Steam, Blizzard, Riot 계정을 연결해 두세요.
            외부 계정 로그인용이 아니라, 본인 인증과 기능 확장을 위한 연동입니다.
          </p>
        </div>
        <div className={styles.heroBadge}>
          <span>{user.username}</span>
          <strong>{connections.filter((item) => item.connected).length} / {connections.length || 4}</strong>
          <small>연동 완료</small>
        </div>
      </section>

      {flashMessage && (
        <div className={`${styles.flash} ${flashMessage.result === 'success' ? styles.flashSuccess : styles.flashError}`}>
          <div>
            <strong>{PROVIDER_META[flashMessage.provider]?.label || flashMessage.provider}</strong>
            <p>{flashMessage.message}</p>
          </div>
          <button type="button" onClick={dismissFlash}>닫기</button>
        </div>
      )}

      {error && <div className={styles.errorBox}>{error}</div>}

      <section className={styles.grid}>
        {loading ? (
          <div className={styles.loading}>연동 상태를 불러오는 중입니다...</div>
        ) : (
          connections.map((connection) => {
            const meta = PROVIDER_META[connection.provider] || { label: connection.provider, tone: '#666', icon: 'NA' }
            const isBusy = busyProvider === connection.provider

            return (
              <article key={connection.provider} className={styles.card} style={{ '--tone': meta.tone }}>
                <div className={styles.cardTop}>
                  <div className={styles.iconBubble}>{meta.icon}</div>
                  <div>
                    <h2>{meta.label}</h2>
                    <p className={styles.note}>{connection.note}</p>
                  </div>
                  <span className={`${styles.status} ${connection.connected ? styles.statusOn : styles.statusOff}`}>
                    {connection.connected ? '연동됨' : '미연동'}
                  </span>
                </div>

                <div className={styles.cardBody}>
                  {connection.connected ? (
                    <>
                      <div className={styles.identityRow}>
                        <div>
                          <strong>{connection.displayName || meta.label}</strong>
                          {connection.secondaryValue && <p>{connection.secondaryValue}</p>}
                        </div>
                        <div className={styles.verifyBadge}>
                          {connection.ownershipVerified ? '본인 인증 완료' : '조회 기반 연동'}
                        </div>
                      </div>
                      <button
                        type="button"
                        className={styles.secondaryBtn}
                        disabled={isBusy}
                        onClick={() => handleDisconnect(connection.provider)}
                      >
                        {isBusy ? '처리 중...' : '연동 해제'}
                      </button>
                    </>
                  ) : connection.provider === 'riot' ? (
                    <form className={styles.riotForm} onSubmit={handleRiotLink}>
                      <input
                        value={riotForm.gameName}
                        onChange={(event) => setRiotForm((prev) => ({ ...prev, gameName: event.target.value }))}
                        placeholder="Game Name"
                        required
                      />
                      <input
                        value={riotForm.tagLine}
                        onChange={(event) => setRiotForm((prev) => ({ ...prev, tagLine: event.target.value }))}
                        placeholder="Tag Line"
                        required
                      />
                      <button type="submit" className={styles.primaryBtn} disabled={isBusy}>
                        {isBusy ? '연동 중...' : 'Riot 조회 후 연동'}
                      </button>
                    </form>
                  ) : (
                    <button
                      type="button"
                      className={styles.primaryBtn}
                      disabled={isBusy}
                      onClick={() => handleOAuthConnect(connection.provider)}
                    >
                      {isBusy ? '연동 창 여는 중...' : `${meta.label} 연동하기`}
                    </button>
                  )}
                </div>
              </article>
            )
          })
        )}
      </section>
    </div>
  )
}
