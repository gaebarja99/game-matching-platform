import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { createPost, getPostDetail, updatePost } from '../api/index.js'
import { useAuth } from '../App.jsx'
import styles from './WritePostPage.module.css'

const CATEGORIES = [
  { id: 'FREE', label: '자유게시판' },
  { id: 'QUESTION', label: '질문게시판' },
  { id: 'LOL', label: '롤' },
  { id: 'VALORANT', label: '발로란트' },
  { id: 'PUBG', label: '배그' },
  { id: 'OVERWATCH', label: '오버워치' },
  { id: 'CS2', label: 'CS2' },
  { id: 'APEX', label: '에이펙스' },
  { id: 'BLIZZARD', label: '블리자드' },
  { id: 'STEAM', label: '스팀' },
]

export default function WritePostPage() {
  const { userId } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const editId = searchParams.get('edit')
  const isEdit = Boolean(editId)

  const [form, setForm] = useState({
    boardCategory: 'FREE',
    title: '',
    content: '',
    hashtags: '',
  })
  const [loading, setLoading] = useState(false)
  const [loadingPost, setLoadingPost] = useState(isEdit)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!isEdit) return

    getPostDetail(editId)
      .then((post) => {
        setForm({
          boardCategory: post.boardCategory,
          title: post.title,
          content: post.content,
          hashtags: post.hashtags?.join(', ') || '',
        })
      })
      .catch(console.error)
      .finally(() => setLoadingPost(false))
  }, [editId, isEdit])

  useEffect(() => {
    if (!userId) {
      navigate('/community')
    }
  }, [userId, navigate])

  const handleChange = (field) => (event) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!userId || loading) return

    setError(null)

    const hashtags = form.hashtags
      .split(',')
      .map((item) => item.trim().replace(/^#/, ''))
      .filter(Boolean)

    const payload = {
      boardCategory: form.boardCategory,
      title: form.title.trim(),
      content: form.content.trim(),
      hashtags,
      isNotice: false,
    }

    setLoading(true)
    try {
      if (isEdit) {
        await updatePost(userId, editId, payload)
        navigate(`/community/posts/${editId}`)
      } else {
        const post = await createPost(userId, payload)
        navigate(`/community/posts/${post.id}`)
      }
    } catch (err) {
      const data = err.response?.data
      if (data?.errors?.length) {
        setError(data.errors.map((item) => `${item.field}: ${item.defaultMessage}`).join(' / '))
      } else if (data?.message) {
        setError(data.message)
      } else if (typeof data === 'string') {
        setError(data)
      } else {
        setError(`오류가 발생했습니다. (${err.response?.status ?? '?'})`)
      }
    } finally {
      setLoading(false)
    }
  }

  if (loadingPost) {
    return (
      <div className={styles.loadingWrap}>
        <span className={styles.spinner} />
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.title}>{isEdit ? '게시글 수정' : '새 게시글 작성'}</h1>
        <Link to="/community" className={styles.cancelLink}>취소</Link>
      </div>

      <form className={styles.form} onSubmit={handleSubmit}>
        {error && <div className={styles.errorBox}>오류: {error}</div>}

        <div className={styles.fieldGroup}>
          <label className={styles.label}>게시판</label>
          <div className={styles.catTabs}>
            {CATEGORIES.map((category) => (
              <button
                key={category.id}
                type="button"
                className={styles.catTab + (form.boardCategory === category.id ? ` ${styles.catTabActive}` : '')}
                onClick={() => setForm((prev) => ({ ...prev, boardCategory: category.id }))}
              >
                {category.label}
              </button>
            ))}
          </div>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label} htmlFor="title">제목</label>
          <input
            id="title"
            type="text"
            className={styles.input}
            placeholder="제목을 입력해 주세요"
            value={form.title}
            onChange={handleChange('title')}
            required
            maxLength={200}
          />
          <span className={styles.charCount}>{form.title.length}/200</span>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label} htmlFor="content">내용</label>
          <textarea
            id="content"
            className={styles.textarea}
            placeholder="내용을 입력해 주세요."
            value={form.content}
            onChange={handleChange('content')}
            required
            rows={16}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label} htmlFor="hashtags">
            해시태그 <span className={styles.optional}>(선택, 쉼표로 구분)</span>
          </label>
          <input
            id="hashtags"
            type="text"
            className={styles.input}
            placeholder="예: LoL, 듀오, 티어"
            value={form.hashtags}
            onChange={handleChange('hashtags')}
          />
        </div>

        {form.hashtags && (
          <div className={styles.hashtagPreview}>
            {form.hashtags
              .split(',')
              .map((item) => item.trim())
              .filter(Boolean)
              .map((item, index) => (
                <span key={index} className={styles.hashtagChip}>#{item.replace(/^#/, '')}</span>
              ))}
          </div>
        )}

        <div className={styles.formActions}>
          <Link to="/community" className={styles.cancelBtn}>취소</Link>
          <button type="submit" className={styles.submitBtn} disabled={loading}>
            {loading ? <><span className={styles.spinnerSm} /> 처리 중...</> : isEdit ? '수정 완료' : '게시글 등록'}
          </button>
        </div>
      </form>
    </div>
  )
}
