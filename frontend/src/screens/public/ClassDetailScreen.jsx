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
  const navigate = useNavigate();
  const location = useLocation();
  useEffect(() => {
    let active = true;
    Promise.all([api.get(endpoints["class-details"](id)), api.get(endpoints["class-schedules"](id))])
      .then(([classResponse, scheduleResponse]) => { if (active) { setItem(apiData(classResponse)); setSchedules(apiData(scheduleResponse) || []); } })
      .catch((error) => active && setMessage(apiError(error)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [id]);
  const enroll = async () => {
    const token = localStorage.getItem(SESSION_KEYS.token);
    if (!isTokenActive(token)) { navigate("/login", { state: { from: location } }); return; }
    if (localStorage.getItem(SESSION_KEYS.role) !== "STUDENT") { setMessage("Chỉ tài khoản học viên mới được đăng ký lớp."); return; }
    try {
      await authApis().post(endpoints.enrollments, { courseClassId: Number(id) });
      setMessage("Đăng ký và giữ chỗ thành công. Bạn có thể thanh toán trong Lịch sử đăng ký.");
    } catch (error) { setMessage(apiError(error)); }
  };
  if (loading) return <div className="public-container detail-loading">Đang tải lớp học...</div>;
  if (!item) return <div className="public-container detail-loading"><div className="public-alert">{message || "Không tìm thấy lớp học."}</div></div>;
  return <section className="languages-page"><div className="public-container">
    <div className="catalog-hero"><Link className="breadcrumb" to="/lop-hoc">Các lớp đang mở</Link><span className="section-kicker">{item.classCode} · {item.levelCode}</span><h1>{item.className}</h1><p>{item.courseName}</p></div>
    {message && <div className="public-alert">{message}</div>}
    <div className="detail-body class-detail-body"><main className="detail-main"><div className="detail-panel"><h2>Lịch học cố định</h2>{schedules.length ? <div className="enrollment-class-list">{schedules.map((schedule) => <div className="class-schedule-detail" key={schedule.id}><strong>{days[schedule.dayOfWeek]}</strong><span>{schedule.startTime} – {schedule.endTime}</span><small>{schedule.deliveryMode === "ONLINE" ? "Trực tuyến" : `${schedule.roomName || "Phòng đang cập nhật"}${schedule.roomLocation ? ` · ${schedule.roomLocation}` : ""}`}</small></div>)}</div> : <div className="public-empty">Lịch học đang được cập nhật.</div>}</div></main>
      <aside className="enroll-card"><span>Học phí lớp</span><strong>{Number(item.appliedTuitionFee) === 0 ? "Miễn phí" : formatMoney(item.appliedTuitionFee)}</strong><p>{formatDate(item.startDate)} – {formatDate(item.endDate)}</p><small>Giảng viên: {item.teacherName || "Đang cập nhật"}</small><small>Còn {item.availableSeats} chỗ</small><button className="primary-cta enroll-action" type="button" onClick={enroll}>Chọn lớp và đăng ký</button></aside>
    </div>
  </div></section>;
}
