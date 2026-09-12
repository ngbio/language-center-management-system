import { useState } from "react";
import { Link } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiError } from "../../utils/api";
import "../../styles/PublicSite.css";

export default function ForgotPasswordScreen() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const darkMode = localStorage.getItem("publicTheme") === "dark";

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await api.post(endpoints["forgot-password"], { email });
      setMessage(response.data.message);
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className={`public-site student-login-page ${darkMode ? "dark" : ""}`}>
      <section className="student-login-visual">
        <Link className="public-brand" to="/"><span className="public-brand-mark">LC</span><span><strong>Lingua Center</strong><small>Học ngoại ngữ mỗi ngày</small></span></Link>
        <div><span className="section-kicker">KHÔI PHỤC TÀI KHOẢN</span><h1>Quay lại hành trình học tập.</h1><p>Nhập email đã đăng ký, chúng tôi sẽ gửi cho bạn liên kết đặt lại mật khẩu an toàn.</p></div>
        <span className="student-login-japanese">再開</span>
      </section>
      <section className="student-login-panel">
        <form className="student-login-form" onSubmit={submit}>
          <Link className="back-home" to="/login">← Về đăng nhập</Link>
          <span className="section-kicker">QUÊN MẬT KHẨU</span>
          <h2>Nhận liên kết</h2>
          <p>Liên kết chỉ dùng một lần và hết hạn sau thời gian được ghi trong email.</p>
          {message && <div className="public-alert public-alert-success">{message}</div>}
          {error && <div className="public-alert">{error}</div>}
          <label>Email<input type="email" required autoFocus autoComplete="email" value={email} placeholder="email@linguacenter.vn" onChange={(event) => setEmail(event.target.value)} /></label>
          <button className="primary-cta student-login-submit" disabled={loading}>{loading ? "Đang gửi..." : "Gửi liên kết đặt lại"}</button>
        </form>
      </section>
    </main>
  );
}
