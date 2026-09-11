import { useEffect, useMemo, useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate } from "../../utils/api";
import { isTokenActive, SESSION_KEYS } from "../../utils/authSession";
import "../../styles/Attendance.css";

const STATUS_LABELS = {
  PRESENT: "Có mặt",
  ABSENT: "Vắng",
  LATE: "Đi trễ",
  EXCUSED: "Vắng có phép",
};

export default function StudentAttendanceScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [params] = useSearchParams();
  const requestedClassId = params.get("classId");
  const [classes, setClasses] = useState([]);
  const [lessonsByClass, setLessonsByClass] = useState({});
  const [attendance, setAttendance] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isTokenActive(token) || role !== "STUDENT") return undefined;
    let active = true;
    const api = authApis();
    Promise.all([
      api.get(endpoints["my-classes"]),
      api.get(endpoints["my-attendance"]),
      api.get(endpoints["my-lessons"]),
    ])
      .then(([classResponse, attendanceResponse, lessonResponse]) => {
        const classList = apiData(classResponse) || [];
        if (!active) return;
        setClasses(classList);
        setAttendance(apiData(attendanceResponse) || []);
        setLessonsByClass(groupByClassId(apiData(lessonResponse) || []));
        const requestedClass = classList.find((item) => String(item.id) === requestedClassId);
        setSelectedClassId(requestedClass?.id || classList[0]?.id || null);
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [requestedClassId, role, token]);

  const selectedLessons = lessonsByClass[selectedClassId] || [];
  const attendanceByLesson = useMemo(
    () => new Map(attendance.map((item) => [item.lessonId, item])),
    [attendance],
  );

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "STUDENT") return <Navigate to="/" replace />;

  return <section className="attendance-page">
    <div className="student-learning-hero"><div className="public-container"><span className="section-kicker">TIẾN ĐỘ HỌC TẬP</span><h1>Kết quả điểm danh</h1><p>Theo dõi tỷ lệ tham gia và kết quả từng buổi trong các lớp đang học.</p></div></div>
    <div className="public-container attendance-content">
      {error && <div className="public-alert">{error}</div>}
      {loading && <div className="learning-loading">Đang tải kết quả điểm danh...</div>}
      {!loading && classes.length === 0 && <div className="learning-empty"><span>✓</span><h2>Chưa có lớp đang học</h2><p>Lớp sẽ xuất hiện sau khi đăng ký và thanh toán thành công.</p></div>}
      {!loading && classes.length > 0 && <div className="attendance-layout">
        <aside className="attendance-class-list">
          <h2>Lớp đang học</h2>
          {classes.map((item) => {
            const lessons = lessonsByClass[item.id] || [];
            const completed = lessons.filter((lesson) => lesson.status === "COMPLETED");
            const attended = completed.filter((lesson) => ["PRESENT", "LATE"].includes(attendanceByLesson.get(lesson.id)?.status)).length;
            const rate = completed.length ? Math.round((attended * 100) / completed.length) : 0;
            return <button type="button" className={selectedClassId === item.id ? "active" : ""} onClick={() => setSelectedClassId(item.id)} key={item.id}>
              <span><strong>{item.className}</strong><small>{item.classCode}</small></span>
              <span className="attendance-rate"><b>{rate}%</b><small>{completed.length} buổi đã học</small></span>
            </button>;
          })}
        </aside>
        <section className="attendance-detail">
          <header><div><span className="section-kicker">CHI TIẾT BUỔI HỌC</span><h2>{classes.find((item) => item.id === selectedClassId)?.className}</h2></div><strong>{selectedLessons.filter((item) => item.status === "COMPLETED").length}/{selectedLessons.length} buổi hoàn thành</strong></header>
          {selectedLessons.length === 0 ? <div className="attendance-empty">Lớp chưa có danh sách buổi học.</div> : <div className="lesson-list">
            {selectedLessons.map((lesson) => {
              const record = attendanceByLesson.get(lesson.id);
              return <article key={lesson.id}>
                <div className="lesson-index">{String(selectedLessons.indexOf(lesson) + 1).padStart(2, "0")}</div>
                <div><strong>{lesson.topic || `Buổi học ngày ${formatDate(lesson.lessonDate)}`}</strong><small>{formatDate(lesson.lessonDate)} · {lesson.startTime}–{lesson.endTime} · {lesson.roomName || (lesson.deliveryMode === "ONLINE" ? "Trực tuyến" : "Phòng đang cập nhật")}</small>{record?.note && <p>{record.note}</p>}</div>
                <span className={`attendance-status status-${(record?.status || lesson.status).toLowerCase()}`}>{record ? STATUS_LABELS[record.status] : lesson.status === "COMPLETED" ? "Chưa có kết quả" : "Chưa điểm danh"}</span>
              </article>;
            })}
          </div>}
        </section>
      </div>}
    </div>
  </section>;
}

const groupByClassId = (lessons) => lessons.reduce((result, lesson) => {
  (result[lesson.courseClassId] ||= []).push(lesson);
  return result;
}, {});
