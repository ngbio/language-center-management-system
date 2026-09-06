import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError, formatMoney } from "../../utils/api";

const placeholders = Array.from({ length: 6 });

export default function CoursesScreen() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [languages, setLanguages] = useState([]);
  const [levels, setLevels] = useState([]);
  const [courses, setCourses] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const languageId = searchParams.get("languageId") || "";
  const keyword = searchParams.get("keyword") || "";
  const levelId = searchParams.get("levelId") || "";
  const sort = searchParams.get("sort") || "courseName";
  const direction = searchParams.get("direction") || "asc";
  const page = Math.max(Number(searchParams.get("page") || 0), 0);

  useEffect(() => {
    api.get(endpoints.languages).then((response) => setLanguages(apiData(response) || [])).catch(() => {});
  }, []);

  useEffect(() => {
    const endpoint = languageId ? endpoints["language-levels"](languageId) : endpoints.levels;
    api.get(endpoint).then((response) => setLevels(apiData(response) || [])).catch(() => setLevels([]));
  }, [languageId]);

  useEffect(() => {
    let active = true;
    api.get(endpoints.courses, { params: { page, size: 9, keyword: keyword || undefined, languageId: languageId || undefined, levelId: levelId || undefined, sort, direction } })
      .then((response) => { if (active) { const data = apiData(response); setCourses(data?.content || []); setTotalPages(data?.totalPages || 0); } })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [keyword, languageId, levelId, sort, direction, page]);

  const selectedLanguage = languages.find((item) => String(item.id) === languageId);
  const updateFilters = (updates) => {
    setLoading(true);
    setError("");
    const next = Object.fromEntries(searchParams.entries());
    Object.entries(updates).forEach(([key, value]) => value ? next[key] = value : delete next[key]);
    if (!("page" in updates)) delete next.page;
    setSearchParams(next);
  };

  return <section className="course-section catalog-page">
    <div className="public-container">
      <div className="catalog-hero"><span className="section-kicker">CHƯƠNG TRÌNH ĐÀO TẠO</span><h1>{selectedLanguage ? `Khóa học ${selectedLanguage.languageName}` : "Tất cả khóa học"}</h1><p>Khám phá khóa học theo ngôn ngữ và chọn lộ trình phù hợp với mục tiêu của bạn.</p></div>
      <div className="catalog-toolbar">
        <input value={keyword} placeholder="Tìm tên hoặc mã khóa học..." onChange={(event) => updateFilters({ keyword: event.target.value })} />
        <select value={languageId} onChange={(event) => updateFilters({ languageId: event.target.value, levelId: "" })}><option value="">Tất cả ngôn ngữ</option>{languages.map((language) => <option value={language.id} key={language.id}>{language.languageName}</option>)}</select>
        <select value={levelId} onChange={(event) => updateFilters({ levelId: event.target.value })}><option value="">Tất cả trình độ</option>{levels.map((level) => <option value={level.id} key={level.id}>{level.levelCode} · {level.levelName}</option>)}</select>
        <select value={`${sort}:${direction}`} onChange={(event) => { const [nextSort, nextDirection] = event.target.value.split(":"); updateFilters({ sort: nextSort, direction: nextDirection }); }}><option value="courseName:asc">Tên A–Z</option><option value="courseName:desc">Tên Z–A</option><option value="tuitionFee:asc">Học phí thấp nhất</option><option value="tuitionFee:desc">Học phí cao nhất</option><option value="createdAt:desc">Khóa học mới nhất</option></select>
      </div>
      {error && <div className="public-alert">{error}</div>}
      <div className="course-grid">
        {loading && placeholders.map((_, index) => <div className="course-card course-skeleton" key={index} />)}
        {!loading && courses.map((course, index) => <CourseCard course={course} index={index} key={course.id} />)}
      </div>
      {!loading && !error && courses.length === 0 && <div className="public-empty">Chưa có khóa học phù hợp.</div>}
      {!loading && totalPages > 1 && <div className="public-pagination"><button disabled={page === 0} onClick={() => updateFilters({ page: String(page - 1) })}>← Trang trước</button><span>Trang {page + 1} / {totalPages}</span><button disabled={page + 1 >= totalPages} onClick={() => updateFilters({ page: String(page + 1) })}>Trang sau →</button></div>}
    </div>
  </section>;
}

function CourseCard({ course, index }) {
  return <article className="course-card">
    <Link className={`course-cover cover-${index % 4}`} to={`/khoa-hoc/${course.slug}`}>
      {course.thumbnailUrl ? <img src={course.thumbnailUrl} alt={`Ảnh khóa học ${course.courseName}`} /> : <><span>{course.languageCode || "LC"}</span><b>{course.levelCode || "Starter"}</b></>}
      {course.featured && <em>Nổi bật</em>}
    </Link>
    <div className="course-card-body"><span className="course-meta">{course.languageName} · {course.levelName}</span><h3><Link to={`/khoa-hoc/${course.slug}`}>{course.courseName}</Link></h3><p>{course.shortDescription || course.description || "Khám phá nội dung và lộ trình của khóa học."}</p><div className="course-facts"><span>◷ {course.totalSessions} buổi</span><span>▤ {course.durationHours || "—"} giờ</span></div><div className="course-card-foot"><strong>{Number(course.tuitionFee) === 0 ? "Miễn phí" : formatMoney(course.tuitionFee)}</strong><Link to={`/khoa-hoc/${course.slug}`}>Xem chi tiết →</Link></div></div>
  </article>;
}
