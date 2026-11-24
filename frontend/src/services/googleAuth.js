import { getApps, initializeApp } from "firebase/app";
import {
  browserPopupRedirectResolver,
  GoogleAuthProvider,
  initializeAuth,
  inMemoryPersistence,
  signInWithPopup,
  signOut,
} from "firebase/auth";
import { firebaseConfig } from "../configs/firebase";

let googleAuth;

export async function getGoogleIdToken() {
  if (["apiKey", "authDomain", "projectId", "appId"].some((key) => !firebaseConfig[key])) {
    throw new Error("Đăng nhập Google chưa được cấu hình. Vui lòng đăng nhập bằng email và mật khẩu.");
  }
  if (!googleAuth) {
    // This app never replaces the default app's lcm-user-* chat session.
    const app = getApps().find((app) => app.name === "google-sign-in")
      || initializeApp(firebaseConfig, "google-sign-in");
    googleAuth = initializeAuth(app, {
      persistence: inMemoryPersistence,
      popupRedirectResolver: browserPopupRedirectResolver,
    });
  }
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  try {
    const credential = await signInWithPopup(googleAuth, provider);
    return await credential.user.getIdToken();
  } finally {
    // Only the application's JWT is persisted after the backend accepts this token.
    await signOut(googleAuth).catch(() => {});
  }
}

export function googleAuthError(error) {
  const messages = {
    "auth/popup-blocked": "Trình duyệt đã chặn cửa sổ Google. Hãy cho phép cửa sổ bật lên và thử lại.",
    "auth/unauthorized-domain": "Tên miền này chưa được cho phép đăng nhập Google. Vui lòng liên hệ trung tâm.",
    "auth/operation-not-allowed": "Đăng nhập Google chưa được bật trên hệ thống.",
    "auth/configuration-not-found": "Đăng nhập Google chưa được cấu hình trên hệ thống.",
    "auth/network-request-failed": "Không kết nối được Google. Kiểm tra mạng và thử lại.",
    "auth/account-exists-with-different-credential": "Email đã dùng phương thức đăng nhập khác. Vui lòng đăng nhập bằng phương thức hiện tại.",
    "auth/invalid-api-key": "Đăng nhập Google chưa được cấu hình đúng. Vui lòng liên hệ trung tâm.",
    "auth/too-many-requests": "Bạn đã thử quá nhiều lần. Vui lòng đợi rồi thử lại.",
  };
  return messages[error?.code] || error?.response?.data?.message || error?.message
    || "Không thể đăng nhập Google. Vui lòng thử lại.";
}
