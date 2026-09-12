import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";
import { getActiveSessionHome, SESSION_KEYS } from "../../utils/authSession";
import "../../styles/PublicSite.css";
import "../../styles/PasswordToggle.css";

export default function StudentLoginScreen() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const darkMode = localStorage.getItem("publicTheme") === "dark";
  const activeSessionHome = getActiveSessionHome();

  if (activeSessionHome) {
    return <Navigate to={activeSessionHome} replace />;
  }

  const submit = async (event) => {
    event.preventDefault();
    const currentSessionHome = getActiveSessionHome();
    if (currentSessionHome) {
      navigate(currentSessionHome, { replace: true });
      return;
    }
    setLoading(true);
    setError("");
    try {
      const data = apiData(await api.post(endpoints.login, form));
      localStorage.setItem(SESSION_KEYS.token, data.token);
      localStorage.setItem(SESSION_KEYS.role, data.roleCode);
      localStorage.setItem(SESSION_KEYS.email, data.email);
      navigate(location.state?.from?.pathname || "/", { replace: true });
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className={`public-site student-login-page ${darkMode ? "dark" : ""}`}>
      <section className="student-login-visual">
        <Link className="public-brand" to="/">
          <span className="public-brand-mark">LC</span>
          <span><strong>Lingua Center</strong><small>Học ngoại ngữ mỗi ngày</small></span>
        </Link>
        <div><span className="section-kicker">CỔNG HỌC TẬP</span><h1>Tiếp tục hành trình của bạn.</h1><p>Không gian đăng nhập dành cho học viên và giáo viên của trung tâm.</p></div>
        <span className="student-login-japanese">学ぶ</span>
      </section>
      <section className="student-login-panel">
        <form className="student-login-form" onSubmit={submit}>
          <Link className="back-home" to="/">← Về trang chủ</Link>
          <span className="section-kicker">CHÀO MỪNG TRỞ LẠI</span>
          <h2>Đăng nhập</h2>
          <p>Sử dụng tài khoản học viên hoặc giáo viên để đăng nhập.</p>
          {location.state?.registrationSuccess && (
            <div className="public-alert public-alert-success">{location.state.registrationSuccess}</div>
          )}
          {error && <div className="public-alert">{error}</div>}
          <label>Email<input type="email" required autoFocus value={form.email} placeholder="email@linguacenter.vn" onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
          <label>
            Mật khẩu
            <span className="password-input-wrap">
              <input
                type={showPassword ? "text" : "password"}
                required
                autoComplete="current-password"
                value={form.password}
                placeholder="Nhập mật khẩu"
                onChange={(event) => setForm({ ...form, password: event.target.value })}
              />
              <button
                className="password-toggle"
                type="button"
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                aria-pressed={showPassword}
                onClick={() => setShowPassword((visible) => !visible)}
              >
                <PasswordEyeIcon visible={showPassword} />
              </button>
            </span>
          </label>
          <Link className="forgot-password-link" to="/forgot-password">Quên mật khẩu?</Link>
          <button className="primary-cta student-login-submit" disabled={loading}>{loading ? "Đang xác thực..." : "Đăng nhập"}</button>
          <small>Chưa có tài khoản? <Link to="/register">Đăng ký tại đây</Link>.</small>
        </form>
      </section>
    </main>
  );
}

function PasswordEyeIcon({ visible }) {
  return visible ? (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 3l18 18M10.6 10.7a2 2 0 002.7 2.7M9.9 4.2A10.7 10.7 0 0112 4c5.5 0 9 5.5 9 5.5a17 17 0 01-2.2 2.7M6.2 6.2C4.1 7.6 3 9.5 3 9.5S6.5 15 12 15c1 0 2-.2 2.8-.5" />
    </svg>
  ) : (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 12s3.5-5.5 9-5.5 9 5.5 9 5.5-3.5 5.5-9 5.5S3 12 3 12z" />
      <circle cx="12" cy="12" r="2.5" />
    </svg>
  );
}
