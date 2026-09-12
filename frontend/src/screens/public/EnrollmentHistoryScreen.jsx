import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate, formatDateTime, formatMoney } from "../../utils/api";
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
  const [previewingId, setPreviewingId] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [methods, setMethods] = useState({});
  const [detail, setDetail] = useState(null);
  const [detailLoadingId, setDetailLoadingId] = useState(null);

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

  // eslint-disable-next-line react-hooks/set-state-in-effect
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
      const response = await authApis().post(
        endpoints["enrollment-payments"](enrollment.id),
        { method: methods[enrollment.id] || "MOMO" },
      );
      const payment = apiData(response);
      if (!payment?.paymentUrl) throw new Error("Cổng thanh toán chưa trả về đường dẫn thanh toán");
      window.location.assign(payment.paymentUrl);
    } catch (requestError) { setError(apiError(requestError)); setPayingId(null); }
  };

  const cancel = async (enrollment) => {
    if (!window.confirm(`Bạn có chắc muốn hủy đăng ký lớp ${enrollment.className}?`)) return;
    const cancellationReason = window.prompt("Vui lòng nhập lý do hủy đăng ký:");
    if (cancellationReason === null) return;
    if (!cancellationReason.trim()) {
      setError("Lý do hủy đăng ký không được để trống.");
      return;
    }
    setError(""); setNotice("");
    try {
      await authApis().post(endpoints["cancel-enrollment"](enrollment.id), {
        cancellationReason: cancellationReason.trim(),
      });
      setNotice("Hủy đăng ký thành công."); await loadHistory();
    } catch (requestError) { setError(apiError(requestError)); }
  };

  const setInvoiceRequestError = async (requestError) => {
    const responseBody = requestError.response?.data;
    if (responseBody instanceof Blob) {
      try {
        const payload = JSON.parse(await responseBody.text());
        setError(payload.message || apiError(requestError));
        return;
      } catch {
        // The response is not a JSON API error; use the shared fallback below.
      }
    }
    setError(apiError(requestError));
  };

  const viewInvoice = async (enrollment) => {
    const previewWindow = window.open("about:blank", "_blank");
    if (!previewWindow) {
      setError("Trình duyệt đang chặn cửa sổ xem hóa đơn. Vui lòng cho phép mở tab mới.");
      return;
    }
    previewWindow.opener = null;
    previewWindow.document.title = "Đang tải hóa đơn...";
    setPreviewingId(enrollment.id); setError(""); setNotice("");
    try {
      const response = await authApis().get(endpoints["enrollment-invoice-pdf"](enrollment.id), {
        params: { download: false },
        responseType: "blob",
      });
      const url = URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
      previewWindow.location.replace(url);
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (requestError) {
      previewWindow.close();
      await setInvoiceRequestError(requestError);
    } finally {
      setPreviewingId(null);
    }
  };

  const downloadInvoice = async (enrollment) => {
    setDownloadingId(enrollment.id); setError(""); setNotice("");
    try {
      const response = await authApis().get(endpoints["enrollment-invoice-pdf"](enrollment.id), {
        params: { download: true },
        responseType: "blob",
      });
      const url = URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `hoa-don-${enrollment.classCode}-${enrollment.studentCode}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (requestError) {
      await setInvoiceRequestError(requestError);
    } finally {
      setDownloadingId(null);
    }
  };

  const viewDetail = async (enrollment) => {
    setDetailLoadingId(enrollment.id); setError("");
    try {
      const api = authApis();
      const [paymentResponse, refundResponse, invoiceResponse] = await Promise.all([
        api.get(endpoints["enrollment-payments"](enrollment.id)),
        api.get(endpoints["enrollment-refunds"](enrollment.id)),
        api.get(endpoints["enrollment-invoice"](enrollment.id)),
      ]);
      setDetail({
        enrollment,
        payments: apiData(paymentResponse) || [],
        refunds: apiData(refundResponse) || [],
        invoice: apiData(invoiceResponse),
      });
    } catch (requestError) { setError(apiError(requestError)); }
    finally { setDetailLoadingId(null); }
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
              return <tr key={item.id}><td>{formatDate(item.enrollmentDate)}{item.paymentStatus === "PENDING" && <small>Hạn: {formatDateTime(item.paymentDeadline)}</small>}</td><td><strong>{item.className}</strong><small>{item.classCode} · {item.courseName}</small></td><td>{formatMoney(item.amountDue)}</td><td><Status value={item.enrollmentStatus} /></td><td><Status value={item.paymentStatus} /></td><td>{transaction ? <><strong>{transaction.method}</strong><small>{transaction.transactionCode}<br />{labels[transaction.status] || transaction.status}</small></> : "Chưa có"}</td><td><div className="student-enrollment-actions">
                {canPay && <><select value={methods[item.id] || "MOMO"} onChange={(event) => setMethods((current) => ({ ...current, [item.id]: event.target.value }))}><option value="MOMO">MoMo</option><option value="ZALOPAY">ZaloPay</option></select><button className="pay-enrollment-button" disabled={payingId === item.id} onClick={() => pay(item)}>{payingId === item.id ? "Đang tạo..." : "Thanh toán"}</button></>}
                {canCancel && <button type="button" onClick={() => cancel(item)}>Hủy đăng ký</button>}
                <button type="button" disabled={detailLoadingId === item.id} onClick={() => viewDetail(item)}>{detailLoadingId === item.id ? "Đang tải..." : "Xem chi tiết"}</button>
                {["PAID", "REFUNDED"].includes(item.paymentStatus) && <><button type="button" disabled={previewingId === item.id} onClick={() => viewInvoice(item)}>{previewingId === item.id ? "Đang mở..." : "Xem hóa đơn"}</button><button type="button" className="pay-enrollment-button" disabled={downloadingId === item.id} onClick={() => downloadInvoice(item)}>{downloadingId === item.id ? "Đang tải..." : "Tải hóa đơn"}</button></>}
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
    {detail && <BillingDetail detail={detail} onClose={() => setDetail(null)} />}
  </section>;
}

function HistorySection({ kicker, title, count, children }) {
  return <section className="enrollment-history"><div className="history-section-heading"><div><span className="section-kicker">{kicker}</span><h2>{title}</h2></div><strong>{count}</strong></div><div className="history-table-wrap">{children}</div></section>;
}

function Status({ value }) { return <span className={`learning-status status-${String(value).toLowerCase()}`}>{labels[value] || value}</span>; }

function BillingDetail({ detail, onClose }) {
  const { enrollment, invoice, payments: attempts, refunds } = detail;
  return <div className="public-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="public-enrollment-modal billing-detail-modal" role="dialog" aria-modal="true" aria-labelledby="billing-detail-title">
      <button className="public-modal-close" type="button" onClick={onClose}>×</button>
      <span className="section-kicker">CHI TIẾT TÀI CHÍNH</span><h2 id="billing-detail-title">{enrollment.className}</h2>
      {invoice ? <dl className="billing-summary"><div><dt>Số hóa đơn</dt><dd>{invoice.invoiceNumber}</dd></div><div><dt>Học phí</dt><dd>{formatMoney(invoice.tuitionAmount)}</dd></div><div><dt>Đã thanh toán</dt><dd>{formatMoney(invoice.paidAmount)}</dd></div><div><dt>Đã hoàn</dt><dd>{formatMoney(invoice.refundedAmount)}</dd></div><div><dt>Thực thu</dt><dd>{formatMoney(invoice.netPaidAmount)}</dd></div><div><dt>Trạng thái</dt><dd><Status value={invoice.paymentStatus} /></dd></div></dl> : <div className="public-empty">Dữ liệu hóa đơn đang được cập nhật.</div>}
      <h3>Các lần thử thanh toán</h3>
      <div className="history-table-wrap"><table><thead><tr><th>Mã giao dịch</th><th>Phương thức</th><th>Số tiền</th><th>Trạng thái</th><th>Thời gian</th></tr></thead><tbody>{attempts.length ? attempts.map((payment) => <tr key={payment.id}><td>{payment.transactionCode}</td><td>{payment.method}</td><td>{formatMoney(payment.amount)}</td><td><Status value={payment.status} /></td><td>{formatDateTime(payment.completedAt || payment.createdAt)}</td></tr>) : <tr><td colSpan="5">Chưa có lần thử thanh toán.</td></tr>}</tbody></table></div>
      <h3>Lịch sử hoàn tiền</h3>
      <div className="history-table-wrap"><table><thead><tr><th>Mã hoàn tiền</th><th>Số tiền</th><th>Lý do</th><th>Trạng thái</th><th>Thời gian</th></tr></thead><tbody>{refunds.length ? refunds.map((refund) => <tr key={refund.id}><td>{refund.refundCode}</td><td>{formatMoney(refund.amount)}</td><td>{refund.reason}</td><td><Status value={refund.status} /></td><td>{formatDateTime(refund.completedAt || refund.createdAt)}</td></tr>) : <tr><td colSpan="5">Chưa có yêu cầu hoàn tiền.</td></tr>}</tbody></table></div>
    </section>
  </div>;
}
