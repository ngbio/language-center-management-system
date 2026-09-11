import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import api, { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate, formatMoney } from "../../utils/api";
import { isTokenActive, SESSION_KEYS } from "../../utils/authSession";

const days = { 1: "Thứ 2", 2: "Thứ 3", 3: "Thứ 4", 4: "Thứ 5", 5: "Thứ 6", 6: "Thứ 7", 7: "Chủ nhật" };

export default function ClassDetailScreen() {
  const { id } = useParams();
  const [item, setItem] = useState(null);
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [currentEnrollment, setCurrentEnrollment] = useState(null);
  const [checkedEnrollmentClassId, setCheckedEnrollmentClassId] = useState(null);
  const [enrolling, setEnrolling] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const sessionToken = localStorage.getItem(SESSION_KEYS.token);
  const sessionRole = isTokenActive(sessionToken)
    ? localStorage.getItem(SESSION_KEYS.role)
    : null;
  const enrollmentChecking = sessionRole === "STUDENT"
    && String(checkedEnrollmentClassId) !== String(id);
  const displayedEnrollment = Number(currentEnrollment?.courseClassId) === Number(id)
    ? currentEnrollment
    : null;
  useEffect(() => {
    let active = true;
    api.get(endpoints["class-details"](id))
      .then((classResponse) => { if (active) { const value = apiData(classResponse); setItem(value); setSchedules(value?.schedules || []); } })
      .catch((error) => active && setMessage(apiError(error)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [id]);

  useEffect(() => {
    if (sessionRole !== "STUDENT") return undefined;

    let active = true;
    authApis().get(endpoints["my-enrollments"])
      .then((response) => {
        if (!active) return;
        const enrollment = (apiData(response) || []).find(
          (candidate) => Number(candidate.courseClassId) === Number(id),
        );
        setCurrentEnrollment(enrollment || null);
      })
      .catch(() => {})
      .finally(() => { if (active) setCheckedEnrollmentClassId(id); });

    return () => { active = false; };
  }, [id, sessionRole]);

  const enroll = async () => {
    const token = localStorage.getItem(SESSION_KEYS.token);
    if (!isTokenActive(token)) { navigate("/login", { state: { from: location } }); return; }
    if (localStorage.getItem(SESSION_KEYS.role) !== "STUDENT") { setMessage("Chỉ tài khoản học viên mới được đăng ký lớp."); return; }
    setEnrolling(true);
    try {
      const response = await authApis().post(endpoints.enrollments, { courseClassId: Number(id) });
      setCurrentEnrollment(apiData(response));
      setMessage("Đăng ký và giữ chỗ thành công. Bạn có thể thanh toán trong Lịch sử đăng ký.");
    } catch (error) { setMessage(apiError(error)); }
    finally { setEnrolling(false); }
  };
  if (loading) return <div className="public-container detail-loading">Đang tải lớp học...</div>;
  if (!item) return <div className="public-container detail-loading"><div className="public-alert">{message || "Không tìm thấy lớp học."}</div></div>;
  return <section className="languages-page"><div className="public-container">
    <div className="catalog-hero"><Link className="breadcrumb" to="/lop-hoc">Các lớp đang mở</Link><span className="section-kicker">{item.classCode} · {item.levelCode}</span><h1>{item.className}</h1><p>{item.courseName}</p></div>
    {message && <div className="public-alert">{message}</div>}
    <div className="detail-body class-detail-body"><main className="detail-main"><div className="detail-panel"><h2>Lịch học cố định</h2>{schedules.length ? <div className="enrollment-class-list">{schedules.map((schedule) => <div className="class-schedule-detail" key={schedule.id}><strong>{days[schedule.dayOfWeek]}</strong><span>{schedule.startTime} – {schedule.endTime}</span><small>{schedule.deliveryMode === "ONLINE" ? "Trực tuyến" : `${schedule.roomName || "Phòng đang cập nhật"}${schedule.roomLocation ? ` · ${schedule.roomLocation}` : ""}`}</small></div>)}</div> : <div className="public-empty">Lịch học đang được cập nhật.</div>}</div></main>
      <aside className="enroll-card"><span>Học phí lớp</span><strong>{Number(item.appliedTuitionFee) === 0 ? "Miễn phí" : formatMoney(item.appliedTuitionFee)}</strong><p>{formatDate(item.startDate)} – {formatDate(item.endDate)}</p><small>Giảng viên: {item.teacherName || "Đang cập nhật"}</small><small>Còn {item.availableSeats} chỗ</small>{displayedEnrollment ? <><div className="already-enrolled">✓ {enrollmentStatusText(displayedEnrollment)}</div><Link className="enroll-curriculum-link" to="/lich-su-dang-ky">Xem đăng ký của tôi →</Link></> : sessionRole === "STUDENT" ? <button className="primary-cta enroll-action" type="button" onClick={enroll} disabled={enrollmentChecking || enrolling}>{enrollmentChecking ? "Đang kiểm tra..." : enrolling ? "Đang đăng ký..." : "Chọn lớp và đăng ký"}</button> : sessionRole === null ? <button className="primary-cta enroll-action" type="button" onClick={enroll}>Đăng nhập để đăng ký</button> : <small>Chỉ tài khoản học viên mới có thể đăng ký lớp.</small>}</aside>
    </div>
  </div></section>;
}

const enrollmentStatusText = (enrollment) => {
  if (enrollment.enrollmentStatus === "CANCELLED") return "Đăng ký đã hủy";
  if (enrollment.paymentStatus === "REFUNDED") return "Đã hoàn tiền";
  if (enrollment.paymentStatus === "PAID") return "Đã đăng ký và thanh toán";
  return "Đã giữ chỗ, chờ thanh toán";
};
