import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  blockUser,
  cancelUserReport,
  checkBlockedUser,
  createUserReport,
  getBlockedUsers,
  getMyReports,
  getReportDetail,
  unblockUser,
} from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './ReportBlockTestPage.module.css'

const REPORT_REASONS = [
  { value: 'SPAM', label: '스팸' },
  { value: 'HARASSMENT', label: '괴롭힘' },
  { value: 'INAPPROPRIATE_CONTENT', label: '부적절한 콘텐츠' },
  { value: 'CHEATING', label: '부정행위' },
  { value: 'IMPERSONATION', label: '사칭' },
  { value: 'HATE_SPEECH', label: '혐오 발언' },
  { value: 'OTHER', label: '기타' },
]

export default function ReportBlockTestPage() {
  const { user, userId } = useAuth()
  const navigate = useNavigate()
  const [reports, setReports] = useState([])
  const [blockedUsers, setBlockedUsers] = useState([])
  const [reportDetail, setReportDetail] = useState(null)
  const [checkResult, setCheckResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [reportForm, setReportForm] = useState({
    reportedUserId: '',
    reason: 'SPAM',
    description: '',
  })
  const [blockTargetId, setBlockTargetId] = useState('')
  const [checkTargetId, setCheckTargetId] = useState('')

  useEffect(() => {
    if (!user) {
      navigate('/login', { replace: true })
      return
    }
    refreshAll()
  }, [user])

  const refreshAll = async () => {
    setLoading(true)
    setError(null)
    try {
      const [reportData, blockedData] = await Promise.all([
        getMyReports(userId),
        getBlockedUsers(userId),
      ])
      setReports(reportData || [])
      setBlockedUsers(blockedData || [])
    } catch (err) {
      setError(err.response?.data?.message || '신고/차단 데이터를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleCreateReport = async (e) => {
    e.preventDefault()
    setError(null)
    setNotice(null)
    try {
      await createUserReport(userId, {
        reportedUserId: Number(reportForm.reportedUserId),
        reason: reportForm.reason,
        description: reportForm.description || undefined,
      })
      setReportForm({ reportedUserId: '', reason: 'SPAM', description: '' })
      setNotice('신고가 등록되었습니다.')
      await refreshAll()
    } catch (err) {
      setError(err.response?.data?.message || '신고 등록에 실패했습니다.')
    }
  }

  const handleCancelReport = async (reportId) => {
    setError(null)
    setNotice(null)
    try {
      await cancelUserReport(userId, reportId)
      setNotice('신고가 취소되었습니다.')
      if (reportDetail?.id === reportId) {
        setReportDetail(null)
      }
      await refreshAll()
    } catch (err) {
      setError(err.response?.data?.message || '신고 취소에 실패했습니다.')
    }
  }

  const handleLoadReportDetail = async (reportId) => {
    setError(null)
    try {
      const detail = await getReportDetail(userId, reportId)
      setReportDetail(detail)
    } catch (err) {
      setError(err.response?.data?.message || '신고 상세 조회에 실패했습니다.')
    }
  }

  const handleBlockUser = async (e) => {
    e.preventDefault()
    setError(null)
    setNotice(null)
    try {
      await blockUser(userId, { blockedUserId: Number(blockTargetId) })
      setBlockTargetId('')
      setNotice('사용자를 차단했습니다.')
      await refreshAll()
    } catch (err) {
      setError(err.response?.data?.message || '차단에 실패했습니다.')
    }
  }

  const handleUnblockUser = async (blockedUserId) => {
    setError(null)
    setNotice(null)
    try {
      await unblockUser(userId, blockedUserId)
      setNotice('차단을 해제했습니다.')
      await refreshAll()
    } catch (err) {
      setError(err.response?.data?.message || '차단 해제에 실패했습니다.')
    }
  }

  const handleCheckBlock = async (e) => {
    e.preventDefault()
    setError(null)
    setCheckResult(null)
    try {
      const result = await checkBlockedUser(userId, Number(checkTargetId))
      setCheckResult(Boolean(result))
    } catch (err) {
      setError(err.response?.data?.message || '차단 여부 확인에 실패했습니다.')
    }
  }

  if (!user) {
    return null
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>Safety Tools</p>
          <h1 className={styles.title}>신고·차단 테스트</h1>
          <p className={styles.description}>
            현재 로그인한 회원 ID <strong>{userId}</strong> 기준으로 신고 등록, 신고 취소, 차단 등록,
            차단 해제, 차단 여부 확인 API를 직접 검증할 수 있는 테스트 화면입니다.
          </p>
        </div>
      </section>

      {error && <div className={styles.errorBox}>{error}</div>}
      {notice && <div className={styles.noticeBox}>{notice}</div>}

      <section className={styles.grid}>
        <article className={styles.card}>
          <h2>신고 등록</h2>
          <form className={styles.form} onSubmit={handleCreateReport}>
            <label>
              신고 대상 사용자 ID
              <input
                type="number"
                min="1"
                value={reportForm.reportedUserId}
                onChange={(e) => setReportForm((prev) => ({ ...prev, reportedUserId: e.target.value }))}
                required
              />
            </label>
            <label>
              신고 사유
              <select
                value={reportForm.reason}
                onChange={(e) => setReportForm((prev) => ({ ...prev, reason: e.target.value }))}
              >
                {REPORT_REASONS.map((reason) => (
                  <option key={reason.value} value={reason.value}>{reason.label}</option>
                ))}
              </select>
            </label>
            <label>
              상세 설명
              <textarea
                rows="4"
                maxLength="500"
                value={reportForm.description}
                onChange={(e) => setReportForm((prev) => ({ ...prev, description: e.target.value }))}
                placeholder="필수는 아니지만 검증할 때 같이 남기면 좋습니다."
              />
            </label>
            <button type="submit" className={styles.primaryBtn}>신고 등록</button>
          </form>
        </article>

        <article className={styles.card}>
          <h2>차단 등록</h2>
          <form className={styles.form} onSubmit={handleBlockUser}>
            <label>
              차단할 사용자 ID
              <input
                type="number"
                min="1"
                value={blockTargetId}
                onChange={(e) => setBlockTargetId(e.target.value)}
                required
              />
            </label>
            <button type="submit" className={styles.primaryBtn}>차단하기</button>
          </form>

          <hr className={styles.divider} />

          <h3>차단 여부 확인</h3>
          <form className={styles.inlineForm} onSubmit={handleCheckBlock}>
            <input
              type="number"
              min="1"
              value={checkTargetId}
              onChange={(e) => setCheckTargetId(e.target.value)}
              placeholder="확인할 사용자 ID"
              required
            />
            <button type="submit" className={styles.secondaryBtn}>확인</button>
          </form>
          {checkResult != null && (
            <p className={styles.checkResult}>
              결과: <strong>{checkResult ? '차단됨' : '차단 안 됨'}</strong>
            </p>
          )}
        </article>
      </section>

      <section className={styles.grid}>
        <article className={styles.cardWide}>
          <div className={styles.sectionHead}>
            <h2>내 신고 목록</h2>
            <button type="button" className={styles.secondaryBtn} onClick={refreshAll}>
              새로고침
            </button>
          </div>
          {loading ? (
            <p className={styles.empty}>불러오는 중...</p>
          ) : reports.length === 0 ? (
            <p className={styles.empty}>등록된 신고가 없습니다.</p>
          ) : (
            <div className={styles.list}>
              {reports.map((report) => (
                <div key={report.id} className={styles.listItem}>
                  <div>
                    <strong>#{report.id} · 대상 {report.reportedUsername} ({report.reportedUserId})</strong>
                    <p>사유: {report.reason} · 상태: {report.status}</p>
                  </div>
                  <div className={styles.actions}>
                    <button type="button" className={styles.secondaryBtn} onClick={() => handleLoadReportDetail(report.id)}>
                      상세
                    </button>
                    {report.status === 'PENDING' && (
                      <button type="button" className={styles.dangerBtn} onClick={() => handleCancelReport(report.id)}>
                        취소
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
          {reportDetail && (
            <div className={styles.detailBox}>
              <h3>신고 상세 #{reportDetail.id}</h3>
              <p>대상: {reportDetail.reportedUsername} ({reportDetail.reportedUserId})</p>
              <p>사유: {reportDetail.reason}</p>
              <p>설명: {reportDetail.description || '없음'}</p>
              <p>상태: {reportDetail.status}</p>
            </div>
          )}
        </article>

        <article className={styles.cardWide}>
          <div className={styles.sectionHead}>
            <h2>차단 목록</h2>
            <button type="button" className={styles.secondaryBtn} onClick={refreshAll}>
              새로고침
            </button>
          </div>
          {loading ? (
            <p className={styles.empty}>불러오는 중...</p>
          ) : blockedUsers.length === 0 ? (
            <p className={styles.empty}>차단한 사용자가 없습니다.</p>
          ) : (
            <div className={styles.list}>
              {blockedUsers.map((blocked) => (
                <div key={blocked.id} className={styles.listItem}>
                  <div>
                    <strong>{blocked.blockedUsername}</strong>
                    <p>사용자 ID: {blocked.blockedUserId}</p>
                  </div>
                  <button
                    type="button"
                    className={styles.dangerBtn}
                    onClick={() => handleUnblockUser(blocked.blockedUserId)}
                  >
                    차단 해제
                  </button>
                </div>
              ))}
            </div>
          )}
        </article>
      </section>
    </div>
  )
}
