import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

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
  const logout = () => {
    ["token", "role", "adminEmail"].forEach((key) =>
      localStorage.removeItem(key),
    );
    navigate("/admin/login", { replace: true });
  };

  return (
    <div className="admin-shell">
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
            <small>{localStorage.getItem("adminEmail")}</small>
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
