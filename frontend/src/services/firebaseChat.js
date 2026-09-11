import { getApp, getApps, initializeApp } from "firebase/app";
import { getAuth, signInWithCustomToken, signOut } from "firebase/auth";
import {
  get,
  getDatabase,
  increment,
  limitToLast,
  onValue,
  orderByChild,
  push,
  query,
  ref,
  serverTimestamp,
  update,
} from "firebase/database";
import { endpoints, authApis } from "../configs/Apis";
import { apiData } from "../utils/api";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

const requiredConfig = ["apiKey", "authDomain", "databaseURL", "projectId", "appId"];
let sessionPromise;

const describeFirebaseAuthError = (error) => {
  if (error?.code === "auth/configuration-not-found") {
    return new Error(
      "Firebase Authentication chưa được khởi tạo. Hãy mở Firebase Console > Authentication và nhấn Get started.",
      { cause: error },
    );
  }
  if (error?.code === "auth/unauthorized-domain") {
    return new Error(
      "Tên miền hiện tại chưa được phép trong Firebase Authentication > Settings > Authorized domains.",
      { cause: error },
    );
  }
  return error;
};

const getFirebase = () => {
  const missing = requiredConfig.filter((key) => !firebaseConfig[key]);
  if (missing.length) throw new Error(`Thiếu cấu hình Firebase: ${missing.join(", ")}`);
  const app = getApps().length ? getApp() : initializeApp(firebaseConfig);
  return { auth: getAuth(app), database: getDatabase(app) };
};

export const connectFirebaseChat = async () => {
  if (!sessionPromise) {
    sessionPromise = (async () => {
      const response = await authApis().post(endpoints["firebase-chat-token"]);
      const identity = apiData(response);
      const firebase = getFirebase();
      await signInWithCustomToken(firebase.auth, identity.customToken);
      return { ...firebase, identity };
    })().catch((error) => {
      sessionPromise = undefined;
      throw describeFirebaseAuthError(error);
    });
  }
  return sessionPromise;
};

export const disconnectFirebaseChat = async () => {
  try {
    const { auth } = getFirebase();
    await signOut(auth);
  } catch {
    // Firebase may be intentionally disabled in local environments.
  } finally {
    sessionPromise = undefined;
  }
};

const chatRoot = (consultantUid, studentUid) =>
  `chats/${consultantUid}/${studentUid}`;

export const ensureStudentConversation = async (session, email) => {
  const { database, identity } = session;
  const metadataRef = ref(
    database,
    `${chatRoot(identity.consultantUid, identity.uid)}/metadata`,
  );
  const snapshot = await get(metadataRef);
  if (!snapshot.exists()) {
    await update(metadataRef, {
      studentUid: identity.uid,
      studentName: identity.fullName,
      studentEmail: email,
      consultantUid: identity.consultantUid,
      consultantName: identity.consultantName,
      lastMessage: "",
      lastMessageAt: serverTimestamp(),
      unreadStudent: 0,
      unreadConsultant: 0,
    });
  }
};

export const subscribeMessages = (session, studentUid, onMessages) => {
  const { database, identity } = session;
  const messagesQuery = query(
    ref(database, `${chatRoot(identity.consultantUid, studentUid)}/messages`),
    orderByChild("createdAt"),
    limitToLast(100),
  );
  return onValue(messagesQuery, (snapshot) => {
    const messages = [];
    snapshot.forEach((child) => messages.push({ id: child.key, ...child.val() }));
    onMessages(messages);
  });
};

export const subscribeConsultantConversations = (session, onConversations) => {
  const conversationsRef = ref(session.database, `chats/${session.identity.uid}`);
  return onValue(conversationsRef, (snapshot) => {
    const conversations = [];
    snapshot.forEach((child) => {
      const metadata = child.child("metadata").val();
      if (metadata) conversations.push({ studentUid: child.key, ...metadata });
    });
    conversations.sort((a, b) => (b.lastMessageAt || 0) - (a.lastMessageAt || 0));
    onConversations(conversations);
  });
};

export const sendChatMessage = async (session, studentUid, text) => {
  const cleanText = text.trim();
  if (!cleanText || cleanText.length > 2000) {
    throw new Error("Tin nhắn phải có từ 1 đến 2.000 ký tự");
  }
  const { database, identity } = session;
  const root = chatRoot(identity.consultantUid, studentUid);
  const messageKey = push(ref(database, `${root}/messages`)).key;
  const recipientCounter = identity.role === "STUDENT" ? "unreadConsultant" : "unreadStudent";
  await update(ref(database), {
    [`${root}/messages/${messageKey}`]: {
      senderUid: identity.uid,
      senderRole: identity.role,
      senderName: identity.fullName,
      text: cleanText,
      createdAt: serverTimestamp(),
    },
    [`${root}/metadata/lastMessage`]: cleanText,
    [`${root}/metadata/lastMessageAt`]: serverTimestamp(),
    [`${root}/metadata/${recipientCounter}`]: increment(1),
  });
};

export const markConversationRead = async (session, studentUid) => {
  const field = session.identity.role === "STUDENT" ? "unreadStudent" : "unreadConsultant";
  await update(
    ref(
      session.database,
      `${chatRoot(session.identity.consultantUid, studentUid)}/metadata`,
    ),
    { [field]: 0 },
  );
};
