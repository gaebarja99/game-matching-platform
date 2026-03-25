import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  try {
    const raw = localStorage.getItem('gm_user')
    if (raw) {
      const user = JSON.parse(raw)
      if (user?.authToken) {
        config.headers['X-Auth-Token'] = user.authToken
      }
    }
  } catch {
    // ignore localStorage parsing errors
  }
  return config
})

// ─────────────────────────────────────────────
// Player Search
// ─────────────────────────────────────────────
export const searchPlayer = (request) =>
  api.post('/search/player', request).then(r => r.data)

export const batchSearchJson = (requests) =>
  api.post('/search/batch/json', requests).then(r => r.data)

// ─────────────────────────────────────────────
// Community Public (no auth)
// ─────────────────────────────────────────────
export const getCommunityPosts = (category, params = {}) => {
  const url = category
    ? `/community/boards/${category}/posts`
    : `/community/posts`
  return api.get(url, { params }).then(r => r.data)
}

export const getPopularPosts = (category, limit = 5) => {
  const url = category
    ? `/community/boards/${category}/posts/popular`
    : `/community/posts/popular`
  return api.get(url, { params: { limit } }).then(r => r.data)
}

export const getPostDetail = (postId) =>
  api.get(`/community/posts/${postId}`).then(r => r.data)

export const getPostComments = (postId) =>
  api.get(`/community/posts/${postId}/comments`).then(r => r.data)

// ─────────────────────────────────────────────
// Community (auth required — userId in path)
// ─────────────────────────────────────────────
export const createPost = (userId, post) =>
  api.post(`/users/${userId}/community/posts/json`, post).then(r => r.data)

export const updatePost = (userId, postId, post) =>
  api.put(`/users/${userId}/community/posts/${postId}`, post).then(r => r.data)

export const deletePost = (userId, postId) =>
  api.delete(`/users/${userId}/community/posts/${postId}`)

export const createComment = (userId, postId, body) =>
  api.post(`/users/${userId}/community/posts/${postId}/comments`, body).then(r => r.data)

export const deleteComment = (userId, commentId) =>
  api.delete(`/users/${userId}/community/comments/${commentId}`)

export const toggleLike = (userId, postId) =>
  api.post(`/users/${userId}/community/posts/${postId}/like`)

export const toggleBookmark = (userId, postId) =>
  api.post(`/users/${userId}/community/posts/${postId}/bookmark`)

export const recommendPost = (userId, postId, type) =>
  api.post(`/users/${userId}/community/posts/${postId}/recommend`, null, {
    params: { type }
  })

export const getNotifications = (userId, params = {}) =>
  api.get(`/users/${userId}/community/notifications`, { params }).then(r => r.data)

export const getUnreadCount = (userId) =>
  api.get(`/users/${userId}/community/notifications/unread-count`).then(r => r.data)

export const markNotificationRead = (userId, notificationId) =>
  api.patch(`/users/${userId}/community/notifications/${notificationId}/read`)

export const getBookmarks = (userId, params = {}) =>
  api.get(`/users/${userId}/community/bookmarks`, { params }).then(r => r.data)

// ─────────────────────────────────────────────
// Auth
// ─────────────────────────────────────────────
export const login = (body) =>
  api.post('/auth/login', body).then(r => r.data)

export const signup = (body) =>
  api.post('/auth/signup', body).then(r => r.data)

export const getAccountConnections = () =>
  api.get('/account-links').then(r => r.data)

export const unlinkAccountConnection = (provider) =>
  api.delete(`/account-links/${provider}`)

export const startOAuthConnection = (provider) =>
  api.get(`/account-links/oauth/${provider}/start`).then(r => r.data)

export const linkRiotAccountManually = (body) =>
  api.post('/account-links/riot/manual', body).then(r => r.data)

export const createUserReport = (userId, body) =>
  api.post(`/users/${userId}/reports`, body).then(r => r.data)

export const getMyReports = (userId, params = {}) =>
  api.get(`/users/${userId}/reports`, { params }).then(r => r.data)

export const getReportDetail = (userId, reportId) =>
  api.get(`/users/${userId}/reports/${reportId}`).then(r => r.data)

export const cancelUserReport = (userId, reportId) =>
  api.delete(`/users/${userId}/reports/${reportId}`)

export const blockUser = (userId, body) =>
  api.post(`/users/${userId}/blocks`, body).then(r => r.data)

export const unblockUser = (userId, blockedUserId) =>
  api.delete(`/users/${userId}/blocks/${blockedUserId}`)

export const getBlockedUsers = (userId) =>
  api.get(`/users/${userId}/blocks`).then(r => r.data)

export const checkBlockedUser = (userId, targetUserId) =>
  api.get(`/users/${userId}/blocks/check/${targetUserId}`).then(r => r.data)

export const getAdminReports = (params = {}) =>
  api.get('/admin/reports', { params }).then(r => r.data)

export const updateAdminReportStatus = (reportId, status, adminNote) =>
  api.patch(`/admin/reports/${reportId}`, null, {
    params: {
      status,
      ...(adminNote ? { adminNote } : {}),
    },
  }).then(r => r.data)

export const banReportedUser = (reportId, adminNote) =>
  api.post(`/admin/reports/${reportId}/ban`, null, {
    params: {
      ...(adminNote ? { adminNote } : {}),
    },
  }).then(r => r.data)

export const unbanReportedUser = (reportId, adminNote) =>
  api.post(`/admin/reports/${reportId}/unban`, null, {
    params: {
      ...(adminNote ? { adminNote } : {}),
    },
  }).then(r => r.data)

export const getMyProfile = () =>
  api.get('/me').then(r => r.data)

export const sendChatMessage = (message) =>
  api.post('/chat', { message }, { timeout: 60000 }).then(r => r.data)

export default api
