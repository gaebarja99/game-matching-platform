import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { banReportedUser, getAdminReports, unbanReportedUser, updateAdminReportStatus } from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './AdminReportsPage.module.css'

const FILTER_OPTIONS = [
  { value: '', label: '전체 상태' },
  { value: 'PENDING', label: '접수 대기' },
  { value: 'IN_REVIEW', label: '검토 중' },
  { value: 'RESOLVED', label: '처리 완료' },
  { value: 'DISMISSED', label: '기각' },
]

const EDIT_OPTIONS = FILTER_OPTIONS.filter((option) => option.value)

function formatDate(value) {
  if (!value) return '미처리'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('ko-KR')
}

export default function AdminReportsPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [statusFilter, setStatusFilter] = useState('')
  const [reports, setReports] = useState([])
  const [drafts, setDrafts] = useState({})
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState(null)
  const [banningId, setBanningId] = useState(null)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)

  useEffect(() => {
    if (!user) {
      navigate('/login', { replace: true })
      return
    }

    if (user.role !== 'ADMIN') {
      navigate('/', { replace: true })
      return
    }

    void loadReports(statusFilter)
  }, [user, statusFilter, navigate])

  const loadReports = async (status) => {
    setLoading(true)
    setError(null)
    try {
      const data = await getAdminReports(status ? { status } : {})
      const nextReports = data || []
      setReports(nextReports)
      setDrafts(
        nextReports.reduce((acc, report) => {
          acc[report.id] = {
            status: report.status,
            adminNote: report.adminNote || '',
          }
          return acc
        }, {})
      )
    } catch (err) {
      setError(err.response?.data?.message || '신고 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleDraftChange = (reportId, field, value) => {
    setDrafts((prev) => ({
      ...prev,
      [reportId]: {
        ...prev[reportId],
        [field]: value,
      },
    }))
  }

  const handleUpdate = async (reportId) => {
    const draft = drafts[reportId]
    if (!draft?.status) return

    setSavingId(reportId)
    setError(null)
    setNotice(null)
    try {
      const updated = await updateAdminReportStatus(reportId, draft.status, draft.adminNote.trim())
      setReports((prev) => prev.map((report) => (report.id === reportId ? updated : report)))
      setDrafts((prev) => ({
        ...prev,
        [reportId]: {
          status: updated.status,
          adminNote: updated.adminNote || '',
        },
      }))
      setNotice(`신고 #${reportId} 상태를 저장했습니다.`)
    } catch (err) {
      setError(err.response?.data?.message || '신고 상태 저장에 실패했습니다.')
    } finally {
      setSavingId(null)
    }
  }

  const handleBan = async (reportId, nextAction) => {
    const draft = drafts[reportId]
    setBanningId(reportId)
    setError(null)
    setNotice(null)
    try {
      const updated = nextAction === 'unban'
        ? await unbanReportedUser(reportId, draft?.adminNote?.trim())
        : await banReportedUser(reportId, draft?.adminNote?.trim())
      setReports((prev) => prev.map((report) => (report.id === reportId ? updated : report)))
      setDrafts((prev) => ({
        ...prev,
        [reportId]: {
          status: updated.status,
          adminNote: updated.adminNote || '',
        },
      }))
      setNotice(
        nextAction === 'unban'
          ? `신고 #${reportId} 대상 사용자의 정지를 해제했습니다.`
          : `신고 #${reportId} 대상 사용자를 서버 밴 처리했습니다.`
      )
    } catch (err) {
      setError(err.response?.data?.message || '사용자 상태 변경에 실패했습니다.')
    } finally {
      setBanningId(null)
    }
  }

  if (!user) {
    return null
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <p className={styles.eyebrow}>Admin Console</p>
        <h1 className={styles.title}>신고 관리</h1>
        <p className={styles.description}>
          접수된 신고를 상태별로 확인하고, 관리자 메모와 함께 처리 결과를 저장할 수 있습니다.
        </p>
      </section>

      <section className={styles.toolbar}>
        <label className={styles.filter}>
          상태 필터
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            {FILTER_OPTIONS.map((option) => (
              <option key={option.value || 'all'} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className={styles.secondaryBtn} onClick={() => loadReports(statusFilter)}>
          새로고침
        </button>
      </section>

      {error && <div className={styles.errorBox}>{error}</div>}
      {notice && <div className={styles.noticeBox}>{notice}</div>}

      {loading ? (
        <section className={styles.emptyState}>신고 목록을 불러오는 중입니다.</section>
      ) : reports.length === 0 ? (
        <section className={styles.emptyState}>조건에 맞는 신고가 없습니다.</section>
      ) : (
        <section className={styles.list}>
          {reports.map((report) => {
            const draft = drafts[report.id] || { status: report.status, adminNote: report.adminNote || '' }
            const isSaving = savingId === report.id
            const isSuspended = report.reportedUserStatus === 'SUSPENDED'
            const isBanning = banningId === report.id

            return (
              <article key={report.id} className={styles.card}>
                <div className={styles.cardHead}>
                  <div>
                    <p className={styles.reportId}>신고 #{report.id}</p>
                    <h2 className={styles.cardTitle}>
                      {report.reportedUsername} <span>({report.reportedUserId})</span>
                    </h2>
                  </div>
                  <span className={`${styles.badge} ${styles[`status${report.status}`] || ''}`}>
                    {report.status}
                  </span>
                </div>

                <div className={styles.metaGrid}>
                  <div>
                    <span className={styles.metaLabel}>신고자</span>
                    <strong>{report.reporterUsername} ({report.reporterId})</strong>
                  </div>
                  <div>
                    <span className={styles.metaLabel}>신고 사유</span>
                    <strong>{report.reason}</strong>
                  </div>
                  <div>
                    <span className={styles.metaLabel}>접수 시각</span>
                    <strong>{formatDate(report.createdAt)}</strong>
                  </div>
                  <div>
                    <span className={styles.metaLabel}>처리 시각</span>
                    <strong>{formatDate(report.resolvedAt)}</strong>
                  </div>
                  <div>
                    <span className={styles.metaLabel}>피신고 상태</span>
                    <strong>{report.reportedUserStatus}</strong>
                  </div>
                </div>

                <div className={styles.detailBox}>
                  <span className={styles.metaLabel}>신고 내용</span>
                  <p>{report.description || '상세 내용 없음'}</p>
                </div>

                <div className={styles.formGrid}>
                  <label>
                    처리 상태
                    <select
                      value={draft.status}
                      onChange={(event) => handleDraftChange(report.id, 'status', event.target.value)}
                    >
                      {EDIT_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className={styles.noteField}>
                    관리자 메모
                    <textarea
                      rows="4"
                      maxLength="500"
                      value={draft.adminNote}
                      onChange={(event) => handleDraftChange(report.id, 'adminNote', event.target.value)}
                      placeholder="처리 내용이나 내부 메모를 남겨 주세요."
                    />
                  </label>
                </div>

                <div className={styles.cardFooter}>
                  <span className={styles.timestamps}>최근 수정: {formatDate(report.updatedAt)}</span>
                  <div className={styles.footerActions}>
                    <button
                      type="button"
                      className={styles.dangerBtn}
                      onClick={() => handleBan(report.id, isSuspended ? 'unban' : 'ban')}
                      disabled={isBanning}
                    >
                      {isBanning ? '처리 중...' : isSuspended ? '밴 해제' : '서버 밴'}
                    </button>
                    <button
                      type="button"
                      className={styles.primaryBtn}
                      onClick={() => handleUpdate(report.id)}
                      disabled={isSaving}
                    >
                      {isSaving ? '저장 중...' : '상태 저장'}
                    </button>
                  </div>
                </div>
              </article>
            )
          })}
        </section>
      )}
    </div>
  )
}
