import type { FirebaseApp, FirebaseOptions } from 'firebase/app';
import type { Auth } from 'firebase/auth';

let appInstance: FirebaseApp | null = null;
let authInstance: Auth | null = null;

function getFirebaseConfig(): FirebaseOptions {
  return {
    apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
    authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
    projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
    storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
    messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
    appId: import.meta.env.VITE_FIREBASE_APP_ID,
    ...(import.meta.env.VITE_FIREBASE_MEASUREMENT_ID && {
      measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
    }),
  };
}

function hasFirebaseConfig(cfg: ReturnType<typeof getFirebaseConfig>): boolean {
  return Boolean(cfg.apiKey && String(cfg.apiKey).trim() !== '' && cfg.appId && String(cfg.appId).trim() !== '');
}

export async function getFirebaseApp(): Promise<FirebaseApp | null> {
  if (appInstance) return appInstance;

  const config = getFirebaseConfig();
  if (!hasFirebaseConfig(config)) return null;

  try {
    const firebaseApp = await import('firebase/app');
    const apps = firebaseApp.getApps();
    appInstance = apps.length > 0 ? apps[0] : firebaseApp.initializeApp(config);
    return appInstance;
  } catch {
    return null;
  }
}

export async function getAuth(): Promise<Auth | null> {
  if (authInstance) return authInstance;

  const app = await getFirebaseApp();
  if (!app) return null;

  try {
    const firebaseAuth = await import('firebase/auth');
    authInstance = firebaseAuth.getAuth(app);
    return authInstance;
  } catch {
    return null;
  }
}
