import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import api, { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate, formatMoney } from "../../utils/api";
import { isTokenActive, SESSION_KEYS } from "../../utils/authSession";

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
  const [enrollmentOpen, setEnrollmentOpen] = useState(false);
  const [openClasses, setOpenClasses] = useState([]);
  const [selectedClass, setSelectedClass] = useState(null);
  const [selectedSchedules, setSelectedSchedules] = useState([]);
  const [enrollmentLoading, setEnrollmentLoading] = useState(false);
  const [enrollmentMessage, setEnrollmentMessage] = useState("");
  const [currentEnrollment, setCurrentEnrollment] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();
  const sessionToken = localStorage.getItem(SESSION_KEYS.token);
  const sessionRole = isTokenActive(sessionToken) ? localStorage.getItem(SESSION_KEYS.role) : null;
  const canRegister = sessionRole === null || sessionRole === "STUDENT";

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

  useEffect(() => {
    if (!course) return undefined;
    const token = localStorage.getItem(SESSION_KEYS.token);
    const role = localStorage.getItem(SESSION_KEYS.role);
    if (!isTokenActive(token) || role !== "STUDENT") return undefined;
    let active = true;
    authApis().get(endpoints["my-enrollments"])
      .then((response) => {
        if (!active) return;
        const enrollment = (apiData(response) || []).find((item) =>
          item.courseId === course.id && ["PENDING", "CONFIRMED"].includes(item.enrollmentStatus));
        setCurrentEnrollment(enrollment || null);
      })
      .catch(() => {});
    return () => { active = false; };
  }, [course]);

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

  const beginEnrollment = async () => {
    const token = localStorage.getItem(SESSION_KEYS.token);
    const role = localStorage.getItem(SESSION_KEYS.role);
    if (!isTokenActive(token)) {
      navigate("/login", { state: { from: location } });
      return;
    }
    if (role !== "STUDENT") {
      setError("Chỉ tài khoản học viên mới có thể đăng ký lớp học.");
      return;
    }
    setEnrollmentOpen(true);
    setEnrollmentLoading(true);
    setEnrollmentMessage("");
    setSelectedClass(null);
    setSelectedSchedules([]);
    try {
      const response = await api.get(endpoints.classes, { params: { courseId: course.id, page: 0, size: 100, sort: "startDate", direction: "asc" } });
      setOpenClasses(apiData(response)?.content || []);
    } catch (requestError) {
      setEnrollmentMessage(apiError(requestError));
    } finally {
      setEnrollmentLoading(false);
    }
  };

  const chooseClass = async (item) => {
    setSelectedClass(item);
    setSelectedSchedules([]);
    try {
      const response = await api.get(endpoints["class-schedules"](item.id));
      setSelectedSchedules(apiData(response) || []);
    } catch (requestError) {
      setEnrollmentMessage(apiError(requestError));
    }
  };

  const confirmEnrollment = async () => {
    if (!selectedClass) return;
    const token = localStorage.getItem(SESSION_KEYS.token);
    const role = localStorage.getItem(SESSION_KEYS.role);
    if (!isTokenActive(token) || role !== "STUDENT") {
      setEnrollmentOpen(false);
      setError("Chỉ tài khoản học viên mới có thể đăng ký lớp học.");
      return;
    }
    setEnrollmentLoading(true);
    setEnrollmentMessage("");
    try {
      const response = await authApis().post(endpoints.enrollments, { courseClassId: selectedClass.id });
      const enrollment = apiData(response);
      setCurrentEnrollment(enrollment);
      setEnrollmentMessage(
        Number(selectedClass.appliedTuitionFee) > 0
          ? "Đăng ký và giữ chỗ thành công. Vui lòng thanh toán trong vòng 48 giờ tại Lịch sử đăng ký."
          : "Đăng ký lớp miễn phí thành công. Quyền học đã được kích hoạt.",
      );
      setOpenClasses([]);
    } catch (requestError) {
      setEnrollmentMessage(apiError(requestError));
    } finally {
      setEnrollmentLoading(false);
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
          <aside className="enroll-card"><span>Học phí khóa học</span><strong>{Number(course.tuitionFee) === 0 ? "Miễn phí" : formatMoney(course.tuitionFee)}</strong>{currentEnrollment ? <><div className="already-enrolled">✓ {enrollmentStatusText(currentEnrollment)}</div><Link className="enroll-curriculum-link" to="/lich-su-dang-ky">Xem đăng ký của tôi →</Link></> : canRegister && <button className="primary-cta enroll-action" type="button" onClick={beginEnrollment}>Đăng ký khóa học</button>}<a className="enroll-curriculum-link" href="#curriculum">Xem chương trình</a><small>{currentEnrollment ? `Lớp: ${currentEnrollment.className}` : canRegister ? "Chọn một lớp đang mở và gửi yêu cầu đăng ký tới trung tâm." : "Tài khoản giảng viên và nhân viên không đăng ký khóa học."}</small></aside>
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
      {enrollmentOpen && <div className="public-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setEnrollmentOpen(false); }}><section className="public-enrollment-modal" role="dialog" aria-modal="true" aria-labelledby="enrollment-title"><button className="public-modal-close" type="button" onClick={() => setEnrollmentOpen(false)}>×</button><span className="section-kicker">ĐĂNG KÝ KHÓA HỌC</span><h2 id="enrollment-title">Chọn lớp cho {course.courseName}</h2>{enrollmentMessage && <div className={openClasses.length ? "public-alert" : "register-note"}>{enrollmentMessage}</div>}{enrollmentLoading && <div className="public-empty">Đang xử lý...</div>}{!enrollmentLoading && openClasses.length > 0 && <><div className="enrollment-class-list">{openClasses.map((item) => <button type="button" className={selectedClass?.id === item.id ? "selected" : ""} onClick={() => chooseClass(item)} key={item.id}><span><strong>{item.className}</strong><small>{item.classCode} · {item.levelCode}</small></span><span><b>{formatDate(item.startDate)}</b><small>{item.availableSeats} chỗ còn lại</small></span></button>)}</div>{selectedClass && <div className="enrollment-confirm"><h3>Thông tin đăng ký</h3><dl><div><dt>Lớp</dt><dd>{selectedClass.className}</dd></div><div><dt>Thời gian</dt><dd>{formatDate(selectedClass.startDate)} – {formatDate(selectedClass.endDate)}</dd></div><div><dt>Lịch cố định</dt><dd>{formatWeeklySchedules(selectedSchedules)}</dd></div><div><dt>Giáo viên</dt><dd>{selectedClass.teacherName || "Đang cập nhật"}</dd></div><div><dt>Học phí</dt><dd>{formatMoney(selectedClass.appliedTuitionFee)}</dd></div></dl><p className="payment-later-note">Bạn có thể thanh toán sau trong mục Lịch sử đăng ký.</p><button className="primary-cta" type="button" onClick={confirmEnrollment}>Xác nhận đăng ký</button></div>}</>}{!enrollmentLoading && !enrollmentMessage && openClasses.length === 0 && <div className="public-empty">Khóa học này chưa có lớp đang mở đăng ký.</div>}</section></div>}
    </>
  );
}

const weekDays = { 1: "Thứ 2", 2: "Thứ 3", 3: "Thứ 4", 4: "Thứ 5", 5: "Thứ 6", 6: "Thứ 7", 7: "Chủ nhật" };
const formatWeeklySchedules = (values) => values.length
  ? values.map((item) => `${weekDays[item.dayOfWeek]} ${item.startTime}–${item.endTime}`).join(", ")
  : "Đang cập nhật";
const enrollmentStatusText = (enrollment) => enrollment.enrollmentStatus === "PENDING"
  ? "Đang chờ trung tâm xác nhận"
  : enrollment.paymentStatus === "PAID" ? "Đã đăng ký và thanh toán" : "Đã được xác nhận";
