import React from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../App.jsx'
import styles from './Layout.module.css'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <NavLink to="/" className={styles.logo}>
            <span className={styles.logoIcon}>GM</span>
            <span className={styles.logoText}>GAME<span>MATCHER</span></span>
          </NavLink>

          <nav className={styles.nav}>
            <NavLink to="/" end className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
              홈
            </NavLink>
            <NavLink to="/search" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
              전적 검색
            </NavLink>
            <NavLink to="/community" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
              커뮤니티
            </NavLink>
            <NavLink to="/chatbot" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
              챗봇
            </NavLink>
            {user && (
              <NavLink to="/me" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
                내 정보
              </NavLink>
            )}
            {user && (
              <NavLink to="/connections" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
                계정 연동
              </NavLink>
            )}
            {user && (
              <NavLink to="/safety" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
                신고·차단
              </NavLink>
            )}
            {user?.role === 'ADMIN' && (
              <NavLink to="/admin/reports" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}>
                신고 관리
              </NavLink>
            )}
          </nav>

          <div className={styles.authArea}>
            {user ? (
              <div className={styles.userInfo}>
                <span className={styles.userBadge}>
                  <span className={styles.dot} />
                  <span className={styles.userName}>{user.username}</span>
                  {user.role === 'ADMIN' && <span className={styles.adminTag}>관리자</span>}
                </span>
                <button
                  className={styles.logoutBtn}
                  onClick={() => {
                    logout()
                    navigate('/')
                  }}
                >
                  로그아웃
                </button>
              </div>
            ) : (
              <Link to="/login" className={styles.loginBtn}>
                로그인
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <Outlet />
      </main>

      <footer className={styles.footer}>
        <p>© 2026 GameMatcher · Powered by Spring Boot + React</p>
      </footer>
    </div>
  )
}
