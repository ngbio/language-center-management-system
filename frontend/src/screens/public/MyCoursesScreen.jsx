import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import api, { authApis, endpoints } from "../../configs/Apis";
import { SESSION_KEYS, isTokenActive } from "../../utils/authSession";
import { apiData, apiError, formatMoney } from "../../utils/api";
import "../../styles/StudentCourses.css";

const placeholders = Array.from({ length: 3 });

export default function MyCoursesScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [purchasedCourses, setPurchasedCourses] = useState([]);
  const [freeCourses, setFreeCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (role !== "STUDENT" || !isTokenActive(token)) return undefined;
    let active = true;
    Promise.all([
      authApis().get(endpoints["my-courses"]),
      api.get(endpoints.courses, { params: { page: 0, size: 100, sort: "courseName" } }),
    ])
      .then(([purchasedResponse, publicResponse]) => {
        if (!active) return;
        const purchased = apiData(purchasedResponse) || [];
        const free = (apiData(publicResponse)?.content || []).filter(
          (course) => Number(course.tuitionFee) === 0,
        );
        setPurchasedCourses(purchased.filter((course) => Number(course.tuitionFee) > 0));
        setFreeCourses(free);
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [role, token]);

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "STUDENT") return <Navigate to="/" replace />;

  return (
    <section className="my-courses-page">
      <div className="my-courses-hero"><div className="public-container">
        <span className="section-kicker">KHÔNG GIAN HỌC TẬP</span>
        <h1>Khóa học của tôi</h1>
        <p>Học các khóa đã mua và khám phá toàn bộ nội dung miễn phí.</p>
      </div></div>
      <div className="public-container my-courses-content">
        <div className="my-courses-heading">
          <div><strong>{loading ? "—" : purchasedCourses.length + freeCourses.length}</strong><span>Khóa học dành cho bạn</span></div>
          <Link to="/#courses">Khám phá thêm khóa học →</Link>
        </div>
        {error && <div className="public-alert">{error}</div>}
        {loading && <div className="course-grid">{placeholders.map((_, index) => <div className="course-card course-skeleton" key={index} />)}</div>}
        {!loading && <>
          <CourseGroup title="Khóa học đã mua" description="Các khóa học đã xác nhận và thanh toán thành công." courses={purchasedCourses} badge="Đã mua" emptyText="Bạn chưa mua khóa học có phí nào." />
          <CourseGroup title="Khóa học miễn phí" description="Có thể xem nội dung đã xuất bản mà không cần đăng ký hoặc thanh toán." courses={freeCourses} badge="Miễn phí" emptyText="Hiện chưa có khóa học miễn phí." free />
        </>}
      </div>
    </section>
  );
}

function CourseGroup({ title, description, courses, badge, emptyText, free = false }) {
  return <section className="my-course-group">
    <header><div><span className="section-kicker">{free ? "HỌC NGAY" : "ĐÃ KÍCH HOẠT"}</span><h2>{title}</h2><p>{description}</p></div><strong>{courses.length}</strong></header>
    {courses.length ? <div className="course-grid">{courses.map((course, index) => <CourseCard course={course} index={index} badge={badge} free={free} key={course.id} />)}</div> : <div className="course-group-empty">{emptyText}</div>}
  </section>;
}

function CourseCard({ course, index, badge, free }) {
  return <article className="course-card owned-course-card">
    <Link className={`course-cover cover-${index % 4}`} to={`/khoa-hoc/${course.slug}`}>
      {course.thumbnailUrl ? <img src={course.thumbnailUrl} alt={`Ảnh khóa học ${course.courseName}`} /> : <><span>{course.languageCode || "LC"}</span><b>{course.levelCode || "Starter"}</b></>}
      <em className={free ? "free-course-badge" : "owned-badge"}>{badge}</em>
    </Link>
    <div className="course-card-body">
      <span className="course-meta">{course.languageName} · {course.levelName}</span>
      <h3><Link to={`/khoa-hoc/${course.slug}`}>{course.courseName}</Link></h3>
      <p>{course.shortDescription || "Khóa học đã sẵn sàng để bạn bắt đầu."}</p>
      <div className="course-facts"><span>◷ {course.totalSessions} buổi</span><span>▤ {course.durationHours || "—"} giờ</span></div>
      <div className="course-card-foot"><strong>{free ? "Miễn phí" : formatMoney(course.tuitionFee)}</strong><Link className="continue-course" to={`/khoa-hoc/${course.slug}`}>Vào học →</Link></div>
    </div>
  </article>;
}
