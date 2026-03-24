import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyProfile } from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './MyProfilePage.module.css'

const PROVIDER_LABELS = {
  discord: 'Discord',
  steam: 'Steam',
  blizzard: 'Blizzard',
  riot: 'Riot',
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('ko-KR')
}

export default function MyProfilePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!user) {
      navigate('/login', { replace: true })
      return
    }

    void loadProfile()
  }, [user, navigate])

  const loadProfile = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getMyProfile()
      setProfile(data)
    } catch (err) {
      setError(err.response?.data?.message || '내 정보를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  if (!user) {
    return null
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <p className={styles.eyebrow}>My Account</p>
        <h1 className={styles.title}>내 정보</h1>
        <p className={styles.description}>
          계정 정보와 현재 연동 상태를 한 번에 확인할 수 있습니다.
        </p>
      </section>

      {error && <div className={styles.errorBox}>{error}</div>}

      {loading ? (
        <div className={styles.loading}>내 정보를 불러오는 중입니다...</div>
      ) : profile && (
        <>
          <section className={styles.summaryGrid}>
            <article className={styles.card}>
              <span className={styles.cardLabel}>Nickname</span>
              <strong className={styles.cardValue}>{profile.username}</strong>
            </article>
            <article className={styles.card}>
              <span className={styles.cardLabel}>Email</span>
              <strong className={styles.cardValue}>{profile.email}</strong>
            </article>
            <article className={styles.card}>
              <span className={styles.cardLabel}>Status</span>
              <strong className={styles.cardValue}>{profile.status}</strong>
            </article>
            <article className={styles.card}>
              <span className={styles.cardLabel}>Created</span>
              <strong className={styles.cardValue}>{formatDate(profile.createdAt)}</strong>
            </article>
          </section>

          <section className={styles.detailCard}>
            <h2>기본 정보</h2>
            <dl className={styles.infoList}>
              <div>
                <dt>아이디</dt>
                <dd>{profile.loginId}</dd>
              </div>
              <div>
                <dt>닉네임</dt>
                <dd>{profile.username}</dd>
              </div>
              <div>
                <dt>이메일</dt>
                <dd>{profile.email}</dd>
              </div>
              <div>
                <dt>권한</dt>
                <dd>{profile.role}</dd>
              </div>
            </dl>
          </section>

          <section className={styles.detailCard}>
            <div className={styles.sectionHead}>
              <h2>연동된 계정</h2>
              <button type="button" className={styles.refreshBtn} onClick={loadProfile}>
                새로고침
              </button>
            </div>
            <div className={styles.connectionList}>
              {profile.connections?.map((connection) => (
                <div key={connection.provider} className={styles.connectionItem}>
                  <div>
                    <strong>{PROVIDER_LABELS[connection.provider] || connection.provider}</strong>
                    <p>{connection.connected ? (connection.displayName || '연동 완료') : '연동 안 됨'}</p>
                  </div>
                  {connection.connected ? (
                    <span className={`${styles.statusBadge} ${styles.connected}`}>연동 완료</span>
                  ) : (
                    <button
                      type="button"
                      className={styles.linkBtn}
                      onClick={() => navigate('/connections')}
                    >
                      연결하기
                    </button>
                  )}
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}
