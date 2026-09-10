import { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { authApis, endpoints } from "../configs/Apis";
import { SESSION_KEYS, clearSession, isTokenActive } from "../utils/authSession";
import "../styles/ChangePasswordScreen.css";

const initialForm = { currentPassword: "", newPassword: "", confirmPassword: "" };
const passwordPattern = "(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).{8,100}";

export default function ChangePasswordScreen() {
  const navigate = useNavigate();
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [form, setForm] = useState(initialForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  if (!isTokenActive(token)) return <Navigate to={loginPath(role)} replace />;

  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    if (form.newPassword !== form.confirmPassword) {
      setError("Xác nhận mật khẩu mới không khớp.");
      return;
    }
    setSaving(true);
    try {
      await authApis().put(endpoints["change-password"], form);
      clearSession();
      window.alert("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
      navigate(loginPath(role), { replace: true });
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || "Không thể đổi mật khẩu.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="change-password-page">
      <div className="change-password-card">
        <span className="section-kicker">BẢO MẬT TÀI KHOẢN</span>
        <h1>Đổi mật khẩu</h1>
        <p>Sau khi đổi thành công, bạn cần đăng nhập lại bằng mật khẩu mới.</p>
        {error && <div className="change-password-error">{error}</div>}
        <form onSubmit={submit}>
          <label>Mật khẩu hiện tại<input name="currentPassword" type="password" required autoComplete="current-password" value={form.currentPassword} onChange={update} /></label>
          <label>Mật khẩu mới<input name="newPassword" type="password" required minLength="8" maxLength="100" pattern={passwordPattern} title="Từ 8 ký tự, có chữ thường, chữ số và ký tự đặc biệt" autoComplete="new-password" value={form.newPassword} onChange={update} /></label>
          <label>Xác nhận mật khẩu mới<input name="confirmPassword" type="password" required minLength="8" maxLength="100" autoComplete="new-password" value={form.confirmPassword} onChange={update} /></label>
          <button type="submit" disabled={saving}>{saving ? "Đang cập nhật..." : "Cập nhật mật khẩu"}</button>
        </form>
      </div>
    </section>
  );
}

function loginPath(role) {
  if (role === "ADMIN") return "/admin/login";
  if (role === "CONSULTANT") return "/staff/login";
  return "/login";
}
