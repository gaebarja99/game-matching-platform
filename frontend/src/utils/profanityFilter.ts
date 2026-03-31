const PROFANITY_KEYWORDS = [
  '시발',
  '씨발',
  'ㅅㅂ',
  '병신',
  '븅신',
  '개새끼',
  '좆',
  '존나',
  '지랄',
  '꺼져',
  '닥쳐',
  '미친놈',
  '미친년',
  'fuck',
  'shit',
  'bitch',
  'asshole',
];

function normalizeText(text: string): string {
  return text
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/[^a-z0-9가-힣ㄱ-ㅎㅏ-ㅣ]/g, '');
}

export function containsProfanity(text: string): boolean {
  const normalized = normalizeText(text);
  if (!normalized) return false;
  return PROFANITY_KEYWORDS.some((keyword) => normalized.includes(keyword));
}

export const PROFANITY_BLOCK_MESSAGE = '욕설이 포함된 메시지는 보낼 수 없습니다.';
