export type ChatbotStarterPrompt = {
  label: string;
  prompt: string;
};

export const CHATBOT_STARTER_PROMPTS: ChatbotStarterPrompt[] = [
  { label: '전적 검색 하는 방법', prompt: '전적 검색 하는 방법 알려줘' },
  { label: '계정 연동 방법', prompt: '외부 계정 연동은 어떻게 해?' },
  { label: '프로필 수정 위치', prompt: '프로필은 어디서 수정해?' },
  { label: '1:1 채팅 시작', prompt: '친구랑 1:1 채팅은 어떻게 시작해?' },
  { label: '후원 하는 법', prompt: '후원 하는 법 알려줘' },
  { label: '방송 시작 위치', prompt: '방송은 어디서 시작해?' },
  { label: '사이트 설명', prompt: '이 사이트가 어떤 서비스인지 설명해줘' },
];
