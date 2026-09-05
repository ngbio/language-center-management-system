import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";

export default function LanguagesScreen() {
  const [languages, setLanguages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api.get(endpoints.languages)
      .then((response) => { if (active) setLanguages(apiData(response) || []); })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return <section className="languages-page">
    <div className="public-container">
      <div className="catalog-hero"><span className="section-kicker">NGÔN NGỮ ĐÀO TẠO</span><h1>Bạn muốn học ngôn ngữ nào?</h1><p>Chọn một ngôn ngữ để xem toàn bộ khóa học đang được trung tâm mở.</p></div>
      {error && <div className="public-alert">{error}</div>}
      {loading ? <div className="language-card-grid">{[1, 2, 3].map((item) => <div className="language-card language-skeleton" key={item} />)}</div> : <div className="language-card-grid">{languages.map((language) => <Link className="language-card" to={`/khoa-hoc?languageId=${language.id}`} key={language.id}><span>{language.languageCode}</span><h2>{language.languageName}</h2><p>{language.description || "Khám phá các chương trình đào tạo và lộ trình phù hợp."}</p><strong>Xem khóa học →</strong></Link>)}</div>}
      {!loading && !error && languages.length === 0 && <div className="public-empty">Chưa có ngôn ngữ nào đang hoạt động.</div>}
    </div>
  </section>;
}
