import { useEffect, useMemo, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate } from "../../utils/api";
import { SESSION_KEYS, isTokenActive } from "../../utils/authSession";

const days = [
  [1, "Thứ hai"], [2, "Thứ ba"], [3, "Thứ tư"], [4, "Thứ năm"],
  [5, "Thứ sáu"], [6, "Thứ bảy"], [7, "Chủ nhật"],
];

export default function MyClassesScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [activeTab, setActiveTab] = useState("classes");
  const [classes, setClasses] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (role !== "STUDENT" || !isTokenActive(token)) return undefined;
    let active = true;
    const api = authApis();
    Promise.all([
      api.get(endpoints["my-classes"]),
      api.get(endpoints["my-schedules"]),
    ])
      .then(([classResponse, scheduleResponse]) => {
        if (!active) return;
        setClasses(apiData(classResponse) || []);
        setSchedules(apiData(scheduleResponse) || []);
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [role, token]);

  const schedulesByDay = useMemo(
    () => days.map(([value, label]) => ({
      value,
      label,
      items: schedules.filter((schedule) => Number(schedule.dayOfWeek) === value),
    })),
    [schedules],
  );

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "STUDENT") return <Navigate to="/" replace />;

  return (
    <section className="student-learning-page">
      <div className="student-learning-hero"><div className="public-container">
        <span className="section-kicker">TRUNG TÂM HỌC TẬP</span>
        <h1>Lớp học của tôi</h1>
        <p>Theo dõi các lớp đã kích hoạt và lịch học hàng tuần của bạn.</p>
      </div></div>

      <div className="public-container student-learning-content">
        <div className="learning-tabs" role="tablist" aria-label="Thông tin học tập">
          <button className={activeTab === "classes" ? "active" : ""} onClick={() => setActiveTab("classes")} role="tab">Lớp học <span>{classes.length}</span></button>
          <button className={activeTab === "schedule" ? "active" : ""} onClick={() => setActiveTab("schedule")} role="tab">Thời khóa biểu <span>{schedules.length}</span></button>
        </div>

        {error && <div className="public-alert">{error}</div>}
        {loading && <div className="learning-loading">Đang tải thông tin học tập...</div>}

        {!loading && activeTab === "classes" && (
          classes.length ? <div className="my-class-grid">{classes.map((item) => (
            <article className="my-class-card" key={item.id}>
              <div className="my-class-card-top"><span>{item.levelCode}</span><em>{item.status}</em></div>
              <small>{item.courseName}</small><h2>{item.className}</h2><b>{item.classCode}</b>
              <dl><div><dt>Khai giảng</dt><dd>{formatDate(item.startDate)}</dd></div><div><dt>Kết thúc</dt><dd>{formatDate(item.endDate)}</dd></div><div><dt>Giảng viên</dt><dd>{item.teacherName || "Đang cập nhật"}</dd></div><div><dt>Địa điểm học</dt><dd>{formatClassLocations(schedules, item.id)}</dd></div></dl>
              <button type="button" onClick={() => setActiveTab("schedule")}>Xem thời khóa biểu →</button>
            </article>
          ))}</div> : <EmptyLearning title="Chưa có lớp học đã kích hoạt" text="Lớp sẽ xuất hiện sau khi đăng ký được xác nhận và thanh toán thành công." />
        )}

        {!loading && activeTab === "schedule" && (
          schedules.length ? <div className="weekly-schedule">{schedulesByDay.map((day) => (
            <section className={`schedule-day ${day.items.length ? "has-lessons" : ""}`} key={day.value}>
              <header><strong>{day.label}</strong><small>{day.items.length ? `${day.items.length} lịch học` : "Không có lịch"}</small></header>
              <div>{day.items.map((item) => (
                <article key={item.id}>
                  <time>{String(item.startTime).slice(0, 5)}<span>—</span>{String(item.endTime).slice(0, 5)}</time>
                  <div><strong>{item.className}</strong><small>{item.classCode} · {formatSchedulePlace(item)}</small>{item.deliveryMode !== "ONLINE" && <small>{item.roomLocation || "Địa chỉ trung tâm đang cập nhật"}</small>}</div>
                  {item.deliveryMode === "ONLINE" && item.meetingUrl && <a href={item.meetingUrl} target="_blank" rel="noreferrer">Vào lớp</a>}
                </article>
              ))}</div>
            </section>
          ))}</div> : <EmptyLearning title="Chưa có thời khóa biểu" text="Lớp của bạn chưa được thiết lập lịch học." />
        )}

      </div>
    </section>
  );
}

function EmptyLearning({ title, text }) {
  return <div className="learning-empty"><span aria-hidden="true">学</span><h2>{title}</h2><p>{text}</p><Link className="primary-cta" to="/#courses">Khám phá lớp học</Link></div>;
}

const formatRoom = (item) => item.roomName
  ? `${item.roomName}${item.roomCode ? ` (${item.roomCode})` : ""}`
  : item.roomCode || "Phòng đang cập nhật";

const formatSchedulePlace = (item) => item.deliveryMode === "ONLINE" ? "Trực tuyến" : formatRoom(item);

const formatClassLocations = (schedules, classId) => {
  const places = schedules
    .filter((item) => item.courseClassId === classId)
    .map((item) => item.deliveryMode === "ONLINE" ? "Trực tuyến" : `${formatRoom(item)}${item.roomLocation ? ` · ${item.roomLocation}` : ""}`);
  return [...new Set(places)].join("; ") || "Đang cập nhật";
};
