import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate } from "../../utils/api";
import { isTokenActive, SESSION_KEYS } from "../../utils/authSession";

const days = [[1, "Thứ hai"], [2, "Thứ ba"], [3, "Thứ tư"], [4, "Thứ năm"], [5, "Thứ sáu"], [6, "Thứ bảy"], [7, "Chủ nhật"]];

export default function TeacherClassesScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [params] = useSearchParams();
  const courseId = params.get("courseId");
  const [activeTab, setActiveTab] = useState("classes");
  const [classes, setClasses] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isTokenActive(token) || role !== "TEACHER") return undefined;
    let active = true;
    const api = authApis();
    api.get(endpoints["teacher-classes"])
      .then(async (response) => {
        const assigned = apiData(response) || [];
        const visible = courseId ? assigned.filter((item) => String(item.courseId) === courseId) : assigned;
        if (!active) return;
        setClasses(visible);
        const scheduleResponses = await Promise.all(visible.map((item) => api.get(endpoints["class-schedules"](item.id)).catch(() => null)));
        if (active) setSchedules(scheduleResponses.flatMap((item) => item ? apiData(item) || [] : []));
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [courseId, role, token]);

  const schedulesByDay = useMemo(() => days.map(([value, label]) => ({ value, label, items: schedules.filter((item) => Number(item.dayOfWeek) === value) })), [schedules]);

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "TEACHER") return <Navigate to="/" replace />;

  return <section className="student-learning-page teacher-workspace">
    <div className="student-learning-hero"><div className="public-container"><span className="section-kicker">KHÔNG GIAN GIẢNG DẠY</span><h1>Lớp học phụ trách</h1><p>Theo dõi các lớp được phân công và lịch giảng dạy cố định trong tuần.</p></div></div>
    <div className="public-container student-learning-content">
      {courseId && <div className="teacher-filter-note"><span>Đang lọc lớp theo khóa học đã chọn.</span><Link to="/giao-vien/lop-hoc">Hiện tất cả lớp</Link></div>}
      <div className="learning-tabs" role="tablist"><button className={activeTab === "classes" ? "active" : ""} onClick={() => setActiveTab("classes")}>Lớp học <span>{classes.length}</span></button><button className={activeTab === "schedule" ? "active" : ""} onClick={() => setActiveTab("schedule")}>Thời khóa biểu <span>{schedules.length}</span></button></div>
      {error && <div className="public-alert">{error}</div>}
      {loading && <div className="learning-loading">Đang tải thông tin giảng dạy...</div>}
      {!loading && activeTab === "classes" && (classes.length ? <div className="my-class-grid">{classes.map((item) => <article className="my-class-card" key={item.id}><div className="my-class-card-top"><span>{item.levelCode}</span><em>{item.status}</em></div><small>{item.courseName}</small><h2>{item.className}</h2><b>{item.classCode}</b><dl><div><dt>Khai giảng</dt><dd>{formatDate(item.startDate)}</dd></div><div><dt>Kết thúc</dt><dd>{formatDate(item.endDate)}</dd></div><div><dt>Sĩ số</dt><dd>{item.enrolledStudents}/{item.maxStudents}</dd></div><div><dt>Địa điểm dạy</dt><dd>{formatClassLocations(schedules, item.id)}</dd></div></dl><button type="button" onClick={() => setActiveTab("schedule")}>Xem thời khóa biểu →</button></article>)}</div> : <EmptyTeacher />)}
      {!loading && activeTab === "schedule" && (schedules.length ? <div className="weekly-schedule">{schedulesByDay.map((day) => <section className={`schedule-day ${day.items.length ? "has-lessons" : ""}`} key={day.value}><header><strong>{day.label}</strong><small>{day.items.length ? `${day.items.length} lịch dạy` : "Không có lịch"}</small></header><div>{day.items.map((item) => <article key={item.id}><time>{String(item.startTime).slice(0, 5)}<span>—</span>{String(item.endTime).slice(0, 5)}</time><div><strong>{item.className}</strong><small>{item.classCode} · {formatSchedulePlace(item)}</small>{item.deliveryMode !== "ONLINE" && <small>{item.roomLocation || "Địa chỉ trung tâm đang cập nhật"}</small>}</div>{item.deliveryMode === "ONLINE" && item.meetingUrl && <a href={item.meetingUrl} target="_blank" rel="noreferrer">Vào lớp</a>}</article>)}</div></section>)}</div> : <div className="learning-empty"><span>時</span><h2>Chưa có thời khóa biểu</h2><p>Các lớp phụ trách chưa được thiết lập lịch học.</p></div>)}
    </div>
  </section>;
}

function EmptyTeacher() {
  return <div className="learning-empty"><span>教</span><h2>Chưa có lớp được phân công</h2><p>Vui lòng liên hệ quản trị viên để kiểm tra phân công giảng dạy.</p><Link className="primary-cta" to="/giao-vien/khoa-hoc">Xem khóa học phụ trách</Link></div>;
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
