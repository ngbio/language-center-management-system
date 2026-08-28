import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { ErrorAlert, PageTitle } from "../../components/AdminUi";
import { apiData, apiError } from "../../utils/api";

const cards = [
  ["users", "Người dùng", "◎", "/admin/users", "Tài khoản toàn hệ thống"],
  ["courses", "Khóa học", "▤", "/admin/courses", "Chương trình đang quản lý"],
  [
    "classes",
    "Lớp đang mở",
    "◫",
    "/admin/classes",
    "Có thể tiếp nhận học viên",
  ],
  ["rooms", "Phòng học", "⌂", "/admin/rooms", "Cơ sở vật chất"],
];

export default function DashboardScreen() {
  const [stats, setStats] = useState({});
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const api = authApis();
        const [users, courses, classes, rooms, languages, levels] =
          await Promise.all([
            api.get(endpoints["admin-users"], { params: { page: 0, size: 1 } }),
            api.get(endpoints.courses, { params: { page: 0, size: 1 } }),
            api.get(endpoints.classes, { params: { page: 0, size: 1 } }),
            api.get(endpoints.rooms),
            api.get(endpoints.languages),
            api.get(endpoints.levels),
          ]);
        setStats({
          users: apiData(users).totalElements,
          courses: apiData(courses).totalElements,
          classes: apiData(classes).totalElements,
          rooms: apiData(rooms).length,
          languages: apiData(languages).length,
          levels: apiData(levels).length,
        });
      } catch (requestError) {
        setError(apiError(requestError));
      }
    };
    load();
  }, []);

  return (
    <>
      <PageTitle
        eyebrow="TỔNG QUAN"
        title="Chào buổi làm việc mới"
        description="Nắm nhanh tình hình vận hành của trung tâm hôm nay."
      />
      <ErrorAlert message={error} />
      <section className="stat-grid">
        {cards.map(([key, label, icon, to, note]) => (
          <Link className="stat-card" to={to} key={key}>
            <div className="stat-icon">{icon}</div>
            <div>
              <span>{label}</span>
              <strong>{stats[key] ?? "—"}</strong>
              <small>{note}</small>
            </div>
            <b>↗</b>
          </Link>
        ))}
      </section>
      <section className="dashboard-grid">
        <article className="panel quick-panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">TRUY CẬP NHANH</span>
              <h2>Danh mục đào tạo</h2>
            </div>
          </div>
          <div className="quick-list">
            <Link to="/admin/languages">
              <span>文</span>
              <div>
                <strong>{stats.languages ?? "—"} ngôn ngữ</strong>
                <small>Danh mục ngôn ngữ giảng dạy</small>
              </div>
              <b>→</b>
            </Link>
            <Link to="/admin/levels">
              <span>⌁</span>
              <div>
                <strong>{stats.levels ?? "—"} trình độ</strong>
                <small>Lộ trình theo từng ngôn ngữ</small>
              </div>
              <b>→</b>
            </Link>
            <Link to="/admin/courses">
              <span>▤</span>
              <div>
                <strong>Quản lý khóa học</strong>
                <small>Học phí, thời lượng và trạng thái</small>
              </div>
              <b>→</b>
            </Link>
          </div>
        </article>
        <article className="panel system-panel">
          <span className="eyebrow">TRẠNG THÁI HỆ THỐNG</span>
          <h2>Backend đang kết nối</h2>
          <div className="system-line">
            <i />
            <span>Spring Boot API</span>
            <strong>ONLINE</strong>
          </div>
          <div className="system-line">
            <i />
            <span>JWT Authentication</span>
            <strong>ACTIVE</strong>
          </div>
          <p>Số liệu trên trang được lấy trực tiếp từ các API hiện tại.</p>
        </article>
      </section>
    </>
  );
}
