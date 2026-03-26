export type ChatbotSuggestionCategory = {
  id: string;
  title: string;
  description: string;
  prompts: Array<{
    label: string;
    prompt: string;
  }>;
};

export const CHATBOT_SUGGESTION_CATEGORIES: ChatbotSuggestionCategory[] = [
  {
    id: 'matching-help',
    title: '매칭하는법',
    description: '게임방 만들기, 참가, 랜덤 매칭 같은 흐름은 채팅으로 이어서 물어볼 수 있어요.',
    prompts: [
      { label: '매칭하는법', prompt: '매칭은 어떻게 하는지 알려줘' },
    ],
  },
  {
    id: 'account-links',
    title: '계정연동',
    description: 'Discord, Steam, Blizzard, Riot 연동 관련 세부 내용은 채팅으로 더 물어보면 돼요.',
    prompts: [
      { label: '계정연동', prompt: '외부 계정 연동은 어떻게 해?' },
    ],
  },
  {
    id: 'support-payments',
    title: '후원방법',
    description: '팡, 결제 오류, 마일리지, 구독 같은 건 채팅으로 바로 이어서 물어볼 수 있어요.',
    prompts: [
      { label: '후원방법', prompt: '후원 하는 법 알려줘' },
    ],
  },
  {
    id: 'studio-start',
    title: '방송시작',
    description: '스트림 키, OBS 연결, 알림, 채팅 설정 같은 건 채팅으로 이어서 안내받을 수 있어요.',
    prompts: [
      { label: '방송시작', prompt: '방송은 어디서 시작해?' },
    ],
  },
];
