import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { SESSION_KEYS, clearSession } from "../utils/authSession";

const navigation = [
  ["/admin", "▦", "Tổng quan"],
  ["/admin/users", "◎", "Người dùng"],
  ["/admin/languages", "文", "Ngôn ngữ"],
  ["/admin/levels", "⌁", "Trình độ"],
  ["/admin/courses", "▤", "Khóa học"],
  ["/admin/rooms", "⌂", "Phòng học"],
  ["/admin/classes", "◫", "Lớp học"],
];

export default function AdminLayout() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const [sessionNotice, setSessionNotice] = useState(location.state?.sessionNotice || "");

  useEffect(() => {
    if (!location.state?.sessionNotice) return;
    navigate(`${location.pathname}${location.search}${location.hash}`, {
      replace: true,
      state: null,
    });
  }, [location.hash, location.pathname, location.search, location.state, navigate]);
  const logout = () => {
    clearSession();
    navigate("/admin/login", { replace: true });
  };

  return (
    <div className="admin-shell">
      {sessionNotice && (
        <div className="session-notice admin-session-notice" role="alert">
          <span aria-hidden="true">!</span>
          <p><strong>Không thể đăng nhập tài khoản khác</strong>{sessionNotice}</p>
          <button type="button" onClick={() => setSessionNotice("")} aria-label="Đóng thông báo">×</button>
        </div>
      )}
      <aside className={`sidebar ${open ? "is-open" : ""}`}>
        <div className="brand">
          <span className="brand-mark">LC</span>
          <div>
            <strong>Lingua Center</strong>
            <small>Admin workspace</small>
          </div>
        </div>
        <nav>
          {navigation.map(([to, icon, label]) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/admin"}
              onClick={() => setOpen(false)}
            >
              <span className="nav-icon">{icon}</span>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <span className="avatar">A</span>
          <div>
            <strong>Administrator</strong>
            <small>{localStorage.getItem(SESSION_KEYS.email)}</small>
          </div>
        </div>
      </aside>
      {open && (
        <button className="sidebar-backdrop" onClick={() => setOpen(false)} />
      )}
      <main className="admin-main">
        <header className="topbar">
          <button className="menu-button" onClick={() => setOpen(true)}>
            ☰
          </button>
          <span>Hệ thống quản lý trung tâm ngoại ngữ</span>
          <div className="topbar-actions">
            <span className="online-dot">Đang hoạt động</span>
            <button className="logout-button" onClick={logout} type="button">
              Đăng xuất
            </button>
          </div>
        </header>
        <div className="page-container">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
