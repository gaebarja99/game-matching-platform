/**
 * Firebase Auth 오류 메시지/코드를 한글 사용자 메시지로 변환합니다.
 */
const MAP: Record<string, string> = {
  'auth/invalid-app-credential': '보안 검증에 실패했습니다. Firebase 인증 도메인(localhost/127.0.0.1)과 전화 인증 설정을 확인한 뒤 다시 시도해 주세요.',
  'auth/too-many-requests': '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
  'auth/network-request-failed': '네트워크 오류가 발생했습니다.',
  'auth/invalid-verification-code': '인증번호가 올바르지 않거나 만료되었습니다.',
  'auth/code-expired': '인증번호가 만료되었습니다. 다시 요청해 주세요.',
  'auth/session-expired': '인증 세션이 만료되었습니다. 처음부터 다시 시도해 주세요.',
  'auth/captcha-check-failed': '보안 검증에 실패했습니다. 다시 시도해 주세요.',
  'auth/invalid-phone-number': '올바른 휴대폰 번호를 입력해 주세요.',
  'auth/missing-phone-number': '휴대폰 번호를 입력해 주세요.',
  'auth/quota-exceeded': '일시적으로 인증 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.',
};

export function getFirebaseAuthErrorMessage(err: unknown): string {
  if (err == null) return '인증에 실패했습니다.';
  const msg = typeof (err as { message?: string }).message === 'string'
    ? (err as { message: string }).message
    : String(err);
  if (msg.toLowerCase().includes('recaptcha has already been rendered')) {
    return '보안 검증 초기화에 실패했습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.';
  }
  if (msg.toLowerCase().includes('failed to initialize recaptcha enterprise')) {
    return 'reCAPTCHA 초기화에 실패했습니다. Firebase 설정(localhost/127.0.0.1 허용 도메인)을 확인해 주세요.';
  }
  const code = msg.includes('auth/') ? msg.replace(/^.*(auth\/[a-z-]+).*$/i, '$1') : '';
  if (code && MAP[code]) return MAP[code];
  if (msg.includes('auth/')) return MAP['auth/invalid-app-credential'] ?? '인증에 실패했습니다.';
  return msg;
}
