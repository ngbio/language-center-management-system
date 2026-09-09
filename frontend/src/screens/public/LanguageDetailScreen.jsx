import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";

export default function LanguageDetailScreen() {
  const { id } = useParams();
  const [language, setLanguage] = useState(null);
  const [levels, setLevels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    Promise.all([api.get(endpoints["language-details"](id)), api.get(endpoints["language-levels"](id))])
      .then(([languageResponse, levelResponse]) => { if (active) { setLanguage(apiData(languageResponse)); setLevels(apiData(levelResponse) || []); } })
      .catch((requestError) => active && setError(apiError(requestError)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [id]);
  if (loading) return <div className="public-container detail-loading">Đang tải ngôn ngữ...</div>;
  if (error || !language) return <div className="public-container detail-loading"><div className="public-alert">{error || "Không tìm thấy ngôn ngữ."}</div></div>;
  return <section className="languages-page"><div className="public-container">
    <div className="catalog-hero"><Link className="breadcrumb" to="/ngon-ngu">Ngôn ngữ</Link><span className="section-kicker">{language.languageCode}</span><h1>{language.languageName}</h1><p>{language.description || "Thông tin ngôn ngữ đang được trung tâm cập nhật."}</p></div>
    <div className="section-heading"><div><span className="section-kicker">LỘ TRÌNH ĐÀO TẠO</span><h2>Các trình độ</h2></div></div>
    <div className="language-card-grid">{levels.map((level) => <Link className="language-card" to={`/trinh-do/${level.id}`} key={level.id}><span>{level.levelCode}</span><h2>{level.levelName}</h2><p>{level.description || "Nội dung trình độ đang được cập nhật."}</p><strong>Xem chi tiết →</strong></Link>)}</div>
    {levels.length === 0 && <div className="public-empty">Các trình độ đang được cập nhật.</div>}
  </div></section>;
}
