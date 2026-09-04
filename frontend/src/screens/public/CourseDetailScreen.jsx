import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatMoney } from "../../utils/api";

const plainText = (html) => {
  if (!html) return "";
  const documentNode = new DOMParser().parseFromString(html, "text/html");
  return documentNode.body.textContent || "";
};

export default function CourseDetailScreen() {
  const { slug } = useParams();
  const [course, setCourse] = useState(null);
  const [sections, setSections] = useState([]);
  const [contents, setContents] = useState({});
  const [openSection, setOpenSection] = useState(null);
  const [loading, setLoading] = useState(true);
  const [sectionLoading, setSectionLoading] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api.get(endpoints["course-by-slug"](slug))
      .then(async (response) => {
        const value = apiData(response);
        if (!active) return;
        setCourse(value);
        const sectionResponse = await api.get(endpoints["course-sections"](value.id));
        if (active) setSections(apiData(sectionResponse) || []);
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [slug]);

  const toggleSection = async (sectionId) => {
    if (openSection === sectionId) { setOpenSection(null); return; }
    setOpenSection(sectionId);
    if (contents[sectionId]) return;
    setSectionLoading(sectionId);
    try {
      const response = await authApis().get(endpoints["section-contents"](sectionId));
      setContents((current) => ({ ...current, [sectionId]: apiData(response) || [] }));
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setSectionLoading(null);
    }
  };

  if (loading) return <div className="public-container detail-loading">Đang tải khóa học...</div>;
  if (error && !course) return <div className="public-container detail-loading"><div className="public-alert">{error}</div><Link to="/">← Về trang chủ</Link></div>;

  return course && (
    <>
      <section className={`detail-hero ${course.bannerUrl ? "has-banner" : ""}`}>
        {course.bannerUrl && (
          <img
            className="detail-banner"
            src={course.bannerUrl}
            alt={`Banner khóa học ${course.courseName}`}
          />
        )}
        <div className="public-container detail-hero-grid">
          <div><Link className="breadcrumb" to="/">Trang chủ</Link><span className="breadcrumb"> / Khóa học</span><span className="section-kicker">{course.languageName} · {course.levelName}</span><h1>{course.courseName}</h1><p>{course.shortDescription || course.description}</p><div className="detail-facts"><span><b>{course.totalSessions}</b> buổi học</span><span><b>{course.durationHours || "—"}</b> giờ</span><span><b>{sections.length}</b> phần nội dung</span></div></div>
          <aside className="enroll-card"><span>Học phí khóa học</span><strong>{Number(course.tuitionFee) === 0 ? "Miễn phí" : formatMoney(course.tuitionFee)}</strong><a className="primary-cta" href="#curriculum">Xem chương trình</a><small>Thông tin lớp khai giảng sẽ được cập nhật thường xuyên.</small></aside>
        </div>
      </section>

      <section className="detail-body public-container">
        <div className="detail-main">
          <div className="detail-panel"><span className="section-kicker">TỔNG QUAN</span><h2>Bạn sẽ nhận được gì?</h2><p>{course.learningOutcomes || course.description || "Nội dung đang được cập nhật."}</p>{course.targetAudience && <><h3>Khóa học dành cho ai?</h3><p>{course.targetAudience}</p></>}{course.prerequisites && <><h3>Yêu cầu đầu vào</h3><p>{course.prerequisites}</p></>}</div>
          <div className="curriculum" id="curriculum"><div className="curriculum-heading"><div><span className="section-kicker">CHƯƠNG TRÌNH HỌC</span><h2>Nội dung theo từng bài</h2></div><span>{sections.length} phần</span></div>{error && <div className="public-alert">{error}</div>}{sections.map((section) => {
            const open = openSection === section.id;
            return <article className={`curriculum-item ${open ? "open" : ""}`} key={section.id}><button onClick={() => toggleSection(section.id)} type="button"><span><small>PHẦN {section.displayOrder}</small><strong>{section.title}</strong><em>{section.description}</em></span><b>{open ? "−" : "+"}</b></button>{open && <div className="content-list">{sectionLoading === section.id && <p>Đang tải nội dung...</p>}{contents[section.id]?.map((content) => <div className="content-row" key={content.id}><span className="content-icon">文</span><div><strong>{content.title}</strong><p>{content.summary}</p>{content.contentHtml && <small>{plainText(content.contentHtml)}</small>}</div><span className="content-type">{content.contentType}</span></div>)}{sectionLoading !== section.id && contents[section.id]?.length === 0 && <p>Phần này chưa có nội dung.</p>}</div>}</article>;
          })}{sections.length === 0 && <div className="public-empty">Chương trình học đang được cập nhật.</div>}</div>
        </div>
        <aside className="detail-side"><div><span className="section-kicker">THÔNG TIN</span><h3>Tổng quan khóa học</h3><dl><dt>Mã khóa học</dt><dd>{course.courseCode}</dd><dt>Trình độ</dt><dd>{course.levelName}</dd><dt>Ngôn ngữ</dt><dd>{course.languageName}</dd><dt>Chứng chỉ</dt><dd>{course.certificateInfo || "Đang cập nhật"}</dd></dl></div></aside>
      </section>
    </>
  );
}
