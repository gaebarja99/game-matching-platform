import React, { createContext, useContext, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import HomePage from './pages/HomePage.jsx'
import SearchPage from './pages/SearchPage.jsx'
import CommunityPage from './pages/CommunityPage.jsx'
import PostDetailPage from './pages/PostDetailPage.jsx'
import WritePostPage from './pages/WritePostPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import AccountConnectionsPage from './pages/AccountConnectionsPage.jsx'
import ReportBlockTestPage from './pages/ReportBlockTestPage.jsx'
import AdminReportsPage from './pages/AdminReportsPage.jsx'
import MyProfilePage from './pages/MyProfilePage.jsx'
import ChatbotPage from './pages/ChatbotPage.jsx'

export const AuthContext = createContext(null)

export function useAuth() {
  return useContext(AuthContext)
}

const STORAGE_KEY = 'gm_user'

function loadUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export default function App() {
  const [user, setUser] = useState(loadUser)
  const userId = user?.userId ?? null

  const loginUser = (userData) => {
    setUser(userData)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(userData))
  }

  const logout = () => {
    setUser(null)
    localStorage.removeItem(STORAGE_KEY)
  }

  return (
    <AuthContext.Provider value={{ user, userId, loginUser, logout }}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="search" element={<SearchPage />} />
            <Route path="community" element={<CommunityPage />} />
            <Route path="community/write" element={<WritePostPage />} />
            <Route path="community/posts/:postId" element={<PostDetailPage />} />
            <Route path="chatbot" element={<ChatbotPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="me" element={<MyProfilePage />} />
            <Route path="connections" element={<AccountConnectionsPage />} />
            <Route path="safety" element={<ReportBlockTestPage />} />
            <Route path="admin/reports" element={<AdminReportsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthContext.Provider>
  )
}
