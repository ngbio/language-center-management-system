import { useCallback, useEffect, useState } from "react";
import { authApis, endpoints } from "../../../configs/Apis";
import { EmptyState, ErrorAlert, LoadingRows, PageTitle, StatusBadge } from "../../../components/AdminUi";
import { apiData, apiError, formatDateTime, formatMoney } from "../../../utils/api";

const STATUSES = ["", "PENDING", "COMPLETED", "FAILED", "CANCELLED"];

export default function RefundManagementScreen() {
  const [status, setStatus] = useState("");
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshingId, setRefreshingId] = useState(null);
  const [error, setError] = useState("");

  const loadRefunds = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const response = await authApis().get(endpoints["staff-refunds"], {
        params: status ? { status } : {},
      });
      setRefunds(apiData(response) || []);
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => { loadRefunds(); }, [loadRefunds]);

  const refresh = async (refund) => {
    setRefreshingId(refund.id); setError("");
    try {
      await authApis().post(endpoints["staff-refresh-refund"](refund.id));
      await loadRefunds();
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setRefreshingId(null);
    }
  };

  return <>
    <PageTitle eyebrow="THANH TOÁN" title="Quản lý hoàn tiền" description="Theo dõi và đối soát yêu cầu hoàn tiền MoMo/ZaloPay" />
    <ErrorAlert message={error} />
    <section className="panel enrollment-admin-controls">
      <label>Trạng thái<select value={status} onChange={(event) => setStatus(event.target.value)}>{STATUSES.map((value) => <option key={value || "ALL"} value={value}>{value || "Tất cả trạng thái"}</option>)}</select></label>
      <button type="button" onClick={loadRefunds} disabled={loading}>Tải lại</button>
    </section>
    <section className="panel table-panel"><div className="table-wrap"><table><thead><tr><th>Mã hoàn</th><th>Học viên / lớp</th><th>Cổng</th><th>Số tiền</th><th>Trạng thái</th><th>Thời gian</th><th>Kết quả</th><th>Thao tác</th></tr></thead><tbody>
      {loading ? <LoadingRows columns={8} /> : refunds.map((item) => <tr key={item.id}><td><strong>{item.refundCode}</strong><small>Enrollment #{item.enrollmentId}</small></td><td><strong>{item.studentName}</strong><small>{item.className}</small></td><td>{item.paymentMethod}</td><td>{formatMoney(item.amount)}</td><td><StatusBadge value={item.status} /></td><td>{formatDateTime(item.completedAt || item.createdAt)}</td><td>{item.gatewayRefundId && <small>Mã cổng: {item.gatewayRefundId}</small>}{item.errorMessage && <small>{item.errorMessage}</small>}{!item.gatewayRefundId && !item.errorMessage && "—"}</td><td>{item.status === "PENDING" ? <button disabled={refreshingId === item.id} onClick={() => refresh(item)}>{refreshingId === item.id ? "Đang kiểm tra..." : "Đồng bộ"}</button> : "—"}</td></tr>)}
    </tbody></table></div>{!loading && !refunds.length && <EmptyState message="Chưa có yêu cầu hoàn tiền phù hợp" />}</section>
  </>;
}
