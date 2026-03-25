# GameMatcher Frontend

Spring Boot(:8080) API 서버와 연동하는 **Vite + React** 프론트엔드입니다.

## 🚀 빠른 시작

```bash
# 1. 이 폴더에서
cd gamematcher-frontend

# 2. 패키지 설치
npm install

# 3. Spring Boot 먼저 실행한 뒤 → 개발 서버 시작
npm run dev
# → http://localhost:3000
# → /api/* 요청은 자동으로 localhost:8080 프록시
```

## 📦 프로덕션 빌드

```bash
npm run build
# → ../GameMatcher/src/main/resources/static/ 에 자동 복사
# Spring Boot 재시작 후 http://localhost:8080 에서 서비스
```

## 📁 구조

```
src/
├── main.jsx                   진입점
├── App.jsx                    React Router 라우팅
├── constants.js               게임·카테고리 상수
├── api/index.js               API 호출 함수 모음
├── hooks/useUser.jsx          전역 userId Context
├── utils/format.js            날짜 포맷
├── styles/global.css          CSS 변수 + 기본 스타일
├── components/
│   ├── layout/
│   │   ├── Layout.jsx         Outlet 래퍼
│   │   └── Header.jsx         상단 헤더 (UID 변경 포함)
│   ├── ui/                    공통 컴포넌트
│   │   ├── Button             primary·ghost·subtle·danger
│   │   ├── Spinner            로딩 점 애니메이션
│   │   ├── Empty              빈 상태
│   │   ├── ErrorBox           에러 메시지
│   │   ├── Badge              컬러 뱃지
│   │   └── Pagination         페이지 버튼
│   └── stats/
│       ├── StatsResult.jsx    전적 결과 카드
│       └── MatchCard.jsx      매치 한 줄
└── pages/
    ├── HomePage.jsx           홈
    ├── StatsPage.jsx          전적 검색
    ├── CommunityListPage.jsx  게시판 목록 + 사이드바
    ├── CommunityPostPage.jsx  게시글 상세 + 댓글
    └── CommunityWritePage.jsx 글쓰기 / 수정
```

## 🔗 라우팅

| URL | 페이지 |
|-----|--------|
| `/` | 홈 |
| `/stats` | 전적 검색 |
| `/community` | 자유게시판 |
| `/community/:category` | 카테고리 게시판 (FREE·NOTICE·QUESTION·LOL·VALORANT·PUBG) |
| `/community/post/:id` | 게시글 상세 |
| `/community/write` | 글쓰기 |
| `/community/edit/:id` | 글 수정 |
