import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login, signup } from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './LoginPage.module.css'

export default function LoginPage() {
  const { loginUser } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ loginId: '', password: '', username: '', email: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleChange = (event) => {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }))
    setError(null)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError(null)

    try {
      const response = mode === 'login'
        ? await login({ loginId: form.loginId, password: form.password })
        : await signup({
            loginId: form.loginId,
            password: form.password,
            username: form.username,
            email: form.email,
          })

      loginUser(response)
      navigate('/', { replace: true })
    } catch (err) {
      const data = err.response?.data
      if (data?.errors?.length) {
        setError(data.errors.map((item) => item.defaultMessage).join(', '))
      } else if (data?.details) {
        setError(data.details)
      } else if (data?.message) {
        setError(data.message)
      } else if (data?.error) {
        setError(data.error)
      } else {
        setError('오류가 발생했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <Link to="/" className={styles.logo}>
          <span>GM</span> GAMEMATCHER
        </Link>

        <div className={styles.tabs}>
          <button
            className={`${styles.tab} ${mode === 'login' ? styles.tabActive : ''}`}
            onClick={() => { setMode('login'); setError(null) }}
          >
            로그인
          </button>
          <button
            className={`${styles.tab} ${mode === 'signup' ? styles.tabActive : ''}`}
            onClick={() => { setMode('signup'); setError(null) }}
          >
            회원가입
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          {error && <div className={styles.error}>오류: {error}</div>}

          <div className={styles.field}>
            <label>아이디</label>
            <input
              name="loginId"
              type="text"
              placeholder="아이디 입력"
              value={form.loginId}
              onChange={handleChange}
              required
              autoFocus
            />
          </div>

          <div className={styles.field}>
            <label>비밀번호</label>
            <input
              name="password"
              type="password"
              placeholder="비밀번호 입력"
              value={form.password}
              onChange={handleChange}
              required
            />
          </div>

          {mode === 'signup' && (
            <>
              <div className={styles.field}>
                <label>닉네임</label>
                <input
                  name="username"
                  type="text"
                  placeholder="닉네임 입력 (2~20자)"
                  value={form.username}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className={styles.field}>
                <label>이메일</label>
                <input
                  name="email"
                  type="email"
                  placeholder="이메일 입력"
                  value={form.email}
                  onChange={handleChange}
                  required
                />
              </div>
            </>
          )}

          <button type="submit" className={styles.submitBtn} disabled={loading}>
            {loading ? <span className={styles.spinner} /> : mode === 'login' ? '로그인' : '회원가입'}
          </button>
        </form>

        <p className={styles.switchHint}>
          {mode === 'login'
            ? <>계정이 없으신가요? <button className={styles.switchBtn} onClick={() => setMode('signup')}>회원가입</button></>
            : <>이미 계정이 있으신가요? <button className={styles.switchBtn} onClick={() => setMode('login')}>로그인</button></>
          }
        </p>
      </div>
    </div>
  )
}
