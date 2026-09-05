import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import Apis, { endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";
import { getActiveSessionHome, SESSION_KEYS } from "../../utils/authSession";

export default function LoginScreen() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const activeSessionHome = getActiveSessionHome();

  if (activeSessionHome) {
    return (
      <Navigate
        to={activeSessionHome}
        state={{ sessionNotice: "Bạn đang đăng nhập bằng tài khoản khác. Vui lòng đăng xuất khỏi tài khoản hiện tại trước khi đăng nhập Admin." }}
        replace
      />
    );
  }

  const submit = async (event) => {
    event.preventDefault();
    const currentSessionHome = getActiveSessionHome();
    if (currentSessionHome) {
      navigate(currentSessionHome, {
        replace: true,
        state: { sessionNotice: "Bạn đang đăng nhập bằng tài khoản khác. Vui lòng đăng xuất khỏi tài khoản hiện tại trước khi đăng nhập Admin." },
      });
      return;
    }
    setLoading(true);
    setError("");
    try {
      const data = apiData(await Apis.post(endpoints["admin-login"], form));
      localStorage.setItem(SESSION_KEYS.token, data.token);
      localStorage.setItem(SESSION_KEYS.role, data.roleCode);
      localStorage.setItem(SESSION_KEYS.email, data.email);
      navigate(location.state?.from?.pathname || "/admin", { replace: true });
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-story">
        <div className="brand light">
          <span className="brand-mark">LC</span>
          <div>
            <strong>Lingua Center</strong>
            <small>Learning without limits</small>
          </div>
        </div>
        <div className="story-copy">
          <span className="eyebrow light-text">ADMIN PORTAL</span>
          <h1>
            Vận hành trung tâm
            <br />
            trong một không gian.
          </h1>
          <p>
            Theo dõi người học, khóa học và lớp học với trải nghiệm quản trị rõ
            ràng, tập trung.
          </p>
        </div>
        <div className="story-metric">
          <strong>7</strong>
          <span>
            phân hệ quản lý
            <br />
            đã sẵn sàng
          </span>
        </div>
      </section>
      <section className="login-panel">
        <form className="login-form" onSubmit={submit}>
          <span className="login-kicker">CHÀO MỪNG TRỞ LẠI</span>
          <h2>Đăng nhập quản trị</h2>
          <p>Sử dụng tài khoản có quyền ADMIN để tiếp tục.</p>
          {error && <div className="alert error">{error}</div>}
          <label>
            Email
            <input
              type="email"
              required
              autoFocus
              value={form.email}
              placeholder="admin@linguacenter.vn"
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </label>
          <label>
            Mật khẩu
            <input
              type="password"
              required
              value={form.password}
              placeholder="Nhập mật khẩu"
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </label>
          <button className="primary-button login-button" disabled={loading}>
            {loading ? "Đang xác thực..." : "Đăng nhập"}
            <span>→</span>
          </button>
          <small className="login-note">
            Khu vực dành riêng cho quản trị viên hệ thống.
          </small>
        </form>
      </section>
    </main>
  );
}
