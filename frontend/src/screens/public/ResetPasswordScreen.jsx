import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiError } from "../../utils/api";
import "../../styles/PublicSite.css";
import "../../styles/PasswordToggle.css";

export default function ResetPasswordScreen() {
  const [params] = useSearchParams();
  const token = params.get("token") || "";
  const [form, setForm] = useState({ newPassword: "", confirmPassword: "" });
  const [validating, setValidating] = useState(Boolean(token));
  const [valid, setValid] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState(token ? "" : "Liên kết đặt lại mật khẩu không hợp lệ.");
  const [showPassword, setShowPassword] = useState(false);
  const darkMode = localStorage.getItem("publicTheme") === "dark";

  useEffect(() => {
    let active = true;
    if (!token) return undefined;
    api.get(endpoints["validate-reset-password"], { params: { token } })
      .then(() => { if (active) setValid(true); })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setValidating(false); });
    return () => { active = false; };
  }, [token]);

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true); setError("");
    try {
      const response = await api.post(endpoints["reset-password"], { token, ...form });
      setMessage(response.data.message); setValid(false);
    } catch (requestError) { setError(apiError(requestError)); }
    finally { setLoading(false); }
  };

  return (
    <main className={`public-site student-login-page ${darkMode ? "dark" : ""}`}>
      <section className="student-login-visual">
        <Link className="public-brand" to="/"><span className="public-brand-mark">LC</span><span><strong>Lingua Center</strong><small>Học ngoại ngữ mỗi ngày</small></span></Link>
        <div><span className="section-kicker">BẢO MẬT TÀI KHOẢN</span><h1>Tạo mật khẩu mới.</h1><p>Mật khẩu mới cần có ít nhất 8 ký tự, chữ thường, chữ số và ký tự đặc biệt.</p></div>
        <span className="student-login-japanese">安全</span>
      </section>
      <section className="student-login-panel">
        <form className="student-login-form" onSubmit={submit}>
          <Link className="back-home" to="/login">← Về đăng nhập</Link>
          <span className="section-kicker">ĐẶT LẠI MẬT KHẨU</span><h2>Mật khẩu mới</h2>
          {validating && <div className="public-alert">Đang kiểm tra liên kết...</div>}
          {message && <div className="public-alert public-alert-success">{message} <Link to="/login">Đăng nhập ngay</Link></div>}
          {error && <div className="public-alert">{error}</div>}
          {valid && <>
            <label>Mật khẩu mới<span className="password-input-wrap"><input type={showPassword ? "text" : "password"} required autoComplete="new-password" value={form.newPassword} onChange={(event) => setForm({ ...form, newPassword: event.target.value })} /><button className="password-toggle" type="button" aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"} onClick={() => setShowPassword((value) => !value)}>👁</button></span></label>
            <label>Xác nhận mật khẩu<input type={showPassword ? "text" : "password"} required autoComplete="new-password" value={form.confirmPassword} onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })} /></label>
            <button className="primary-cta student-login-submit" disabled={loading}>{loading ? "Đang cập nhật..." : "Đặt lại mật khẩu"}</button>
          </>}
        </form>
      </section>
    </main>
  );
}
