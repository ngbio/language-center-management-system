import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import "../../styles/PublicSite.css";
import {
  SESSION_KEYS,
  clearSession,
  isTokenActive,
} from "../../utils/authSession";

export default function PublicLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const accountMenuRef = useRef(null);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
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
    setAccountMenuOpen(false);
    navigate("/");
  };

  useEffect(() => {
    setSession({
      token: localStorage.getItem(SESSION_KEYS.token),
      role: localStorage.getItem(SESSION_KEYS.role),
      email: localStorage.getItem(SESSION_KEYS.email) || "",
    });
  }, [location.pathname]);

  const sessionAuthenticated =
    isTokenActive(session.token) && ["STUDENT", "TEACHER", "ADMIN", "CONSULTANT"].includes(session.role);
  const publicAuthenticated =
    sessionAuthenticated && ["STUDENT", "TEACHER", "ADMIN"].includes(session.role);

  useEffect(() => {
    if (!accountMenuOpen) return undefined;
    const closeMenu = (event) => {
      if (!accountMenuRef.current?.contains(event.target)) setAccountMenuOpen(false);
    };
    const closeWithEscape = (event) => {
      if (event.key === "Escape") setAccountMenuOpen(false);
    };
    document.addEventListener("mousedown", closeMenu);
    document.addEventListener("keydown", closeWithEscape);
    return () => {
      document.removeEventListener("mousedown", closeMenu);
      document.removeEventListener("keydown", closeWithEscape);
    };
  }, [accountMenuOpen]);

  const accountLabel = session.email.split("@")[0] || "Tài khoản";

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
            <NavLink to="/khoa-hoc">Khóa học</NavLink>
            <NavLink to="/ngon-ngu">Ngôn ngữ</NavLink>
            <NavLink to="/lop-hoc">Lớp đang mở</NavLink>
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
            {!sessionAuthenticated && <Link className="public-register" to="/register">Đăng ký tài khoản</Link>}
            {!sessionAuthenticated && <Link className="public-login" to="/login">Đăng nhập</Link>}
            {publicAuthenticated && (
              <div className="public-account" ref={accountMenuRef}>
                <button className="account-trigger" type="button" aria-haspopup="menu" aria-expanded={accountMenuOpen} onClick={() => setAccountMenuOpen((open) => !open)}>
                  <span className="account-avatar" aria-hidden="true">{accountLabel.charAt(0).toUpperCase()}</span>
                  <span className="account-summary">
                    <strong>{accountLabel}</strong>
                    <small>{session.role === "STUDENT" ? "Học viên" : session.role === "TEACHER" ? "Giáo viên" : "Quản trị viên"}</small>
                  </span>
                  <span className={`account-chevron ${accountMenuOpen ? "open" : ""}`} aria-hidden="true">⌄</span>
                </button>
                {accountMenuOpen && (
                  <div className="account-menu" role="menu">
                    <div className="account-menu-heading"><strong>Xin chào, {accountLabel}</strong><span>{session.email}</span></div>
                    {session.role === "STUDENT" && <Link role="menuitem" to="/khoa-hoc-cua-toi" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">▣</span> Khóa học của tôi</Link>}
                    {session.role === "STUDENT" && <Link role="menuitem" to="/lop-hoc-cua-toi" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">▤</span> Lớp học & thời khóa biểu</Link>}
                    {session.role === "STUDENT" && <Link role="menuitem" to="/lich-su-dang-ky" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">↻</span> Lịch sử đăng ký & thanh toán</Link>}
                    {session.role === "TEACHER" && <Link role="menuitem" to="/giao-vien/khoa-hoc" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">▣</span> Khóa học phụ trách</Link>}
                    {session.role === "TEACHER" && <Link role="menuitem" to="/giao-vien/lop-hoc" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">▤</span> Lớp học & thời khóa biểu</Link>}
                    {session.role === "TEACHER" && <Link role="menuitem" to="/giao-vien/thong-tin-ca-nhan" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">◎</span> Thông tin cá nhân</Link>}
                    {session.role === "ADMIN" && <Link role="menuitem" to="/admin" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">▦</span> Trang quản trị</Link>}
                    {session.role === "STUDENT" && <Link role="menuitem" to="/thong-tin-ca-nhan" onClick={() => setAccountMenuOpen(false)}><span aria-hidden="true">◎</span> Thông tin cá nhân</Link>}
                    <a role="menuitem" href="mailto:hello@linguacenter.vn"><span aria-hidden="true">?</span> Hỗ trợ học tập</a>
                    <button className="account-logout" role="menuitem" type="button" onClick={logout}><span aria-hidden="true">↪</span> Đăng xuất</button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </header>

      <main><Outlet /></main>

      <footer className="public-footer">
        <div className="public-container footer-grid">
          <div>
            <Link className="public-brand footer-brand" to="/">
              <span className="public-brand-mark">LC</span>
              <span><strong>Lingua Center</strong><small>Learn. Grow. Connect.</small></span>
            </Link>
            <p>Nền tảng học ngoại ngữ với lộ trình rõ ràng, nội dung dễ tiếp cận và lớp học linh hoạt.</p>
          </div>
          <div><h3>Khám phá</h3><Link to="/khoa-hoc">Khóa học</Link><Link to="/ngon-ngu">Ngôn ngữ</Link><Link to="/lop-hoc">Lớp đang mở</Link>{!sessionAuthenticated && <><Link to="/staff/login">Cổng nhân viên</Link><Link to="/admin/login">Cổng quản trị</Link></>}</div>
          <div><h3>Liên hệ</h3><span>028 7300 1234</span><span>hello@linguacenter.vn</span><span>TP. Hồ Chí Minh, Việt Nam</span></div>
        </div>
        <div className="public-container footer-bottom"><span>© 2026 Lingua Center</span><span>Học hôm nay, mở lối ngày mai.</span></div>
      </footer>
    </div>
  );
}
