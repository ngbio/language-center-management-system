import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { SESSION_KEYS, clearSession } from "../utils/authSession";
import { disconnectFirebaseChat } from "../services/firebaseChat";

export default function StaffLayout() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const logout = async () => {
    await disconnectFirebaseChat();
    clearSession();
    navigate("/staff/login", { replace: true });
  };

  return (
    <div className="admin-shell">
      <aside className={`sidebar ${open ? "is-open" : ""}`}>
        <div className="brand">
          <span className="brand-mark">LC</span>
          <div><strong>Lingua Center</strong><small>Staff workspace</small></div>
        </div>
        <nav>
          <NavLink to="/staff/profile" onClick={() => setOpen(false)}>
            <span className="nav-icon">S</span>Hồ sơ cá nhân
          </NavLink>
          <NavLink to="/staff/enrollments" onClick={() => setOpen(false)}>
            <span className="nav-icon">✓</span>Đăng ký học
          </NavLink>
          <NavLink to="/staff/refunds" onClick={() => setOpen(false)}>
            <span className="nav-icon">↩</span>Hoàn tiền
          </NavLink>
          <NavLink to="/staff/chat" onClick={() => setOpen(false)}>
            <span className="nav-icon">C</span>Tin nhắn học viên
          </NavLink>
          <NavLink to="/staff/change-password" onClick={() => setOpen(false)}>
            <span className="nav-icon">🔒</span>Đổi mật khẩu
          </NavLink>
        </nav>
        <div className="sidebar-foot">
          <span className="avatar">S</span>
          <div><strong>Nhân viên tư vấn</strong><small>{localStorage.getItem(SESSION_KEYS.email)}</small></div>
        </div>
      </aside>
      {open && (
        <button
          className="sidebar-backdrop"
          type="button"
          aria-label="Đóng menu"
          onClick={() => setOpen(false)}
        />
      )}
      <main className="admin-main">
        <header className="topbar">
          <button
            className="menu-button"
            type="button"
            aria-label="Mở menu"
            aria-expanded={open}
            onClick={() => setOpen(true)}
          >
            ☰
          </button>
          <span>Quản lý đăng ký học viên</span>
          <div className="topbar-actions">
            <span className="online-dot">Đang hoạt động</span>
            <button className="logout-button" onClick={logout} type="button">Đăng xuất</button>
          </div>
        </header>
        <div className="page-container"><Outlet /></div>
      </main>
    </div>
  );
}
