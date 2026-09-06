import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate, formatMoney } from "../../utils/api";
import { SESSION_KEYS, isTokenActive } from "../../utils/authSession";

const labels = { PENDING: "Đang chờ", CONFIRMED: "Đã xác nhận", CANCELLED: "Đã hủy", PAID: "Đã thanh toán", FAILED: "Thất bại", REFUNDED: "Đã hoàn tiền" };

export default function EnrollmentHistoryScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [enrollments, setEnrollments] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [payingId, setPayingId] = useState(null);
  const [methods, setMethods] = useState({});

  const loadHistory = async () => {
    setLoading(true); setError("");
    try {
      const api = authApis();
      const [enrollmentResponse, paymentResponse] = await Promise.all([
        api.get(endpoints["my-enrollments"]), api.get(endpoints["my-payments"]),
      ]);
      setEnrollments(apiData(enrollmentResponse) || []);
      setPayments(apiData(paymentResponse) || []);
    } catch (requestError) { setError(apiError(requestError)); }
    finally { setLoading(false); }
  };

  useEffect(() => { if (role === "STUDENT" && isTokenActive(token)) loadHistory(); }, [role, token]);

  const latestPayment = useMemo(() => {
    const result = new Map();
    payments.forEach((payment) => { if (!result.has(payment.enrollmentId)) result.set(payment.enrollmentId, payment); });
    return result;
  }, [payments]);
  const enrollmentById = useMemo(() => new Map(enrollments.map((item) => [item.id, item])), [enrollments]);

  const pay = async (enrollment) => {
    setPayingId(enrollment.id); setError(""); setNotice("");
    try {
      const response = await authApis().post(endpoints.payments, { enrollmentId: enrollment.id, method: methods[enrollment.id] || "MOMO" });
      const payment = apiData(response);
      if (!payment?.paymentUrl) throw new Error("Cổng thanh toán chưa trả về đường dẫn thanh toán");
      window.location.assign(payment.paymentUrl);
    } catch (requestError) { setError(apiError(requestError)); setPayingId(null); }
  };

  const cancel = async (enrollment) => {
    if (!window.confirm(`Bạn có chắc muốn hủy đăng ký lớp ${enrollment.className}?`)) return;
    setError(""); setNotice("");
    try {
      await authApis().post(endpoints["cancel-enrollment"](enrollment.id));
      setNotice("Hủy đăng ký thành công."); await loadHistory();
    } catch (requestError) { setError(apiError(requestError)); }
  };

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "STUDENT") return <Navigate to="/" replace />;

  return <section className="student-learning-page">
    <div className="student-learning-hero"><div className="public-container"><span className="section-kicker">TÀI KHOẢN HỌC VIÊN</span><h1>Lịch sử đăng ký & thanh toán</h1><p>Xem trạng thái đăng ký, thanh toán các yêu cầu đã xác nhận và kiểm tra lịch sử giao dịch.</p></div></div>
    <div className="public-container student-learning-content enrollment-history-page">
      {error && <div className="public-alert">{error}</div>}{notice && <div className="public-alert public-alert-success">{notice}</div>}{loading && <div className="learning-loading">Đang tải lịch sử...</div>}
      {!loading && <>
        <HistorySection kicker="ĐĂNG KÝ" title="Lịch sử đăng ký lớp học" count={`${enrollments.length} đăng ký`}>
          <table><thead><tr><th>Ngày đăng ký</th><th>Lớp học</th><th>Học phí</th><th>Đăng ký</th><th>Thanh toán</th><th>Giao dịch gần nhất</th><th>Thao tác</th></tr></thead><tbody>
            {enrollments.length ? enrollments.map((item) => {
              const transaction = latestPayment.get(item.id);
              const canPay = item.enrollmentStatus === "CONFIRMED" && item.paymentStatus === "PENDING" && Number(item.amountDue) > 0;
              const canCancel = item.enrollmentStatus !== "CANCELLED" && item.paymentStatus !== "PAID";
              return <tr key={item.id}><td>{formatDate(item.enrollmentDate)}</td><td><strong>{item.className}</strong><small>{item.classCode} · {item.courseName}</small></td><td>{formatMoney(item.amountDue)}</td><td><Status value={item.enrollmentStatus} /></td><td><Status value={item.paymentStatus} /></td><td>{transaction ? <><strong>{transaction.method}</strong><small>{transaction.transactionCode}<br />{labels[transaction.status] || transaction.status}</small></> : "Chưa có"}</td><td><div className="student-enrollment-actions">
                {canPay && <><select value={methods[item.id] || "MOMO"} onChange={(event) => setMethods((current) => ({ ...current, [item.id]: event.target.value }))}><option value="MOMO">MoMo</option><option value="ZALOPAY">ZaloPay</option></select><button className="pay-enrollment-button" disabled={payingId === item.id} onClick={() => pay(item)}>{payingId === item.id ? "Đang tạo..." : "Thanh toán"}</button></>}
                {item.enrollmentStatus === "PENDING" && <small>Chờ Staff xác nhận để thanh toán.</small>}{canCancel && <button type="button" onClick={() => cancel(item)}>Hủy đăng ký</button>}
              </div></td></tr>;
            }) : <tr><td colSpan="7">Bạn chưa có đăng ký lớp học nào.</td></tr>}
          </tbody></table>
        </HistorySection>
        <HistorySection kicker="GIAO DỊCH" title="Lịch sử thanh toán" count={`${payments.length} giao dịch`}>
          <table><thead><tr><th>Ngày tạo</th><th>Mã giao dịch</th><th>Lớp học</th><th>Phương thức</th><th>Số tiền</th><th>Trạng thái</th><th>Hoàn tất</th></tr></thead><tbody>
            {payments.length ? payments.map((payment) => { const enrollment = enrollmentById.get(payment.enrollmentId); return <tr key={payment.id}><td>{formatDate(payment.createdAt)}</td><td><strong>{payment.transactionCode}</strong></td><td>{enrollment ? <><strong>{enrollment.className}</strong><small>{enrollment.classCode}</small></> : `Đăng ký #${payment.enrollmentId}`}</td><td>{payment.method}</td><td>{formatMoney(payment.amount)}</td><td><Status value={payment.status} /></td><td>{payment.completedAt ? formatDate(payment.completedAt) : "—"}</td></tr>; }) : <tr><td colSpan="7">Chưa có giao dịch thanh toán.</td></tr>}
          </tbody></table>
        </HistorySection>
      </>}
    </div>
  </section>;
}

function HistorySection({ kicker, title, count, children }) {
  return <section className="enrollment-history"><div className="history-section-heading"><div><span className="section-kicker">{kicker}</span><h2>{title}</h2></div><strong>{count}</strong></div><div className="history-table-wrap">{children}</div></section>;
}

function Status({ value }) { return <span className={`learning-status status-${String(value).toLowerCase()}`}>{labels[value] || value}</span>; }
