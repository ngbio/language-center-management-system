import { useCallback, useEffect, useState } from "react";
import { EmptyState, ErrorAlert, LoadingRows, PageTitle, StatusBadge } from "../../components/AdminUi";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDateTime } from "../../utils/api";

const emptyResult = { content: [], page: 0, totalPages: 0, totalElements: 0 };

export default function SystemLogsScreen() {
  const [result, setResult] = useState(emptyResult);
  const [filters, setFilters] = useState({ level: "", eventType: "", requestId: "", from: "", to: "" });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const load = useCallback(async (page = 0) => {
    setLoading(true); setError("");
    try {
      const response = await authApis().get(endpoints["admin-system-logs"], { params: {
        level: filters.level || undefined, eventType: filters.eventType || undefined,
        requestId: filters.requestId.trim() || undefined, from: filters.from || undefined,
        to: filters.to || undefined, page, size: 20,
      }});
      setResult(apiData(response) || emptyResult);
    } catch (requestError) { setError(apiError(requestError)); }
    finally { setLoading(false); }
  }, [filters]);
  useEffect(() => {
    // Fetch lại nhật ký khi bộ lọc thay đổi.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(0);
  }, [load]);

  return <>
    <PageTitle eyebrow="GIÁM SÁT" title="Nhật ký hệ thống" description={`${result.totalElements || 0} sự kiện lỗi được ghi nhận`} />
    <ErrorAlert message={error} />
    <section className="panel table-panel">
      <div className="toolbar system-log-filters">
        <select value={filters.level} onChange={(e) => setFilters({...filters, level:e.target.value})}>
          <option value="">Tất cả mức độ</option><option>ERROR</option><option>WARN</option>
        </select>
        <select value={filters.eventType} onChange={(e) => setFilters({...filters, eventType:e.target.value})}>
          <option value="">Tất cả sự kiện</option><option>AUTHENTICATION_FAILURE</option><option>HTTP_REQUEST_FAILED</option>
        </select>
        <input aria-label="Request ID" placeholder="Tìm theo request ID" value={filters.requestId} onChange={(e) => setFilters({...filters, requestId:e.target.value})} />
        <label>Từ<input type="datetime-local" value={filters.from} onChange={(e) => setFilters({...filters, from:e.target.value})} /></label>
        <label>Đến<input type="datetime-local" value={filters.to} onChange={(e) => setFilters({...filters, to:e.target.value})} /></label>
      </div>
      <div className="table-wrap"><table>
        <thead><tr><th>Thời gian</th><th>Mức độ</th><th>Sự kiện</th><th>Request</th><th>Thông báo</th><th>Người dùng</th></tr></thead>
        <tbody>{loading ? <LoadingRows columns={6}/> : result.content.map((entry) => <tr key={entry.id}>
          <td>{formatDateTime(entry.createdAt)}</td><td><StatusBadge value={entry.level}/></td><td><strong className="regular">{entry.eventType}</strong></td>
          <td><strong className="regular">{entry.httpMethod} {entry.requestPath}</strong><small>HTTP {entry.httpStatus} · {entry.requestId}</small></td>
          <td>{entry.message}</td><td>{entry.actorEmail || "Ẩn danh"}</td>
        </tr>)}</tbody>
      </table>{!loading && !result.content.length && <EmptyState message="Chưa ghi nhận sự kiện lỗi"/>}</div>
      <div className="pagination"><span>Trang {result.page + 1} / {Math.max(result.totalPages,1)}</span><div>
        <button disabled={result.page===0} onClick={() => load(result.page-1)}>←</button><button disabled={result.page+1>=result.totalPages} onClick={() => load(result.page+1)}>→</button>
      </div></div>
    </section>
  </>;
}
