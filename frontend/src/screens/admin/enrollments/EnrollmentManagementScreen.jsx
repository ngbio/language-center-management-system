import { useCallback, useEffect, useMemo, useState } from "react";
import { authApis, endpoints } from "../../../configs/Apis";
import { EmptyState, ErrorAlert, LoadingRows, Modal, PageTitle, StatusBadge } from "../../../components/AdminUi";
import { apiData, apiError, formatDate, formatDateTime, formatMoney } from "../../../utils/api";
import useDebouncedValue from "../../../hooks/useDebouncedValue";

export default function EnrollmentManagementScreen() {
  const [classes, setClasses] = useState([]);
  const [courseId, setCourseId] = useState("");
  const [classId, setClassId] = useState("");
  const [result, setResult] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebouncedValue(keyword);
  const [enrollmentStatus, setEnrollmentStatus] = useState("");
  const [paymentStatus, setPaymentStatus] = useState("");
  const [details, setDetails] = useState(null);
  const [studentEmail, setStudentEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [transferEnrollment, setTransferEnrollment] = useState(null);
  const [targetClassId, setTargetClassId] = useState("");

  const selectedClass = useMemo(
    () => classes.find((item) => String(item.id) === String(classId)),
    [classes, classId],
  );

  const courses = useMemo(() => {
    const unique = new Map();
    classes.forEach((item) => {
      if (!unique.has(item.courseId)) {
        unique.set(item.courseId, { id: item.courseId, code: item.courseCode, name: item.courseName });
      }
    });
    return [...unique.values()].sort((left, right) => left.name.localeCompare(right.name, "vi"));
  }, [classes]);

  const filteredClasses = useMemo(
    () => courseId ? classes.filter((item) => String(item.courseId) === courseId) : classes,
    [classes, courseId],
  );

  const loadEnrollments = useCallback(async (selectedId, page = 0) => {
    setLoading(true);
    setError("");
    try {
      const response = await authApis().get(endpoints["staff-enrollments"], { params: {
        keyword: debouncedKeyword || undefined, courseId: courseId || undefined,
        classId: selectedId || undefined, enrollmentStatus: enrollmentStatus || undefined,
        paymentStatus: paymentStatus || undefined, page, size: 20,
        sort: "enrollmentDate", direction: "desc",
      }});
      setResult(apiData(response) || { content: [], page: 0, totalPages: 0, totalElements: 0 });
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  }, [debouncedKeyword, courseId, enrollmentStatus, paymentStatus]);

  useEffect(() => {
    let active = true;
    authApis().get(endpoints["admin-classes"], { params: { page: 0, size: 100, sort: "startDate", direction: "desc" } })
      .then((response) => {
        if (!active) return;
        const values = apiData(response)?.content || [];
        setClasses(values);
        setLoading(false);
      })
      .catch((requestError) => { if (active) { setError(apiError(requestError)); setLoading(false); } });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    // Tải lại enrollment từ backend mỗi khi Admin chọn một lớp khác.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadEnrollments(classId, 0);
  }, [classId, loadEnrollments]);

  const perform = async (request, message) => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      await request();
      setNotice(message);
      await loadEnrollments(classId, result.page);
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setSaving(false);
    }
  };

  const assignStudent = (event) => {
    event.preventDefault();
    perform(
      () => authApis().post(endpoints["staff-enrollments"], { courseClassId: Number(classId), studentEmail: studentEmail.trim() }),
      "Đã xếp học viên vào lớp.",
    ).then(() => setStudentEmail(""));
  };

  const changeCourse = (value) => {
    setCourseId(value);
    setClassId("");
  };

  const openDetails = async (id) => {
    setError("");
    try {
      const response = await authApis().get(endpoints["staff-enrollment-details"](id));
      setDetails(apiData(response));
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const changeStatus = (item, status) => perform(
    () => authApis().patch(endpoints["change-enrollment-status"](item.id), { status }),
    `Đã chuyển trạng thái đăng ký sang ${status}.`,
  );

  const openTransferForm = (item) => {
    const candidates = classes.filter((target) => target.id !== item.courseClassId && target.courseId === item.courseId && target.status === "OPEN");
    setTransferEnrollment(item);
    setTargetClassId(candidates.length ? String(candidates[0].id) : "");
  };

  const transferCandidates = transferEnrollment
    ? classes.filter((target) => target.id !== transferEnrollment.courseClassId
      && target.courseId === transferEnrollment.courseId
      && target.status === "OPEN")
    : [];

  const transfer = (event) => {
    event.preventDefault();
    if (!transferEnrollment || !targetClassId) return;
    perform(
      () => authApis().post(endpoints["transfer-enrollment"](transferEnrollment.id), { targetCourseClassId: Number(targetClassId) }),
      "Đã chuyển lớp cho học viên.",
    ).then(() => {
      setTransferEnrollment(null);
      setTargetClassId("");
    });
  };

  const refund = async (item) => {
    const reason = window.prompt(`Nhập lý do hoàn toàn bộ học phí cho ${item.studentName}:`);
    if (!reason?.trim()) return;
    const idempotencyKey = globalThis.crypto?.randomUUID?.()
      || `refund-${item.id}-${Date.now()}`;
    setSaving(true); setError(""); setNotice("");
    try {
      const response = await authApis().post(endpoints["staff-refund"](item.id), {
        amount: null,
        reason: reason.trim(),
        idempotencyKey,
      });
      const refundResult = apiData(response);
      if (refundResult.status === "COMPLETED") {
        setNotice("Cổng thanh toán đã xác nhận hoàn tiền. Quyền truy cập lớp đã được cập nhật.");
      } else if (refundResult.status === "PENDING") {
        setNotice("Yêu cầu hoàn tiền đang được cổng thanh toán xử lý.");
      } else {
        setError(refundResult.errorMessage || "Cổng thanh toán từ chối yêu cầu hoàn tiền.");
      }
      await loadEnrollments(classId, result.page);
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setSaving(false);
    }
  };

  return <>
    <PageTitle eyebrow="TUYỂN SINH & XẾP LỚP" title="Quản lý đăng ký học" description="Xếp lớp, duyệt, hủy hoặc chuyển lớp cho học viên" />
    <ErrorAlert message={error} />
    {notice && <div className="alert enrollment-success">{notice}</div>}
    <section className="panel enrollment-admin-controls">
      <label>Lọc theo khóa học<select value={courseId} onChange={(event) => changeCourse(event.target.value)}><option value="">Tất cả khóa học</option>{courses.map((item) => <option key={item.id} value={item.id}>{item.code} · {item.name}</option>)}</select></label>
      <label>Chọn lớp học<select value={classId} onChange={(event) => setClassId(event.target.value)}><option value="">Chọn lớp</option>{filteredClasses.map((item) => <option key={item.id} value={item.id}>{item.classCode} · {item.className}</option>)}</select></label>
      <form onSubmit={assignStudent}><label>Email tài khoản học viên<input type="email" value={studentEmail} onChange={(event) => setStudentEmail(event.target.value)} placeholder="student@example.com" autoComplete="off" required /></label><button className="primary-button" disabled={!classId || !studentEmail.trim() || saving}>＋ Xếp vào lớp</button></form>
      {selectedClass && <p>{selectedClass.courseName} · {formatDate(selectedClass.startDate)} · {selectedClass.availableSeats} chỗ còn lại</p>}
    </section>
    <section className="panel table-panel">
      <div className="toolbar">
        <div className="search-box">⌕<input placeholder="Tìm học viên, email, mã lớp..." value={keyword} onChange={(event) => setKeyword(event.target.value)} /></div>
        <select value={enrollmentStatus} onChange={(event) => setEnrollmentStatus(event.target.value)}><option value="">Tất cả đăng ký</option><option>PENDING</option><option>CONFIRMED</option><option>CANCELLED</option></select>
        <select value={paymentStatus} onChange={(event) => setPaymentStatus(event.target.value)}><option value="">Tất cả thanh toán</option><option>PENDING</option><option>PAID</option><option>FAILED</option><option>CANCELLED</option><option>REFUNDED</option></select>
        <span className="record-count">{result.totalElements} đăng ký</span>
      </div>
      <div className="table-wrap"><table><thead><tr><th>Học viên</th><th>Lớp học</th><th>Ngày đăng ký</th><th>Hạn thanh toán</th><th>Học phí</th><th>Đăng ký</th><th>Thanh toán</th><th>Thao tác</th></tr></thead><tbody>
        {loading ? <LoadingRows columns={8} /> : result.content.map((item) => <tr key={item.id}><td><strong>{item.studentName}</strong><small>{item.studentCode} · ID {item.studentId}</small></td><td><strong className="regular">{item.className}</strong><small>{item.classCode}</small></td><td>{formatDate(item.enrollmentDate)}</td><td>{item.paymentStatus === "PENDING" ? formatDateTime(item.paymentDeadline) : "—"}</td><td>{formatMoney(item.amountDue)}</td><td><StatusBadge value={item.enrollmentStatus} /></td><td><StatusBadge value={item.paymentStatus} /></td><td><div className="enrollment-row-actions"><button onClick={() => openDetails(item.id)}>Chi tiết</button>{item.enrollmentStatus !== "CANCELLED" && item.paymentStatus !== "PAID" && <button disabled={saving} onClick={() => changeStatus(item, "CANCELLED")}>Hủy</button>}{item.enrollmentStatus === "CONFIRMED" && item.paymentStatus === "PENDING" && <button disabled={saving} onClick={() => openTransferForm(item)}>Chuyển lớp</button>}{item.paymentStatus === "PAID" && Number(item.amountDue) > 0 && <button disabled={saving} onClick={() => refund(item)}>Hoàn tiền</button>}</div></td></tr>)}
      </tbody></table></div>
      {!loading && !result.content.length && <EmptyState message="Không có đăng ký phù hợp" />}
      <div className="pagination"><span>Trang {result.page + 1} / {Math.max(result.totalPages, 1)}</span><div><button disabled={result.page === 0} onClick={() => loadEnrollments(classId, result.page - 1)}>←</button><button disabled={result.page + 1 >= result.totalPages} onClick={() => loadEnrollments(classId, result.page + 1)}>→</button></div></div>
    </section>
    {details && <Modal title={`Chi tiết đăng ký #${details.id}`} onClose={() => setDetails(null)}><div className="detail-grid"><span>Học viên<strong>{details.studentName}</strong></span><span>Email<strong>{details.studentEmail}</strong></span><span>Mã học viên<strong>{details.studentCode}</strong></span><span>Lớp<strong>{details.classCode} · {details.className}</strong></span><span>Khóa học<strong>{details.courseCode} · {details.courseName}</strong></span><span>Học phí<strong>{formatMoney(details.amountDue)}</strong></span><span>Đăng ký<strong><StatusBadge value={details.enrollmentStatus} /></strong></span><span>Thanh toán<strong><StatusBadge value={details.paymentStatus} /></strong></span><span>Ngày đăng ký<strong>{formatDateTime(details.enrollmentDate)}</strong></span><span>Hạn thanh toán<strong>{formatDateTime(details.paymentDeadline)}</strong></span>{details.cancellationReason && <span className="field-wide">Lý do hủy<strong>{details.cancellationReason}</strong></span>}</div></Modal>}
    {transferEnrollment && <div className="modal-backdrop" role="presentation" onMouseDown={() => !saving && setTransferEnrollment(null)}>
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="transfer-title" onMouseDown={(event) => event.stopPropagation()}>
        <header><h2 id="transfer-title">Chuyển lớp học</h2><button type="button" className="icon-button" aria-label="Đóng" disabled={saving} onClick={() => setTransferEnrollment(null)}>×</button></header>
        <form onSubmit={transfer}>
          <div className="form-grid">
            <label>Học viên<input value={`${transferEnrollment.studentName} · ${transferEnrollment.studentCode}`} disabled /></label>
            <label>Khóa học<input value={transferEnrollment.courseName} disabled /></label>
            <label className="field-wide">Lớp hiện tại<input value={`${transferEnrollment.classCode} · ${transferEnrollment.className}`} disabled /></label>
            <label className="field-wide">Chọn lớp mới<select value={targetClassId} onChange={(event) => setTargetClassId(event.target.value)} required><option value="">Không có lớp phù hợp</option>{transferCandidates.map((target) => <option key={target.id} value={target.id}>{target.classCode} · {target.className} · khai giảng {formatDate(target.startDate)} · còn {target.availableSeats} chỗ</option>)}</select></label>
          </div>
          {!transferCandidates.length && <div className="alert info">Khóa học này hiện không có lớp OPEN khác để chuyển.</div>}
          <div className="modal-actions"><button type="button" disabled={saving} onClick={() => setTransferEnrollment(null)}>Hủy</button><button type="submit" className="primary-button" disabled={saving || !targetClassId}>Xác nhận chuyển lớp</button></div>
        </form>
      </section>
    </div>}
  </>;
}
