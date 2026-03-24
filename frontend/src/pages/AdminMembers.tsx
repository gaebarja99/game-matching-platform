import { useEffect, useMemo, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';
import { fetchAdminMemberHistory, fetchAdminMembers, giftAdminPang, updateAdminMember } from '../api/admin';
import type { AdminAuditLogRow, AdminMemberRow } from '../api/admin';

const suspensionOptions = [
  { label: '1일 정지', days: 1 },
  { label: '7일 정지', days: 7 },
  { label: '30일 정지', days: 30 },
  { label: '60일 정지', days: 60 },
  { label: '90일 정지', days: 90 },
  { label: '180일 정지', days: 180 },
  { label: '1년 정지', days: 365 },
  { label: '영구 정지', days: -1 },
];

function formatDate(value?: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function shortenLoginId(loginId: string) {
  if (!loginId) return '-';
  const socialPrefixes = ['google_', 'naver_', 'kakao_'];
  const matchedPrefix = socialPrefixes.find((prefix) => loginId.startsWith(prefix));
  if (!matchedPrefix) return loginId;
  const rest = loginId.slice(matchedPrefix.length);
  if (rest.length <= 5) return loginId;
  return `${matchedPrefix}${rest.slice(0, 5)}...`;
}

function memberStatusMeta(status: AdminMemberRow['status']) {
  switch (status) {
    case 'ACTIVE':
      return { label: '활성', tone: 'tone-success' };
    case 'INACTIVE':
      return { label: '비활성', tone: 'tone-muted' };
    case 'SUSPENDED':
      return { label: '정지', tone: 'tone-danger' };
    case 'DELETED':
      return { label: '삭제', tone: 'tone-neutral' };
    default:
      return { label: status, tone: 'tone-neutral' };
  }
}

function roleMeta(role: AdminMemberRow['role']) {
  switch (role) {
    case 'ADMIN':
      return { label: '관리자', tone: 'tone-accent' };
    case 'USER':
    default:
      return { label: '일반 회원', tone: 'tone-neutral' };
  }
}

function compareText(a?: string | null, b?: string | null) {
  return (a ?? '').localeCompare(b ?? '', 'ko');
}

export default function AdminMembers() {
  const { user, loading: authLoading } = useAuth();

  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortKey, setSortKey] = useState('created-desc');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<{ content: AdminMemberRow[]; totalPages: number; first: boolean; last: boolean } | null>(null);
  const [loading, setLoading] = useState(true);
  const [submittingId, setSubmittingId] = useState<number | null>(null);
  const [selectedMember, setSelectedMember] = useState<AdminMemberRow | null>(null);
  const [history, setHistory] = useState<AdminAuditLogRow[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [suspendTarget, setSuspendTarget] = useState<AdminMemberRow | null>(null);
  const [suspensionDays, setSuspensionDays] = useState('7');
  const [suspensionReason, setSuspensionReason] = useState('');
  const [nicknameDraft, setNicknameDraft] = useState('');
  const [giftLoginId, setGiftLoginId] = useState('');
  const [giftAmount, setGiftAmount] = useState('');
  const [giftMessage, setGiftMessage] = useState('');
  const [modalGiftAmount, setModalGiftAmount] = useState('');
  const [modalGiftMessage, setModalGiftMessage] = useState('');

  const load = async () => {
    setLoading(true);
    const response = await fetchAdminMembers({ query, role: roleFilter, status: statusFilter, page, size: 12 });
    setResult(response.ok && response.data ? response.data : { content: [], totalPages: 0, first: true, last: true });
    setLoading(false);
  };

  const loadHistory = async (memberId: number) => {
    setHistoryLoading(true);
    const response = await fetchAdminMemberHistory(memberId);
    setHistory(response.ok && response.data ? response.data : []);
    setHistoryLoading(false);
  };

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    void load();
  }, [page, query, roleFilter, statusFilter, user]);

  useEffect(() => {
    if (!selectedMember) {
      setHistory([]);
      setNicknameDraft('');
      setModalGiftAmount('');
      setModalGiftMessage('');
      return;
    }
    setNicknameDraft(selectedMember.nickname || selectedMember.username);
    void loadHistory(selectedMember.id);
  }, [selectedMember]);

  const rows = result?.content ?? [];

  const visibleRows = useMemo(() => {
    const nextRows = [...rows];
    nextRows.sort((a, b) => {
      const createdA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const createdB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      const lastLoginA = a.lastLoginAt ? new Date(a.lastLoginAt).getTime() : 0;
      const lastLoginB = b.lastLoginAt ? new Date(b.lastLoginAt).getTime() : 0;
      const pangA = Number(a.pangBalance ?? 0);
      const pangB = Number(b.pangBalance ?? 0);
      switch (sortKey) {
        case 'created-asc':
          return createdA - createdB;
        case 'last-login-desc':
          return lastLoginB - lastLoginA || createdB - createdA;
        case 'nickname-asc':
          return compareText(a.nickname || a.username, b.nickname || b.username);
        case 'pang-desc':
          return pangB - pangA || createdB - createdA;
        case 'role-admin-first':
          return (a.role === 'ADMIN' ? 0 : 1) - (b.role === 'ADMIN' ? 0 : 1) || createdB - createdA;
        case 'status-active-first': {
          const order = { ACTIVE: 0, SUSPENDED: 1, INACTIVE: 2, DELETED: 3 };
          return (order[a.status] ?? 9) - (order[b.status] ?? 9) || createdB - createdA;
        }
        case 'created-desc':
        default:
          return createdB - createdA;
      }
    });
    return nextRows;
  }, [rows, sortKey]);

  const mergeSelectedMember = (memberId: number, patch: Partial<AdminMemberRow>) => {
    setSelectedMember((current) => (current && current.id === memberId ? { ...current, ...patch } : current));
  };

  const runMemberUpdate = async (member: AdminMemberRow, body: Parameters<typeof updateAdminMember>[1]) => {
    setSubmittingId(member.id);
    const response = await updateAdminMember(member.id, body);
    setSubmittingId(null);
    if (!response.ok) {
      window.alert(response.message ?? '회원 정보 변경에 실패했습니다.');
      return false;
    }
    await load();
    await loadHistory(member.id);
    mergeSelectedMember(member.id, {
      ...('status' in body ? { status: (response.data?.status ?? member.status) as AdminMemberRow['status'] } : {}),
      ...('role' in body ? { role: (response.data?.role ?? member.role) as AdminMemberRow['role'] } : {}),
      ...('nickname' in body ? { nickname: response.data?.nickname ?? nicknameDraft.trim() } : {}),
      suspendedUntil: response.data?.suspendedUntil ?? (body.status === 'ACTIVE' || body.status === 'INACTIVE' ? null : member.suspendedUntil),
      suspensionReason: response.data?.suspensionReason ?? (body.status === 'ACTIVE' || body.status === 'INACTIVE' ? null : member.suspensionReason),
    });
    return true;
  };

  const handleStatusChange = async (member: AdminMemberRow, status: 'ACTIVE' | 'INACTIVE') => {
    await runMemberUpdate(member, { status });
  };

  const handleRoleChange = async (member: AdminMemberRow, role: 'ADMIN' | 'USER') => {
    await runMemberUpdate(member, { role });
  };

  const handleNicknameSave = async () => {
    if (!selectedMember) return;
    if (!nicknameDraft.trim()) {
      window.alert('닉네임을 입력해 주세요.');
      return;
    }
    const ok = await runMemberUpdate(selectedMember, { nickname: nicknameDraft.trim() });
    if (ok) {
      window.alert('닉네임이 변경되었습니다.');
    }
  };

  const handleSuspendSubmit = async () => {
    if (!suspendTarget) return;
    const selected = Number(suspensionDays);
    const ok = await runMemberUpdate(suspendTarget, {
      status: 'SUSPENDED',
      suspensionDays: selected > 0 ? selected : undefined,
      permanentSuspension: selected === -1,
      suspensionReason: suspensionReason.trim(),
    });
    if (ok) {
      setSuspendTarget(null);
      setSuspensionDays('7');
      setSuspensionReason('');
    }
  };

  const submitGift = async (loginId: string, amountText: string, messageText: string, onSuccess: () => void) => {
    const trimmedLoginId = loginId.trim();
    const pangAmount = Number(amountText);
    const trimmedMessage = messageText.trim();
    if (!trimmedLoginId) {
      window.alert('아이디를 입력해 주세요.');
      return;
    }
    if (!Number.isFinite(pangAmount) || pangAmount <= 0) {
      window.alert('지급할 팡 수량을 정확히 입력해 주세요.');
      return;
    }
    const response = await giftAdminPang({ loginId: trimmedLoginId, pangAmount, message: trimmedMessage || undefined });
    if (!response.ok) {
      window.alert(response.message ?? '이벤트 팡 지급에 실패했습니다.');
      return;
    }
    await load();
    if (selectedMember && selectedMember.loginId === trimmedLoginId) {
      mergeSelectedMember(selectedMember.id, { pangBalance: response.data?.pangBalance ?? selectedMember.pangBalance });
      await loadHistory(selectedMember.id);
    }
    if (trimmedLoginId === '/all' && response.data?.recipientCount) {
      window.alert(`${response.data.recipientCount}명에게 팡을 지급했습니다.`);
      onSuccess();
      return;
    }
    window.alert(response.message ?? '이벤트 팡 지급이 완료되었습니다.');
    if (trimmedLoginId === '/all' && response.data?.recipientCount) {
      window.alert(`${response.data.recipientCount}명에게 팡을 지급했습니다.`);
      onSuccess();
      return;
    }
    onSuccess();
  };

  if (authLoading) {
    return (
      <AdminLayout
        title="회원 관리"
        description="관리자 권한과 회원 정보를 확인하는 중입니다."
      >
        <section className="admin-panel">
          <p className="admin-subtext">관리자 화면을 불러오는 중입니다.</p>
        </section>
      </AdminLayout>
    );
  }

  if (!user || user.role !== 'ADMIN') {
    return null;
  }

  return (
    <AdminLayout
      title="회원 관리"
      description="회원 검색, 정렬, 상태 변경과 상세 조회를 한 화면에서 처리합니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="아이디, 닉네임, 이메일 검색"
          />
          <select className="admin-filter-select" value={roleFilter} onChange={(event) => { setRoleFilter(event.target.value); setPage(0); }}>
            <option value="">전체 권한</option>
            <option value="USER">일반 회원</option>
            <option value="ADMIN">관리자</option>
          </select>
          <select className="admin-filter-select" value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0); }}>
            <option value="">전체 상태</option>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
            <option value="SUSPENDED">정지</option>
            <option value="DELETED">삭제</option>
          </select>
          <select className="admin-filter-select" value={sortKey} onChange={(event) => setSortKey(event.target.value)}>
            <option value="created-desc">최근 가입순</option>
            <option value="created-asc">오래된 가입순</option>
            <option value="last-login-desc">최근 로그인순</option>
            <option value="nickname-asc">닉네임 가나다순</option>
            <option value="pang-desc">팡 많은순</option>
            <option value="role-admin-first">관리자 우선</option>
            <option value="status-active-first">활성 우선</option>
          </select>
          <button type="button" className="admin-action-btn primary" onClick={() => { setQuery(queryInput.trim()); setPage(0); }}>
            검색
          </button>
          <button
            type="button"
            className="admin-action-btn"
            onClick={() => {
              setQueryInput('');
              setQuery('');
              setRoleFilter('');
              setStatusFilter('');
              setSortKey('created-desc');
              setPage(0);
            }}
          >
            초기화
          </button>
        </div>
      </section>

      <section className="admin-panel admin-detail-card admin-admin-gift-card">
        <div className="admin-section-head">
          <div>
            <h4>이벤트 팡 지급</h4>
            <p className="admin-subtext">포트원 결제 없이 즉시 팡을 지급하고 알림도 함께 보냅니다.</p>
          </div>
        </div>
        <div className="admin-inline-form">
          <input
            className="admin-search-input"
            value={giftLoginId}
            onChange={(event) => setGiftLoginId(event.target.value)}
            placeholder="회원 아이디"
          />
          <input
            className="admin-search-input admin-number-input"
            inputMode="numeric"
            value={giftAmount}
            onChange={(event) => setGiftAmount(event.target.value.replace(/[^0-9]/g, ''))}
            placeholder="지급할 팡 수량"
          />
          <input
            className="admin-search-input"
            value={giftMessage}
            onChange={(event) => setGiftMessage(event.target.value)}
            placeholder="알림 문구 입력"
          />
          <button
            type="button"
            className="admin-action-btn primary"
            onClick={() => void submitGift(giftLoginId, giftAmount, giftMessage, () => { setGiftLoginId(''); setGiftAmount(''); setGiftMessage(''); })}
          >
            바로 지급
          </button>
        </div>
      </section>

      <section className="admin-panel admin-table-card">
        <table className="data-table admin-members-table">
          <colgroup>
            <col style={{ width: '16%' }} />
            <col style={{ width: '18%' }} />
            <col style={{ width: '20%' }} />
            <col style={{ width: '10%' }} />
            <col style={{ width: '10%' }} />
            <col style={{ width: '20%' }} />
            <col style={{ width: '6%' }} />
          </colgroup>
          <thead>
            <tr>
              <th>아이디</th>
              <th>닉네임</th>
              <th>이메일</th>
              <th>권한</th>
              <th>상태</th>
              <th>가입/로그인</th>
              <th>상세</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="empty-msg">불러오는 중입니다.</td></tr>
            ) : visibleRows.length === 0 ? (
              <tr><td colSpan={7} className="empty-msg">조건에 맞는 회원이 없습니다.</td></tr>
            ) : (
              visibleRows.map((member) => {
                const roleBadge = roleMeta(member.role);
                const statusBadge = memberStatusMeta(member.status);
                return (
                  <tr key={member.id}>
                    <td>{shortenLoginId(member.loginId)}</td>
                    <td>
                      <div>{member.nickname || member.username}</div>
                      <div className="admin-subtext">{member.username}</div>
                    </td>
                    <td>{member.email}</td>
                    <td><span className={`admin-status-badge ${roleBadge.tone}`}>{roleBadge.label}</span></td>
                    <td>
                      <span className={`admin-status-badge ${statusBadge.tone}`}>{statusBadge.label}</span>
                      {member.status === 'SUSPENDED' ? (
                        <>
                          <div className="admin-subtext">
                            {member.suspendedUntil ? `해제 예정 ${formatDate(member.suspendedUntil)}` : '영구 정지'}
                          </div>
                          {member.suspensionReason ? <div className="admin-subtext">사유: {member.suspensionReason}</div> : null}
                        </>
                      ) : null}
                    </td>
                    <td>
                      <div>가입 {formatDate(member.createdAt)}</div>
                      <div className="admin-subtext">최근 로그인 {formatDate(member.lastLoginAt)}</div>
                      <div className="admin-subtext">가입경로 {member.provider}</div>
                    </td>
                    <td>
                      <button type="button" className="admin-action-btn" onClick={() => setSelectedMember(member)}>
                        상세
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>

        <div className="admin-pagination">
          <button type="button" className="admin-action-btn" disabled={result?.first ?? true} onClick={() => setPage((current) => Math.max(0, current - 1))}>
            이전
          </button>
          <span>페이지 {page + 1}{result?.totalPages ? ` / ${Math.max(result.totalPages, 1)}` : ''}</span>
          <button type="button" className="admin-action-btn" disabled={result?.last ?? true} onClick={() => setPage((current) => current + 1)}>
            다음
          </button>
        </div>
      </section>

      {selectedMember ? (
        <div className="admin-modal-backdrop" onClick={() => setSelectedMember(null)}>
          <section className="admin-modal" onClick={(event) => event.stopPropagation()}>
            <div className="admin-modal-head">
              <div>
                <h3>{selectedMember.nickname || selectedMember.username}</h3>
                <p>{shortenLoginId(selectedMember.loginId)}</p>
              </div>
              <button type="button" className="admin-action-btn" onClick={() => setSelectedMember(null)}>
                닫기
              </button>
            </div>

            <div className="admin-member-detail-grid">
              <article className="admin-detail-card">
                <h4>기본 정보</h4>
                <div className="admin-detail-list">
                  <div><strong>이메일</strong><span>{selectedMember.email || '-'}</span></div>
                  <div><strong>권한</strong><span>{roleMeta(selectedMember.role).label}</span></div>
                  <div><strong>상태</strong><span>{memberStatusMeta(selectedMember.status).label}</span></div>
                  <div><strong>정지 해제</strong><span>{selectedMember.suspendedUntil ? formatDate(selectedMember.suspendedUntil) : (selectedMember.status === 'SUSPENDED' ? '영구 정지' : '-')}</span></div>
                  <div><strong>정지 사유</strong><span>{selectedMember.suspensionReason || '-'}</span></div>
                  <div><strong>스트리머 등급</strong><span>{selectedMember.streamerTier === 'PARTNER' ? '파트너' : '일반'}</span></div>
                  <div><strong>보유 팡</strong><span>{Number(selectedMember.pangBalance ?? 0).toLocaleString()}팡</span></div>
                  <div><strong>가입 경로</strong><span>{selectedMember.provider}</span></div>
                </div>
              </article>

              <article className="admin-detail-card">
                <h4>활동 정보</h4>
                <div className="admin-detail-list">
                  <div><strong>가입일</strong><span>{formatDate(selectedMember.createdAt)}</span></div>
                  <div><strong>최근 로그인</strong><span>{formatDate(selectedMember.lastLoginAt)}</span></div>
                </div>
              </article>
            </div>

            <div className="admin-member-detail-grid admin-top-gap">
              <article className="admin-detail-card">
                <div className="admin-section-head">
                  <div>
                    <h4>닉네임 수정</h4>
                    <p className="admin-subtext">관리자 권한으로 회원 닉네임을 바로 변경합니다.</p>
                  </div>
                </div>
                <div className="admin-inline-form">
                  <input
                    className="admin-search-input"
                    value={nicknameDraft}
                    onChange={(event) => setNicknameDraft(event.target.value)}
                    placeholder="새 닉네임"
                  />
                  <button type="button" className="admin-action-btn primary" disabled={submittingId === selectedMember.id} onClick={() => void handleNicknameSave()}>
                    닉네임 저장
                  </button>
                </div>
              </article>

              <article className="admin-detail-card">
                <div className="admin-section-head">
                  <div>
                    <h4>운영 액션</h4>
                    <p className="admin-subtext">상태와 권한 변경은 상세 화면 안에서만 처리합니다.</p>
                  </div>
                </div>
                <div className="admin-actions-inline admin-actions-wrap">
                  <button type="button" className="admin-action-btn" disabled={submittingId === selectedMember.id} onClick={() => void handleStatusChange(selectedMember, 'ACTIVE')}>
                    활성
                  </button>
                  <button type="button" className="admin-action-btn" disabled={submittingId === selectedMember.id} onClick={() => void handleStatusChange(selectedMember, 'INACTIVE')}>
                    비활성
                  </button>
                  <button
                    type="button"
                    className="admin-action-btn"
                    disabled={submittingId === selectedMember.id}
                    onClick={() => {
                      setSuspendTarget(selectedMember);
                      setSuspensionDays('7');
                      setSuspensionReason(selectedMember.suspensionReason ?? '');
                    }}
                  >
                    정지 설정
                  </button>
                  <button
                    type="button"
                    className="admin-action-btn"
                    disabled={submittingId === selectedMember.id}
                    onClick={() => void handleRoleChange(selectedMember, selectedMember.role === 'ADMIN' ? 'USER' : 'ADMIN')}
                  >
                    {selectedMember.role === 'ADMIN' ? '일반 전환' : '관리자 전환'}
                  </button>
                </div>
              </article>
            </div>

            <article className="admin-history-card admin-top-gap">
              <div className="admin-section-head">
                <div>
                  <h4>회원 팡 지급</h4>
                  <p className="admin-subtext">선택한 회원에게 운영자 선물 알림과 함께 팡을 즉시 지급합니다.</p>
                </div>
              </div>
              <div className="admin-inline-form">
                <input className="admin-search-input" value={selectedMember.loginId} readOnly />
                <input
                  className="admin-search-input admin-number-input"
                  inputMode="numeric"
                  value={modalGiftAmount}
                  onChange={(event) => setModalGiftAmount(event.target.value.replace(/[^0-9]/g, ''))}
                  placeholder="지급할 팡 수량"
                />
                <input
                  className="admin-search-input"
                  value={modalGiftMessage}
                  onChange={(event) => setModalGiftMessage(event.target.value)}
                  placeholder="알림 문구 입력"
                />
                <button
                  type="button"
                  className="admin-action-btn primary"
                  onClick={() => void submitGift(selectedMember.loginId, modalGiftAmount, modalGiftMessage, () => { setModalGiftAmount(''); setModalGiftMessage(''); })}
                >
                  팡 지급
                </button>
              </div>
            </article>

            <article className="admin-history-card">
              <div className="admin-history-head">
                <h4>변경 이력</h4>
                <p>백엔드 DB에 저장된 회원 상태, 권한, 닉네임, 운영자 지급 이력입니다.</p>
              </div>
              <div className="admin-history-list">
                {historyLoading ? (
                  <p className="admin-subtext">이력을 불러오는 중입니다.</p>
                ) : history.length === 0 ? (
                  <p className="admin-subtext">기록된 변경 이력이 없습니다.</p>
                ) : (
                  history.map((item) => (
                    <div key={item.id}>
                      <strong>{item.summary}</strong>
                      <span>{formatDate(item.createdAt)}</span>
                      <p className="admin-subtext">{item.detail || '-'}</p>
                    </div>
                  ))
                )}
              </div>
            </article>
          </section>
        </div>
      ) : null}

      {suspendTarget ? (
        <div className="admin-modal-backdrop" onClick={() => setSuspendTarget(null)}>
          <section className="admin-modal admin-modal-compact" onClick={(event) => event.stopPropagation()}>
            <div className="admin-modal-head">
              <div>
                <h3>회원 정지 설정</h3>
                <p>{suspendTarget.loginId}</p>
              </div>
              <button type="button" className="admin-action-btn" onClick={() => setSuspendTarget(null)}>
                닫기
              </button>
            </div>

            <div className="admin-detail-list">
              <div>
                <strong>정지 기간</strong>
                <select className="admin-filter-select" value={suspensionDays} onChange={(event) => setSuspensionDays(event.target.value)}>
                  {suspensionOptions.map((option) => (
                    <option key={option.label} value={String(option.days)}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <textarea
              className="admin-note-input"
              rows={4}
              value={suspensionReason}
              onChange={(event) => setSuspensionReason(event.target.value)}
              placeholder="정지 사유 입력"
            />

            <div className="admin-pagination">
              <button type="button" className="admin-action-btn" onClick={() => setSuspendTarget(null)}>
                취소
              </button>
              <button type="button" className="admin-action-btn primary" disabled={submittingId === suspendTarget.id} onClick={() => void handleSuspendSubmit()}>
                정지 적용
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </AdminLayout>
  );
}
