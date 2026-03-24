import { apiUrl } from '../api/client';
import { getFirebaseApp } from '../firebase';

type MessagePayload = {
  notification?: {
    title?: string;
    body?: string;
  };
  data?: Record<string, string>;
};

let currentToken: string | null = null;
let detachForegroundListener: (() => void) | null = null;

function getSwUrl(): string {
  const params = new URLSearchParams({
    apiKey: String(import.meta.env.VITE_FIREBASE_API_KEY ?? ''),
    authDomain: String(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ?? ''),
    projectId: String(import.meta.env.VITE_FIREBASE_PROJECT_ID ?? ''),
    storageBucket: String(import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? ''),
    messagingSenderId: String(import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID ?? ''),
    appId: String(import.meta.env.VITE_FIREBASE_APP_ID ?? ''),
  });
  return `/firebase-messaging-sw.js?${params.toString()}`;
}

async function registerTokenToServer(token: string): Promise<void> {
  await fetch(apiUrl('api/push/token'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({
      token,
      platform: 'web',
    }),
  });
}

export async function setupPushNotifications(): Promise<void> {
  if (typeof window === 'undefined') return;
  if (!('Notification' in window) || !('serviceWorker' in navigator)) return;

  const vapidKey = String(import.meta.env.VITE_FIREBASE_VAPID_KEY ?? '').trim();
  if (!vapidKey) return;

  const app = await getFirebaseApp();
  if (!app) return;

  const messagingModule = await import('firebase/messaging');
  const supported = await messagingModule.isSupported().catch(() => false);
  if (!supported) return;

  const permission = Notification.permission === 'granted'
    ? 'granted'
    : await Notification.requestPermission();
  if (permission !== 'granted') return;

  const registration = await navigator.serviceWorker.register(getSwUrl());
  const messaging = messagingModule.getMessaging(app as never);
  const token = await messagingModule.getToken(messaging, {
    vapidKey,
    serviceWorkerRegistration: registration,
  });

  if (!token) return;
  if (token !== currentToken) {
    await registerTokenToServer(token);
    currentToken = token;
  }

  if (!detachForegroundListener) {
    detachForegroundListener = messagingModule.onMessage(messaging, (payload: MessagePayload) => {
      const detail = {
        title: payload.notification?.title ?? payload.data?.title ?? '알림',
        body: payload.notification?.body ?? payload.data?.body ?? '',
        url: payload.data?.url ?? '/',
      };

      window.dispatchEvent(new CustomEvent('gm:push-received', { detail }));
    });
  }
}

export async function clearPushToken(): Promise<void> {
  if (!currentToken) return;
  try {
    await fetch(apiUrl(`api/push/token?token=${encodeURIComponent(currentToken)}`), {
      method: 'DELETE',
      credentials: 'include',
    });
  } catch {
    // ignore
  } finally {
    currentToken = null;
  }
}

