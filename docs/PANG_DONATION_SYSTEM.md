# 팡 후원 시스템 전체 구조 (결제 / 정산 / DB / API)

트위치 스타일 스트리밍 후원 구조를 GameMatcher에 맞게 정리한 설계 문서입니다.

---

## 1. 개요

- **팡**: 플랫폼 내 가상 화폐. 시청자가 스트리머에게 후원할 때 사용.
- **등급**: 100~999팡 → **팡**, 1,000~9,999팡 → **슈퍼팡**, 10,000팡 이상 → **메가팡** (화면 알림/채팅 표시용).

---

## 2. DB 구조

### 2.1 테이블

| 테이블 | 용도 |
|--------|------|
| **users** | `pang_balance` — 사용자별 팡 잔액 |
| **donations** | 후원 내역 (from_user_id, stream_id, to_user_id, amount, message, created_at) |
| **pang_charges** | 팡 충전 내역 (user_id, pang_amount, price_won, created_at) |

### 2.2 정산용 확장 (추후)

- **streamer_earnings** (또는 donations 집계 뷰): 스트리머별·기간별 후원 합계.
- **withdrawals**: 출금 요청 (user_id, amount_won, status, requested_at, paid_at).

---

## 3. API 구조

### 3.1 현재 구현

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/pang/balance` | 로그인 사용자 팡 잔액 조회 |
| POST | `/api/pang/charge` | 팡 충전 (body: `amount`) |
| POST | `/api/donate` | 후원 (body: `streamId`, `amount`, `message`) → 성공 시 `donorName`, `amount`, `tier` 반환 |

### 3.2 추천 확장 (트위치 수준)

- **GET** `/api/donate/history` — 내가 보낸/받은 후원 목록 (쿼리: type=sent|received, streamId, page)
- **GET** `/api/streams/{streamId}/donations` — 해당 방송 최근 후원 목록 (실시간 알림/채팅 연동)
- **GET** `/api/me/earnings` — 스트리머 본인 수익 요약 (기간별 합계)
- **POST** `/api/me/withdraw` — 출금 요청 (정산 정책 확정 후)

---

## 4. 결제 흐름 (팡 충전)

1. 사용자: 프로필/충전 UI에서 충전 팡 수 입력.
2. **POST** `/api/pang/charge` → `PangService.charge()`:
   - 사용자 검증, 1~999,999 검사.
   - `users.pang_balance` 증가.
   - `pang_charges`에 기록 (userId, pangAmount, priceWon).
3. 실제 결제(PG 연동)는 현재 없을 수 있음. 도입 시:
   - 클라이언트에서 PG 결제 완료 후 `/api/pang/charge` 호출 시 결제 ID 전달.
   - 서버에서 결제 ID 검증 후 잔액 증가 및 `pang_charges` 저장.

---

## 5. 후원 흐름 (팡 사용)

1. 시청자: 시청 페이지에서 "후원하기" → 금액·메시지 입력 → **POST** `/api/donate`.
2. **DonationService.donate()**:
   - 후원자 잔액 차감, 본인 방송 여부 검사.
   - `donations`에 저장.
   - 응답: `DonationResponse` (message, donorName, amount, tier).
3. 프론트:
   - 화면 알림: "OOO님이 N팡 후원! 🎉" + 등급(팡/슈퍼팡/메가팡).
   - 팡 터짐 애니메이션 재생.
   - 채팅 영역에 "팡!" 효과 메시지 추가.

---

## 6. 정산 흐름 (스트리머 수익 / 출금)

- **수익 집계**: `donations`에서 `to_user_id`별 `amount` 합계 (기간별로 조회).
- **정산 주기**: 월 1회 등 플랫폼 정책에 따라 `streamer_earnings` 또는 집계 API로 계산.
- **출금**: 스트리머가 "출금 요청" → 관리자 승인 또는 PG 출금 API 호출 → `withdrawals` 테이블에 상태 저장.

(현재 코드에는 출금·정산 API 미구현. 위는 확장 시 권장 구조.)

---

## 7. 실시간 후원 알림 (다중 시청자)

- 현재: 후원한 클라이언트에서만 화면 알림·채팅 "팡!" 표시.
- 트위치처럼 같은 방송의 **모든 시청자**에게 알림을 보내려면:
  - **WebSocket** 또는 **SSE**: 방송별 채널/스트림으로 "후원 이벤트" 푸시.
  - 후원 API 처리 후 해당 방송 구독자에게 `{ donorName, amount, tier }` 브로드캐스트.

---

## 8. 요약

| 구분 | 내용 |
|------|------|
| **결제** | 팡 충전: `/api/pang/charge`, `pang_charges` 저장, (선택) PG 연동 |
| **후원** | `/api/donate` → 잔액 차감, `donations` 저장, 화면 알림·팡 터짐·채팅 "팡!" |
| **DB** | users.pang_balance, donations, pang_charges (+ 추후 정산/출금 테이블) |
| **확장** | 후원 히스토리 API, 스트리머 수익/출금 API, WebSocket 실시간 알림 |
