import { apiFetch } from './client';

export interface ChatResponse {
  reply: string;
  aiEnabled: boolean;
}

export async function sendChatMessage(message: string): Promise<ChatResponse> {
  const response = await apiFetch<ChatResponse>('/api/chat', {
    method: 'POST',
    body: JSON.stringify({ message }),
  });

  if (!response.ok || !response.data) {
    throw new Error(response.message ?? '챗봇 응답을 불러오지 못했습니다.');
  }

  return response.data;
}
