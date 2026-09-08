import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { ErrorAlert, PageTitle } from "../../components/AdminUi";
import { apiData, apiError } from "../../utils/api";

const isoDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const initialRange = () => {
  const now = new Date();
  return {
    from: isoDate(new Date(now.getFullYear(), now.getMonth() - 11, 1)),
    to: isoDate(now),
  };
};

const money = (value) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 })
    .format(Number(value || 0));
const shortMoney = (value) => new Intl.NumberFormat("vi-VN", { notation: "compact" }).format(Number(value || 0));
const dateLabel = (value) => value ? new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN") : "—";
const monthLabel = (value) => {
  const normalized = Array.isArray(value) ? `${value[0]}-${String(value[1]).padStart(2, "0")}` : value;
  const [year, month] = String(normalized).split("-");
  return month && year ? `T${Number(month)}/${year}` : normalized;
};

export default function DashboardScreen() {
  const [range, setRange] = useState(initialRange);
  const [appliedRange, setAppliedRange] = useState(initialRange);
  const [summary, setSummary] = useState(null);
  const [reports, setReports] = useState({ revenue: [], enrollments: [], popular: [], teachers: [], upcoming: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const api = authApis();
        const params = appliedRange;
        const today = isoDate(new Date());
        const upcomingTo = new Date();
        upcomingTo.setDate(upcomingTo.getDate() + 30);
        const [summaryResult, revenue, enrollments, popular, teachers, upcoming] = await Promise.all([
          api.get(endpoints["admin-dashboard-summary"]),
          api.get(endpoints["admin-report-revenue"], { params }),
          api.get(endpoints["admin-report-enrollments"], { params }),
          api.get(endpoints["admin-report-popular-courses"], { params: { ...params, limit: 5 } }),
          api.get(endpoints["admin-report-teacher-load"], { params }),
          api.get(endpoints["admin-report-upcoming-classes"], { params: { from: today, to: isoDate(upcomingTo) } }),
        ]);
        setSummary(apiData(summaryResult));
        setReports({
          revenue: apiData(revenue) || [], enrollments: apiData(enrollments) || [],
          popular: apiData(popular) || [], teachers: apiData(teachers) || [], upcoming: apiData(upcoming) || [],
        });
      } catch (requestError) {
        setError(apiError(requestError));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [appliedRange]);

  const revenueMax = useMemo(() => Math.max(1, ...reports.revenue.map((item) => Number(item.grossRevenue || 0))), [reports.revenue]);
  const cards = [
    ["Học viên", summary?.totalStudents, "/admin/users", "Tổng hồ sơ học viên"],
    ["Giảng viên", summary?.totalTeachers, "/admin/users", "Đội ngũ giảng dạy"],
    ["Lớp đang hoạt động", summary?.activeClasses, "/admin/classes", "Đang mở hoặc đang học"],
    ["Doanh thu thuần", summary ? money(summary.netRevenue) : null, "/admin/enrollments", "Đã thu trừ hoàn tiền"],
  ];

  const applyFilter = (event) => {
    event.preventDefault();
    if (range.from > range.to) return setError("Ngày bắt đầu không được sau ngày kết thúc");
    setAppliedRange({ ...range });
  };

  return (
    <>
      <PageTitle eyebrow="TỔNG QUAN" title="Dashboard vận hành"
        description="Theo dõi đăng ký, doanh thu, lớp sắp khai giảng và tải giảng dạy từ dữ liệu thực tế." />
      <ErrorAlert message={error} />

      <section className="stat-grid dashboard-stat-grid">
        {cards.map(([label, value, to, note]) => (
          <Link className="stat-card" to={to} key={label}>
            <div><span>{label}</span><strong>{value ?? (loading ? "…" : "0")}</strong><small>{note}</small></div><b>↗</b>
          </Link>
        ))}
      </section>

      <form className="panel report-filter" onSubmit={applyFilter}>
        <div><label htmlFor="report-from">Từ ngày</label><input id="report-from" type="date" value={range.from}
          onChange={(event) => setRange((current) => ({ ...current, from: event.target.value }))} /></div>
        <div><label htmlFor="report-to">Đến ngày</label><input id="report-to" type="date" value={range.to}
          onChange={(event) => setRange((current) => ({ ...current, to: event.target.value }))} /></div>
        <button type="submit" disabled={loading}>{loading ? "Đang tải..." : "Áp dụng báo cáo"}</button>
        <p>Khoảng báo cáo tối đa 366 ngày. Lớp sắp khai giảng luôn tính trong 30 ngày tới.</p>
      </form>

      <section className="dashboard-report-grid">
        <article className="panel report-panel report-wide">
          <div className="panel-heading"><div><span className="eyebrow">TÀI CHÍNH</span><h2>Doanh thu theo tháng</h2></div>
            <strong>{money(summary?.netRevenue)}</strong></div>
          <div className="revenue-chart">
            {reports.revenue.map((item) => (
              <div className="revenue-column" key={String(item.month)} title={`Thu ${money(item.grossRevenue)} · Hoàn ${money(item.refundedAmount)}`}>
                <div className="revenue-bars"><i style={{ height: `${Math.max(3, Number(item.grossRevenue) / revenueMax * 100)}%` }} />
                  <i className="refund" style={{ height: `${Number(item.refundedAmount) / revenueMax * 100}%` }} /></div>
                <strong>{shortMoney(item.netRevenue)}</strong><span>{monthLabel(item.month)}</span>
              </div>
            ))}
          </div>
          <div className="report-legend"><span><i />Đã thanh toán</span><span><i className="refund" />Đã hoàn</span></div>
        </article>

        <article className="panel report-panel">
          <div className="panel-heading"><div><span className="eyebrow">ĐĂNG KÝ</span><h2>Theo tháng</h2></div></div>
          <div className="compact-table"><table><thead><tr><th>Tháng</th><th>Tổng</th><th>Đã trả</th><th>Đã hủy</th></tr></thead><tbody>
            {reports.enrollments.map((item) => <tr key={String(item.month)}><td>{monthLabel(item.month)}</td><td>{item.total}</td><td>{item.paid}</td><td>{item.cancelled}</td></tr>)}
          </tbody></table></div>
        </article>

        <article className="panel report-panel">
          <div className="panel-heading"><div><span className="eyebrow">NHU CẦU</span><h2>Khóa học phổ biến</h2></div></div>
          <div className="rank-list">{reports.popular.map((item, index) => <div key={item.courseId}><b>{index + 1}</b><span><strong>{item.courseName}</strong><small>{item.courseCode} · {item.paidEnrollments}/{item.registrations} đã thanh toán</small></span><em>{money(item.revenue)}</em></div>)}</div>
          {!loading && !reports.popular.length && <p className="empty-report">Chưa có đăng ký trong khoảng này.</p>}
        </article>

        <article className="panel report-panel report-wide">
          <div className="panel-heading"><div><span className="eyebrow">GIẢNG DẠY</span><h2>Tải giảng viên</h2></div></div>
          <div className="compact-table"><table><thead><tr><th>Giảng viên</th><th>Mã</th><th>Lớp phụ trách</th><th>Buổi học</th><th>Hoàn thành</th></tr></thead><tbody>
            {reports.teachers.map((item) => <tr key={item.teacherId}><td>{item.teacherName}</td><td>{item.teacherCode}</td><td>{item.classes}</td><td>{item.lessons}</td><td>{item.completedLessons}</td></tr>)}
          </tbody></table></div>
        </article>

        <article className="panel report-panel report-full">
          <div className="panel-heading"><div><span className="eyebrow">30 NGÀY TỚI</span><h2>Lớp sắp khai giảng</h2></div><strong>{summary?.upcomingClasses ?? 0} lớp</strong></div>
          <div className="compact-table"><table><thead><tr><th>Lớp</th><th>Khóa học</th><th>Giảng viên</th><th>Khai giảng</th><th>Giữ chỗ</th><th>Còn lại</th></tr></thead><tbody>
            {reports.upcoming.map((item) => <tr key={item.classId}><td><strong>{item.className}</strong><small>{item.classCode}</small></td><td>{item.courseName}</td><td>{item.teacherName || "Chưa phân công"}</td><td>{dateLabel(item.startDate)}</td><td>{item.reservedSeats}/{item.maxStudents}</td><td>{item.availableSeats}</td></tr>)}
          </tbody></table></div>
          {!loading && !reports.upcoming.length && <p className="empty-report">Không có lớp khai giảng trong 30 ngày tới.</p>}
        </article>
      </section>
    </>
  );
}
