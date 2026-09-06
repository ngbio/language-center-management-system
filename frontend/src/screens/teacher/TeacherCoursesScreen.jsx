import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatMoney } from "../../utils/api";
import { isTokenActive, SESSION_KEYS } from "../../utils/authSession";
import "../../styles/StudentCourses.css";

export default function TeacherCoursesScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isTokenActive(token) || role !== "TEACHER") return undefined;
    let active = true;
    authApis().get(endpoints["teacher-courses"])
      .then((response) => { if (active) setCourses(apiData(response) || []); })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [role, token]);

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "TEACHER") return <Navigate to="/" replace />;

  return <section className="my-courses-page teacher-workspace">
    <div className="my-courses-hero"><div className="public-container"><span className="section-kicker">KHÔNG GIAN GIẢNG DẠY</span><h1>Khóa học phụ trách</h1><p>Các khóa học được tổng hợp từ những lớp mà trung tâm đã phân công cho bạn.</p></div></div>
    <div className="public-container my-courses-content">
      <div className="my-courses-heading"><div><strong>{loading ? "—" : courses.length}</strong><span>Khóa học đang phụ trách</span></div><Link to="/giao-vien/lop-hoc">Xem lớp được phân công →</Link></div>
      {error && <div className="public-alert">{error}</div>}
      {loading && <div className="course-grid"><div className="course-card course-skeleton" /><div className="course-card course-skeleton" /></div>}
      {!loading && courses.length > 0 && <div className="course-grid">{courses.map((course, index) => <article className="course-card owned-course-card" key={course.id}>
        <div className={`course-cover cover-${index % 4}`}>{course.thumbnailUrl ? <img src={course.thumbnailUrl} alt={`Ảnh khóa học ${course.courseName}`} /> : <><span>{course.languageCode || "LC"}</span><b>{course.levelCode}</b></>}<em className="owned-badge">Phụ trách</em></div>
        <div className="course-card-body"><span className="course-meta">{course.languageName} · {course.levelName}</span><h3>{course.courseName}</h3><p>{course.shortDescription || course.description || "Chương trình được trung tâm phân công giảng dạy."}</p><div className="course-facts"><span>◷ {course.totalSessions} buổi</span><span>▤ {course.durationHours || "—"} giờ</span></div><div className="course-card-foot"><strong>{formatMoney(course.tuitionFee)}</strong><Link to={`/giao-vien/lop-hoc?courseId=${course.id}`}>Xem các lớp →</Link></div></div>
      </article>)}</div>}
      {!loading && !error && courses.length === 0 && <TeacherEmpty title="Chưa có khóa học phụ trách" text="Khóa học sẽ xuất hiện khi Admin phân công bạn vào một lớp." />}
    </div>
  </section>;
}

function TeacherEmpty({ title, text }) {
  return <div className="learning-empty"><span aria-hidden="true">教</span><h2>{title}</h2><p>{text}</p></div>;
}
