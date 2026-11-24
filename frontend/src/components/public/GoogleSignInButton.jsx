import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Capacitor } from "@capacitor/core";
import api, { endpoints } from "../../configs/Apis";
import { apiData } from "../../utils/api";
import { getActiveSessionHome, SESSION_KEYS } from "../../utils/authSession";
import { getGoogleIdToken, googleAuthError } from "../../services/googleAuth";
import "../../styles/GoogleSignIn.css";

export default function GoogleSignInButton({ disabled = false, onBusyChange }) {
  const [pending, setPending] = useState(false);
  const [idToken, setIdToken] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const location = useLocation();

  // Native Google sign-in needs its own SDK flow; web popups do not work in a WebView.
  if (Capacitor.isNativePlatform()) return null;

  const login = async (linkAccount = false) => {
    if (pending || disabled) return;
    const currentHome = getActiveSessionHome();
    if (currentHome) {
      navigate(currentHome, { replace: true });
      return;
    }
    if (linkAccount && !password) {
      setError("Nhập mật khẩu hiện tại để xác nhận liên kết.");
      return;
    }
    setPending(true);
    onBusyChange?.(true);
    setError("");
    try {
      const token = linkAccount ? idToken : await getGoogleIdToken();
      setIdToken(token);
      const data = apiData(await api.post(endpoints["google-login"], {
        idToken: token,
        ...(linkAccount ? { password } : {}),
      }));
      localStorage.setItem(SESSION_KEYS.token, data.token);
      localStorage.setItem(SESSION_KEYS.role, data.roleCode);
      localStorage.setItem(SESSION_KEYS.email, data.email);
      setIdToken("");
      setPassword("");
      navigate(location.state?.from?.pathname || "/", { replace: true });
    } catch (requestError) {
      if (requestError?.response?.status === 409
          && requestError.response.data?.data?.requiresPassword) {
        setError("");
      } else if (!["auth/popup-closed-by-user", "auth/cancelled-popup-request"].includes(requestError?.code)) {
        setError(googleAuthError(requestError));
        if (!linkAccount) setIdToken("");
      } else {
        setIdToken("");
      }
    } finally {
      setPending(false);
      onBusyChange?.(false);
    }
  };

  return (
    <div className="google-sign-in">
      <div className="google-auth-divider"><span>hoặc</span></div>
      {idToken && !pending ? (
        <div className="google-account-link">
          <p>Email Google này đã có tài khoản. Xác nhận mật khẩu của tài khoản tại trung tâm để liên kết.</p>
          <label>Mật khẩu hiện tại
            <input type="password" autoComplete="current-password" maxLength={100}
              value={password} onChange={(event) => setPassword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") { event.preventDefault(); login(true); }
              }} />
          </label>
          <button className="google-sign-in-button" type="button" disabled={disabled || pending}
            onClick={() => login(true)}>Xác nhận liên kết Google</button>
          <button className="google-link-cancel" type="button" disabled={disabled || pending}
            onClick={() => { setIdToken(""); setPassword(""); setError(""); }}>Hủy liên kết</button>
        </div>
      ) : (
        <button className="google-sign-in-button" type="button" disabled={disabled || pending}
          onClick={() => login()}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.39-.18-2.05H12v3.88h5.38a4.6 4.6 0 0 1-2 3.02v2.51h3.24c1.9-1.75 2.98-4.33 2.98-7.36Z" />
            <path fill="#34A853" d="M12 22c2.7 0 4.96-.9 6.62-2.41l-3.24-2.51c-.9.6-2.05.96-3.38.96-2.6 0-4.81-1.76-5.6-4.12H3.06v2.59A10 10 0 0 0 12 22Z" />
            <path fill="#FBBC05" d="M6.4 13.92a6 6 0 0 1 0-3.84V7.49H3.06a10 10 0 0 0 0 9.02l3.34-2.59Z" />
            <path fill="#EA4335" d="M12 5.96c1.47 0 2.79.5 3.82 1.49l2.87-2.87A9.6 9.6 0 0 0 12 2a10 10 0 0 0-8.94 5.49l3.34 2.59A5.99 5.99 0 0 1 12 5.96Z" />
          </svg>
          {pending ? "Đang xác thực..." : "Tiếp tục với Google"}
        </button>
      )}
      {error && <div className="public-alert" role="alert">{error}</div>}
    </div>
  );
}
