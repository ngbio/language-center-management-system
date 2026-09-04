import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import "../../styles/PublicSite.css";
import {
  SESSION_KEYS,
  clearSession,
  isTokenActive,
} from "../../utils/authSession";

export default function PublicLayout() {
  const [darkMode, setDarkMode] = useState(() => {
    const savedTheme = localStorage.getItem("publicTheme");
    if (savedTheme) return savedTheme === "dark";
    return window.matchMedia?.("(prefers-color-scheme: dark)").matches ?? false;
  });
  const [session, setSession] = useState(() => ({
    token: localStorage.getItem(SESSION_KEYS.token),
    role: localStorage.getItem(SESSION_KEYS.role),
    email: localStorage.getItem(SESSION_KEYS.email) || "",
  }));

  const toggleTheme = () => {
    setDarkMode((current) => {
      const next = !current;
      localStorage.setItem("publicTheme", next ? "dark" : "light");
      return next;
    });
  };

  const logout = () => {
    clearSession();
    setSession({ token: null, role: null, email: "" });
  };
  const publicAuthenticated =
    isTokenActive(session.token) && ["STUDENT", "TEACHER", "ADMIN"].includes(session.role);

  return (
    <div className={`public-site ${darkMode ? "dark" : ""}`}>
      <header className="public-header">
        <div className="public-container public-nav">
          <Link className="public-brand" to="/">
            <span className="public-brand-mark">LC</span>
            <span>
              <strong>Lingua Center</strong>
              <small>Học ngoại ngữ mỗi ngày</small>
            </span>
          </Link>
          <nav aria-label="Điều hướng chính">
            <NavLink to="/" end>Trang chủ</NavLink>
            <a href="/#courses">Khóa học</a>
            <a href="/#about">Về chúng tôi</a>
          </nav>
          <div className="public-nav-actions">
            <button
              className="theme-toggle"
              type="button"
              onClick={toggleTheme}
              aria-label={darkMode ? "Chuyển sang giao diện sáng" : "Chuyển sang giao diện tối"}
              title={darkMode ? "Giao diện sáng" : "Giao diện tối"}
            >
              <span aria-hidden="true">{darkMode ? "☀" : "☾"}</span>
            </button>
            {!publicAuthenticated && <Link className="public-login" to="/login">Đăng nhập</Link>}
            {publicAuthenticated && (
              <span className="student-session" title={session.email}>
                <small>
                  {session.role === "STUDENT"
                    ? "Học viên"
                    : session.role === "TEACHER"
                      ? "Giáo viên"
                      : "Quản trị viên"}
                </small>
                <strong>Chào, {session.email}</strong>
              </span>
            )}
            {publicAuthenticated && (
              <button className="session-logout" type="button" onClick={logout}>Đăng xuất</button>
            )}
          </div>
        </div>
      </header>

      <main><Outlet /></main>

      <footer className="public-footer" id="about">
        <div className="public-container footer-grid">
          <div>
            <Link className="public-brand footer-brand" to="/">
              <span className="public-brand-mark">LC</span>
              <span><strong>Lingua Center</strong><small>Learn. Grow. Connect.</small></span>
            </Link>
            <p>Nền tảng học ngoại ngữ với lộ trình rõ ràng, nội dung dễ tiếp cận và lớp học linh hoạt.</p>
          </div>
          <div><h3>Khám phá</h3><a href="/#courses">Khóa học</a><a href="/#benefits">Lợi ích</a><Link to="/admin/login">Cổng quản trị</Link></div>
          <div><h3>Liên hệ</h3><span>028 7300 1234</span><span>hello@linguacenter.vn</span><span>TP. Hồ Chí Minh, Việt Nam</span></div>
        </div>
        <div className="public-container footer-bottom"><span>© 2026 Lingua Center</span><span>Học hôm nay, mở lối ngày mai.</span></div>
      </footer>
    </div>
  );
}
