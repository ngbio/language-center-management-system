import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";
import { SESSION_KEYS, isTokenActive } from "../../utils/authSession";
import "../../styles/PublicSite.css";

export default function StudentLoginScreen() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const darkMode = localStorage.getItem("publicTheme") === "dark";
  const existingToken = localStorage.getItem(SESSION_KEYS.token);
  const existingRole = localStorage.getItem(SESSION_KEYS.role);

  if (isTokenActive(existingToken) && ["STUDENT", "TEACHER", "ADMIN"].includes(existingRole)) {
    return <Navigate to="/" replace />;
  }

  const submit = async (event) => {
    event.preventDefault();
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
          {error && <div className="public-alert">{error}</div>}
          <label>Email<input type="email" required autoFocus value={form.email} placeholder="email@linguacenter.vn" onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
          <label>Mật khẩu<input type="password" required value={form.password} placeholder="Nhập mật khẩu" onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
          <button className="primary-cta student-login-submit" disabled={loading}>{loading ? "Đang xác thực..." : "Đăng nhập"}</button>
          <small>Chưa có tài khoản? Liên hệ trung tâm để được hỗ trợ đăng ký.</small>
        </form>
      </section>
    </main>
  );
}
