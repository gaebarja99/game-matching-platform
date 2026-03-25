# 포트원(아임포트) 결제 연동

팡 충전은 포트원(아임포트)을 통해 실결제 후 서버에서 검증·팡 지급 방식으로 동작합니다.

## 설정

1. [포트원 콘솔](https://admin.portone.io)에서 가맹점 등록 후 다음 값을 확인합니다.
   - **가맹점 식별자** (store-id): 프론트엔드 `IMP.init()` 에 사용
   - **REST API 키** (api-key): 서버 결제 검증 시 토큰 발급용
   - **REST API Secret** (api-secret): 서버 결제 검증 시 토큰 발급용

2. `src/main/resources/application.properties` (또는 환경 변수)에 설정합니다.

```properties
payment.portone.api-key=발급받은_REST_API_키
payment.portone.api-secret=발급받은_REST_API_시크릿
payment.portone.store-id=가맹점_식별자
```

3. 포트원 콘솔에서 **테스트 모드**로 결제 테스트 가능. 실제 결제 시 PG사·가맹점 계약 후 라이브 키로 교체합니다.

## 흐름

1. 사용자가 프로필 > 팡 충전에서 금액 입력 후 **충전하기** 클릭
2. 프론트: `POST /api/payment/orders` → 서버가 주문 생성 후 `orderId`, `amount`, `orderName`, `storeId` 반환
3. 프론트: `IMP.init(storeId)` 후 `IMP.request_pay({ merchant_uid: orderId, amount, name })` 로 결제창 호출
4. 사용자가 결제 완료 시 콜백으로 `imp_uid`, `merchant_uid` 수신
5. 프론트: `POST /api/payment/confirm` body `{ orderId, impUid }` 로 전달
6. 서버: 포트원 API로 `imp_uid` 결제 정보 조회 → 금액·주문번호 일치 시 `PangService.charge()` 호출 후 팡 지급

## 결제 미설정 시

`payment.portone.api-key` 등이 비어 있으면 **결제가 설정되지 않았습니다** 로 응답하며, 프론트는 기존처럼 `POST /api/pang/charge` 직접 충전 API로 폴백합니다. (개발·테스트용)
