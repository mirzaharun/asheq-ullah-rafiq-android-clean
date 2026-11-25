import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

export const FIREBASE_PROJECT_ID =
  import.meta.env.VITE_FIREBASE_PROJECT_ID || 'asheq-ullah-rafiq-digital-p';

export const FIRESTORE_DATABASE_ID =
  import.meta.env.VITE_FIRESTORE_DATABASE_ID ||
  'ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943';

export const FIREBASE_STORAGE_BUCKET =
  import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ||
  'asheq-ullah-rafiq-digital-p.firebasestorage.app';

const firebaseConfig = {
  apiKey:
    import.meta.env.VITE_FIREBASE_API_KEY ||
    'AIzaSyBqcxIYg4ItgyYoXD4ybXUcqVZDvtoHTFI',
  authDomain:
    import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ||
    'asheq-ullah-rafiq-digital-p.firebaseapp.com',
  projectId: FIREBASE_PROJECT_ID,
  storageBucket: FIREBASE_STORAGE_BUCKET,
  messagingSenderId:
    import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '982402541543',
  appId:
    import.meta.env.VITE_FIREBASE_APP_ID ||
    '1:982402541543:android:05b4cc120e18bc4f8e3f42',
};

export const firebaseApp = getApps().length
  ? getApp()
  : initializeApp(firebaseConfig);

export const auth = getAuth(firebaseApp);

// Always bind explicitly to the existing named Firestore database (never '(default)').
export const db = getFirestore(firebaseApp, FIRESTORE_DATABASE_ID);

export const storage = getStorage(firebaseApp);
