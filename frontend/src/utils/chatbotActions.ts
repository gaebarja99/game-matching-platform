import { OUT_OF_SCOPE_REPLY } from './chatbotText';

export type ChatbotAction = {
  label: string;
  to: string;
};

const ACTION_MAP: Array<{
  keywords: string[];
  actions: ChatbotAction[];
}> = [
  {
    keywords: ['전적', '기록', '검색', '발로란트', '롤', 'tft', '배그', 'pubg', '오버워치', 'cs2', '카스'],
    actions: [
      { label: '전적 검색으로 이동', to: '/records' },
    ],
  },
  {
    keywords: ['배그', 'pubg', '카카오', 'steam', '플랫폼'],
    actions: [
      { label: '전적 검색으로 이동', to: '/records' },
      { label: '외부 계정 연동', to: '/profile/account-links' },
    ],
  },
  {
    keywords: ['연동', '계정연동', '디스코드', 'discord', '스팀연동', '블리자드', '라이엇', 'riot'],
    actions: [
      { label: '외부 계정 연동', to: '/profile/account-links' },
      { label: '내 정보로 이동', to: '/profile' },
    ],
  },
  {
    keywords: ['프로필', '내정보', '마이페이지', '닉네임', '소개'],
    actions: [
      { label: '내 정보로 이동', to: '/profile' },
      { label: '내 정보 관리', to: '/profile/my-info' },
      { label: '외부 계정 연동', to: '/profile/account-links' },
    ],
  },
  {
    keywords: ['커뮤니티', '게시글', '글쓰기', '공지', '질문글'],
    actions: [
      { label: '커뮤니티로 이동', to: '/community' },
      { label: '커뮤니티 글쓰기', to: '/community/write' },
    ],
  },
  {
    keywords: ['채팅', 'dm', '친구', '알림', '1:1', '단체채팅', '랜덤채팅', '게임방', '매칭'],
    actions: [
      { label: '1:1 채팅으로 이동', to: '/dm' },
      { label: '단체 채팅으로 이동', to: '/group-chat' },
      { label: '게임방으로 이동', to: '/game-rooms' },
    ],
  },
  {
    keywords: ['후원', '팡', '결제', '구독', '광고제거', 'adfree', '마일리지'],
    actions: [
      { label: '팡 페이지로 이동', to: '/profile/pang' },
      { label: '구독 관리', to: '/profile/subscriptions' },
      { label: '광고 제거', to: '/profile/adfree' },
    ],
  },
  {
    keywords: ['방송', '스트림', '스튜디오', 'obs', '스트리밍', '후원알림', '수익'],
    actions: [
      { label: '스튜디오로 이동', to: '/studio' },
      { label: '방송 시작하기', to: '/studio/live' },
      { label: '스트림 목록 보기', to: '/streams' },
    ],
  },
  {
    keywords: ['차단', '신고', '블락', 'block'],
    actions: [
      { label: '1:1 채팅으로 이동', to: '/dm' },
      { label: '스튜디오 차단 목록', to: '/studio/viewers/blocklist' },
      { label: '커뮤니티로 이동', to: '/community' },
    ],
  },
  {
    keywords: ['로그인', '회원가입', '가입', '로그아웃'],
    actions: [
      { label: '로그인으로 이동', to: '/login' },
      { label: '내 정보로 이동', to: '/profile' },
    ],
  },
];

function normalize(value: string) {
  return value.toLowerCase().replace(/\s+/g, '');
}

export function deriveChatbotActions(text: string): ChatbotAction[] {
  const normalized = normalize(text ?? '');
  if (normalized.includes(normalize(OUT_OF_SCOPE_REPLY))) {
    return [];
  }

  const collected: ChatbotAction[] = [];

  ACTION_MAP.forEach(({ keywords, actions }) => {
    if (!keywords.some((keyword) => normalized.includes(normalize(keyword)))) {
      return;
    }

    actions.forEach((action) => {
      if (!collected.some((item) => item.to === action.to)) {
        collected.push(action);
      }
    });
  });

  return collected.slice(0, 3);
}
