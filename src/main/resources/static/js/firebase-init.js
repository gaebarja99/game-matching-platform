/**
 * Firebase 초기화 (Analytics, Auth 포함)
 * https://firebase.google.com/docs/web/setup
 */
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.7.0/firebase-app.js';
import { getAnalytics } from 'https://www.gstatic.com/firebasejs/10.7.0/firebase-analytics.js';
import { getAuth } from 'https://www.gstatic.com/firebasejs/10.7.0/firebase-auth.js';

// API 키는 Firebase 콘솔 [프로젝트 설정 → 일반 → 내 앱 → SDK 설정] 에서 복사한 값과 동일해야 함
const firebaseConfig = {
  apiKey: 'AIzaSyDMUCfU5LTTLqI_uJp5LVu5b-9UmNVwmUQ',
  authDomain: 'gamematcher-ddec4.firebaseapp.com',
  projectId: 'gamematcher-ddec4',
  storageBucket: 'gamematcher-ddec4.firebasestorage.app',
  messagingSenderId: '879767507887',
  appId: '1:879767507887:web:f98e40d5c8881fd6d2d55a',
  measurementId: 'G-4WQTGPMLTE'
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
const auth = getAuth(app);

// 다른 스크립트에서 사용할 수 있도록 전역에 노출 (선택)
if (typeof window !== 'undefined') {
  window.firebaseApp = app;
  window.firebaseAnalytics = analytics;
  window.firebaseAuth = auth;
}

export { app, analytics, auth };
