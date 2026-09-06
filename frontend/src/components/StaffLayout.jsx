import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { SESSION_KEYS, clearSession } from "../utils/authSession";

export default function StaffLayout() {
  const navigate = useNavigate();
  const logout = () => {
    clearSession();
    navigate("/staff/login", { replace: true });
  };

  return (
    <div className="admin-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">LC</span>
          <div><strong>Lingua Center</strong><small>Staff workspace</small></div>
        </div>
        <nav>
          <NavLink to="/staff/enrollments">
            <span className="nav-icon">✓</span>Đăng ký học
          </NavLink>
        </nav>
        <div className="sidebar-foot">
          <span className="avatar">S</span>
          <div><strong>Nhân viên tư vấn</strong><small>{localStorage.getItem(SESSION_KEYS.email)}</small></div>
        </div>
      </aside>
      <main className="admin-main">
        <header className="topbar">
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
