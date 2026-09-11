import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate, formatMoney } from "../../utils/api";

export default function OpenClassesScreen() {
  const [params, setParams] = useSearchParams();
  const [classes, setClasses] = useState([]);
  const [pageInfo, setPageInfo] = useState(null);
  const [courses, setCourses] = useState([]);
  const [levels, setLevels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [keywordInput, setKeywordInput] = useState(params.get("keyword") || "");
  const filters = {
    keyword: params.get("keyword") || "",
    courseId: params.get("courseId") || "",
    levelId: params.get("levelId") || "",
    date: params.get("date") || "",
    sort: params.get("sort") || "startDate",
    direction: params.get("direction") || "asc",
    page: Math.max(Number(params.get("page")) || 0, 0),
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (keywordInput === (params.get("keyword") || "")) return;
      const next = Object.fromEntries(params.entries());
      if (keywordInput) next.keyword = keywordInput; else delete next.keyword;
      delete next.page;
      setParams(next, { replace: true });
    }, 400);
    return () => window.clearTimeout(timer);
  }, [keywordInput, params, setParams]);

  useEffect(() => {
    Promise.all([
      api.get(endpoints.courses, { params: { page: 0, size: 100, sort: "courseName" } }),
      api.get(endpoints.levels),
    ]).then(([courseResponse, levelResponse]) => {
      setCourses(apiData(courseResponse)?.content || []);
      setLevels(apiData(levelResponse) || []);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    let active = true;
    api.get(endpoints.classes, { params: { ...filters, keyword: filters.keyword || undefined, courseId: filters.courseId || undefined, levelId: filters.levelId || undefined, date: filters.date || undefined, size: 12 } })
      .then((response) => {
        const page = apiData(response);
        const values = page?.content || [];
        if (!active) return;
        setClasses(values);
        setPageInfo(page || null);
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.keyword, filters.courseId, filters.levelId, filters.date, filters.sort, filters.direction, filters.page]);

  const update = (key, value) => {
    setLoading(true);
    setError("");
    const next = Object.fromEntries(params.entries());
    if (value) next[key] = value; else delete next[key];
    if (key !== "page") delete next.page;
    setParams(next);
  };

  return <section className="languages-page open-classes-page"><div className="public-container">
    <div className="catalog-hero"><span className="section-kicker">LỊCH KHAI GIẢNG</span><h1>Các lớp học đang mở</h1><p>Lọc theo khóa học, trình độ hoặc ngày khai giảng để tìm lớp phù hợp.</p></div>
    <div className="catalog-toolbar class-filter-toolbar">
      <input value={keywordInput} placeholder="Tìm mã hoặc tên lớp..." onChange={(event) => setKeywordInput(event.target.value)} />
      <select value={filters.courseId} onChange={(event) => update("courseId", event.target.value)}><option value="">Tất cả khóa học</option>{courses.map((course) => <option value={course.id} key={course.id}>{course.courseName}</option>)}</select>
      <select value={filters.levelId} onChange={(event) => update("levelId", event.target.value)}><option value="">Tất cả trình độ</option>{levels.map((level) => <option value={level.id} key={level.id}>{level.languageCode} · {level.levelCode}</option>)}</select>
      <input type="date" value={filters.date} aria-label="Ngày khai giảng" onChange={(event) => update("date", event.target.value)} />
      <select value={`${filters.sort}:${filters.direction}`} onChange={(event) => { const [sort, direction] = event.target.value.split(":"); const next = Object.fromEntries(params.entries()); next.sort = sort; next.direction = direction; setLoading(true); setParams(next); }}><option value="startDate:asc">Khai giảng gần nhất</option><option value="startDate:desc">Khai giảng mới nhất</option><option value="appliedTuitionFee:asc">Học phí thấp nhất</option><option value="appliedTuitionFee:desc">Học phí cao nhất</option></select>
    </div>
    {error && <div className="public-alert">{error}</div>}
    {loading ? <div className="public-empty">Đang tải danh sách lớp...</div> : <div className="open-class-grid">{classes.map((item) => <article className="open-class-card" key={item.id}><div><span>{item.levelCode}</span><b>{item.availableSeats} chỗ còn lại</b></div><small>{item.classCode}</small><h2>{item.className}</h2><p>{item.courseName}</p><div className="class-weekly-schedule">{formatSchedules(item.schedules)}</div><dl><div><dt>Khai giảng</dt><dd>{formatDate(item.startDate)}</dd></div><div><dt>Kết thúc</dt><dd>{formatDate(item.endDate)}</dd></div><div><dt>Giáo viên</dt><dd>{item.teacherName || "Đang cập nhật"}</dd></div></dl><footer><strong>{formatMoney(item.appliedTuitionFee)}</strong><Link to={`/lop-hoc/${item.id}`}>Xem lớp →</Link></footer></article>)}</div>}
    {!loading && pageInfo?.totalPages > 1 && <div className="catalog-pagination"><button type="button" disabled={pageInfo.first} onClick={() => update("page", String(filters.page - 1))}>Trang trước</button><span>Trang {filters.page + 1}/{pageInfo.totalPages}</span><button type="button" disabled={pageInfo.last} onClick={() => update("page", String(filters.page + 1))}>Trang sau</button></div>}
    {!loading && !error && classes.length === 0 && <div className="public-empty">Không tìm thấy lớp đang mở phù hợp.</div>}
  </div></section>;
}

const dayNames = { 1: "Thứ 2", 2: "Thứ 3", 3: "Thứ 4", 4: "Thứ 5", 5: "Thứ 6", 6: "Thứ 7", 7: "Chủ nhật" };
const formatSchedules = (values = []) => values.length
  ? values.map((item) => `${dayNames[item.dayOfWeek] || `Thứ ${item.dayOfWeek}`} ${item.startTime}–${item.endTime}`).join(" · ")
  : "Lịch học đang cập nhật";
