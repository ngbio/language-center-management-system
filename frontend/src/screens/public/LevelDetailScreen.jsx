import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError, formatMoney } from "../../utils/api";

export default function LevelDetailScreen() {
  const { id } = useParams();
  const [level, setLevel] = useState(null);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    Promise.all([api.get(endpoints["level-details"](id)), api.get(endpoints.courses, { params: { levelId: id, page: 0, size: 100, sort: "courseName", direction: "asc" } })])
      .then(([levelResponse, courseResponse]) => { if (active) { setLevel(apiData(levelResponse)); setCourses(apiData(courseResponse)?.content || []); } })
      .catch((requestError) => active && setError(apiError(requestError)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [id]);
  if (loading) return <div className="public-container detail-loading">Đang tải trình độ...</div>;
  if (error || !level) return <div className="public-container detail-loading"><div className="public-alert">{error || "Không tìm thấy trình độ."}</div></div>;
  return <section className="languages-page"><div className="public-container">
    <div className="catalog-hero"><Link className="breadcrumb" to={`/ngon-ngu/${level.languageId}`}>{level.languageName}</Link><span className="section-kicker">{level.languageCode} · {level.levelCode}</span><h1>{level.levelName}</h1><p>{level.description || "Thông tin trình độ đang được trung tâm cập nhật."}</p></div>
    <div className="course-grid">{courses.map((course, index) => <article className="course-card" key={course.id}><div className={`course-cover cover-${index % 3 + 1}`}>{course.thumbnailUrl ? <img src={course.thumbnailUrl} alt={course.courseName} /> : <span>{course.languageCode}</span>}</div><div className="course-card-body"><span className="course-meta">{course.levelName}</span><h3>{course.courseName}</h3><p>{course.shortDescription || "Thông tin khóa học đang cập nhật."}</p><div className="course-card-foot"><strong>{Number(course.tuitionFee) === 0 ? "Miễn phí" : formatMoney(course.tuitionFee)}</strong><Link to={`/khoa-hoc/${course.slug}`}>Chi tiết →</Link></div></div></article>)}</div>
    {courses.length === 0 && <div className="public-empty">Các khóa học thuộc trình độ này đang được cập nhật.</div>}
  </div></section>;
}
