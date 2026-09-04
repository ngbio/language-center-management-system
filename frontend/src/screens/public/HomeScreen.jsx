import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError, formatMoney } from "../../utils/api";

const fallbackCourses = Array.from({ length: 3 });

export default function HomeScreen() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api.get(endpoints.courses, { params: { page: 0, size: 12, sort: "courseName" } })
      .then((response) => {
        if (active) setCourses(apiData(response)?.content || []);
      })
      .catch((requestError) => {
        if (active) setError(apiError(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  return (
    <>
      <section className="public-hero">
        <div className="public-container hero-grid">
          <div className="hero-copy">
            <span className="eyebrow-pill">LỘ TRÌNH HỌC THỰC TẾ</span>
            <h1>Chạm gần hơn đến thế giới bằng một ngôn ngữ mới.</h1>
            <p>Khóa học có cấu trúc rõ ràng, nội dung theo từng bài và lịch học linh hoạt dành cho mọi trình độ.</p>
            <div className="hero-actions"><a className="primary-cta" href="#courses">Khám phá khóa học</a><a className="text-cta" href="#benefits">Tại sao chọn chúng tôi <span>→</span></a></div>
            <div className="hero-stats"><div><strong>25+</strong><span>Bài học chọn lọc</span></div><div><strong>3</strong><span>Ngôn ngữ</span></div><div><strong>4.9/5</strong><span>Đánh giá học viên</span></div></div>
          </div>
          <div className="hero-visual" aria-label="Minh họa học ngoại ngữ">
            <div className="sun-disc" />
            <div className="hero-card hero-card-main"><span>日本語</span><strong>Học từng bài.<br />Tiến bộ mỗi ngày.</strong><small>Minna no Nihongo · N5</small></div>
            <div className="hero-card hero-card-float"><b>あ</b><span>New lesson<br /><strong>ready to learn</strong></span></div>
            <div className="hero-dots">••••••••••••</div>
          </div>
        </div>
      </section>

      <section className="course-section" id="courses">
        <div className="public-container">
          <div className="section-heading"><div><span className="section-kicker">CHƯƠNG TRÌNH NỔI BẬT</span><h2>Chọn khóa học phù hợp với bạn</h2></div><p>Mỗi khóa học được chia thành từng phần nhỏ để bạn dễ xem lộ trình trước khi bắt đầu.</p></div>
          {error && <div className="public-alert">{error}</div>}
          <div className="course-grid">
            {loading && fallbackCourses.map((_, index) => <div className="course-card course-skeleton" key={index} />)}
            {!loading && courses.map((course, index) => (
              <article className="course-card" key={course.id}>
                <Link className={`course-cover cover-${index % 4}`} to={`/khoa-hoc/${course.slug}`}>
                  {course.thumbnailUrl ? <img src={course.thumbnailUrl} alt="" /> : <><span>{course.languageCode || "LC"}</span><b>{course.levelCode || "Starter"}</b></>}
                  {course.featured && <em>Nổi bật</em>}
                </Link>
                <div className="course-card-body">
                  <span className="course-meta">{course.languageName} · {course.levelName}</span>
                  <h3><Link to={`/khoa-hoc/${course.slug}`}>{course.courseName}</Link></h3>
                  <p>{course.shortDescription || course.description || "Khám phá nội dung và lộ trình của khóa học."}</p>
                  <div className="course-facts"><span>◷ {course.totalSessions} buổi</span><span>▤ {course.durationHours || "—"} giờ</span></div>
                  <div className="course-card-foot"><strong>{Number(course.tuitionFee) === 0 ? "Miễn phí" : formatMoney(course.tuitionFee)}</strong><Link to={`/khoa-hoc/${course.slug}`}>Xem chi tiết →</Link></div>
                </div>
              </article>
            ))}
          </div>
          {!loading && !error && courses.length === 0 && <div className="public-empty">Chưa có khóa học nào được xuất bản.</div>}
        </div>
      </section>

      <section className="benefits-section" id="benefits">
        <div className="public-container"><div className="section-heading centered"><div><span className="section-kicker">HỌC CÓ ĐỊNH HƯỚNG</span><h2>Một hành trình vừa sức, dễ theo dõi</h2></div></div><div className="benefit-grid"><article><span>01</span><h3>Nội dung theo từng bài</h3><p>Mở section và chỉ tải nội dung khi bạn cần xem.</p></article><article><span>02</span><h3>Lộ trình rõ ràng</h3><p>Biết mình sẽ học gì và đang tiến tới đâu.</p></article><article><span>03</span><h3>Linh hoạt mọi thiết bị</h3><p>Giao diện tối ưu cho máy tính, máy tính bảng và điện thoại.</p></article></div></div>
      </section>
    </>
  );
}
