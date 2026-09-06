import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import Apis, { endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";
import { getActiveSessionHome, SESSION_KEYS } from "../../utils/authSession";

export default function StaffLoginScreen() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const activeSessionHome = getActiveSessionHome();

  if (activeSessionHome) return <Navigate to={activeSessionHome} replace />;

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      const data = apiData(await Apis.post(endpoints["staff-login"], form));
      localStorage.setItem(SESSION_KEYS.token, data.token);
      localStorage.setItem(SESSION_KEYS.role, data.roleCode);
      localStorage.setItem(SESSION_KEYS.email, data.email);
      navigate(location.state?.from?.pathname || "/staff/enrollments", { replace: true });
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-story">
        <div className="brand light"><span className="brand-mark">LC</span><div><strong>Lingua Center</strong><small>Staff workspace</small></div></div>
        <div className="story-copy"><span className="eyebrow light-text">STAFF PORTAL</span><h1>Đồng hành cùng<br />học viên.</h1><p>Quản lý đăng ký, xác nhận, hủy và chuyển lớp cho học viên.</p></div>
      </section>
      <section className="login-panel">
        <form className="login-form" onSubmit={submit}>
          <span className="login-kicker">CỔNG NHÂN VIÊN</span>
          <h2>Đăng nhập Staff</h2>
          <p>Sử dụng tài khoản có quyền CONSULTANT để tiếp tục.</p>
          {error && <div className="alert error">{error}</div>}
          <label>Email<input type="email" required autoFocus value={form.email} placeholder="consultant@languagecenter.local" onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
          <label>Mật khẩu<input type="password" required value={form.password} placeholder="Nhập mật khẩu" onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
          <button className="primary-button login-button" disabled={loading}>{loading ? "Đang xác thực..." : "Đăng nhập"}<span>→</span></button>
        </form>
      </section>
    </main>
  );
}
