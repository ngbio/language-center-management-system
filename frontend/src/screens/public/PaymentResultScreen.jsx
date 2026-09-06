import { useEffect, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate, formatMoney } from "../../utils/api";
import { isTokenActive, SESSION_KEYS } from "../../utils/authSession";

export default function PaymentResultScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [params] = useSearchParams();
  const [payments, setPayments] = useState([]);
  const [error, setError] = useState("");
  const provider = params.get("provider") || "Cổng thanh toán";

  useEffect(() => {
    if (!isTokenActive(token) || role !== "STUDENT") return undefined;
    let active = true;
    authApis().get(endpoints["my-payments"])
      .then((response) => { if (active) setPayments(apiData(response) || []); })
      .catch((requestError) => { if (active) setError(apiError(requestError)); });
    return () => { active = false; };
  }, [role, token]);

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "STUDENT") return <Navigate to="/" replace />;
  const latest = payments.find((item) => item.method === provider) || payments[0];

  return <section className="payment-result-page"><div className="public-container payment-result-card">
    <span className="payment-result-icon">✓</span>
    <span className="section-kicker">KẾT QUẢ THANH TOÁN</span>
    <h1>Đã quay lại từ {provider}</h1>
    <p>Kết quả chính thức được cập nhật bằng callback bảo mật từ cổng thanh toán. Nếu trạng thái còn chờ, hãy tải lại sau ít phút.</p>
    {error && <div className="public-alert">{error}</div>}
    {latest && <dl><div><dt>Mã giao dịch</dt><dd>{latest.transactionCode}</dd></div><div><dt>Số tiền</dt><dd>{formatMoney(latest.amount)}</dd></div><div><dt>Phương thức</dt><dd>{latest.method}</dd></div><div><dt>Trạng thái</dt><dd>{latest.status}</dd></div><div><dt>Ngày tạo</dt><dd>{formatDate(latest.createdAt)}</dd></div></dl>}
    <div><Link className="primary-cta" to="/lich-su-dang-ky">Xem lịch sử đăng ký & thanh toán</Link><button type="button" className="text-cta" onClick={() => window.location.reload()}>Tải lại trạng thái</button></div>
  </div></section>;
}
