import { doc, getDocFromServer } from 'firebase/firestore';
import { auth, db, firebaseApp, storage } from '../../lib/firebase';

export { auth, db, firebaseApp, storage };

export async function testFirestoreConnection(): Promise<{
  connected: boolean;
  error?: string;
}> {
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
    return { connected: true };
  } catch (error) {
    if (error instanceof Error && error.message.includes('the client is offline')) {
      console.error('Please check your Firebase configuration.');
      return {
        connected: false,
        error: 'Firestore client is offline. Please verify Firebase configuration.',
      };
    }
    return { connected: true };
  }
}
