import { useEffect, useMemo, useState } from "react";
import { authApis, endpoints } from "../../../configs/Apis";
import { EmptyState, ErrorAlert, LoadingRows, PageTitle, StatusBadge } from "../../../components/AdminUi";
import { apiData, apiError, formatDate, formatDateTime, formatMoney } from "../../../utils/api";

export default function EnrollmentManagementScreen() {
  const [classes, setClasses] = useState([]);
  const [courseId, setCourseId] = useState("");
  const [classId, setClassId] = useState("");
  const [enrollments, setEnrollments] = useState([]);
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

  const loadEnrollments = async (selectedId) => {
    if (!selectedId) {
      setEnrollments([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    try {
      const response = await authApis().get(endpoints["class-enrollments"](selectedId));
      setEnrollments(apiData(response) || []);
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    authApis().get(endpoints["admin-classes"], { params: { page: 0, size: 100, sort: "startDate", direction: "desc" } })
      .then((response) => {
        if (!active) return;
        const values = apiData(response)?.content || [];
        setClasses(values);
        if (values.length) setClassId(String(values[0].id));
        else setLoading(false);
      })
      .catch((requestError) => { if (active) { setError(apiError(requestError)); setLoading(false); } });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    // Tải lại enrollment từ backend mỗi khi Admin chọn một lớp khác.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadEnrollments(classId);
  }, [classId]);

  const perform = async (request, message) => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      await request();
      setNotice(message);
      await loadEnrollments(classId);
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
    const matchingClasses = value
      ? classes.filter((item) => String(item.courseId) === value)
      : classes;
    setClassId(matchingClasses.length ? String(matchingClasses[0].id) : "");
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

  const refund = (item) => {
    const reason = window.prompt(`Nhập lý do hoàn toàn bộ học phí cho ${item.studentName}:`);
    if (!reason?.trim()) return;
    const idempotencyKey = globalThis.crypto?.randomUUID?.()
      || `refund-${item.id}-${Date.now()}`;
    perform(
      () => authApis().post(endpoints["staff-refund"](item.id), {
        amount: null,
        reason: reason.trim(),
        idempotencyKey,
      }),
      "Đã ghi nhận hoàn tiền và hủy quyền truy cập lớp học.",
    );
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
    <section className="panel table-panel"><div className="table-wrap"><table><thead><tr><th>Học viên</th><th>Lớp học</th><th>Ngày đăng ký</th><th>Hạn thanh toán</th><th>Học phí</th><th>Đăng ký</th><th>Thanh toán</th><th>Thao tác</th></tr></thead><tbody>
      {loading ? <LoadingRows columns={8} /> : enrollments.map((item) => <tr key={item.id}><td><strong>{item.studentName}</strong><small>{item.studentCode} · ID {item.studentId}</small></td><td><strong className="regular">{item.className}</strong><small>{item.classCode}</small></td><td>{formatDate(item.enrollmentDate)}</td><td>{item.paymentStatus === "PENDING" ? formatDateTime(item.paymentDeadline) : "—"}</td><td>{formatMoney(item.amountDue)}</td><td><StatusBadge value={item.enrollmentStatus} /></td><td><StatusBadge value={item.paymentStatus} /></td><td><div className="enrollment-row-actions">{item.enrollmentStatus !== "CANCELLED" && item.paymentStatus !== "PAID" && <button disabled={saving} onClick={() => changeStatus(item, "CANCELLED")}>Hủy</button>}{item.enrollmentStatus === "CONFIRMED" && item.paymentStatus === "PENDING" && <button disabled={saving} onClick={() => openTransferForm(item)}>Chuyển lớp</button>}{item.paymentStatus === "PAID" && Number(item.amountDue) > 0 && <button disabled={saving} onClick={() => refund(item)}>Hoàn tiền</button>}</div></td></tr>)}
    </tbody></table></div>{!loading && !enrollments.length && <EmptyState message={classId ? "Lớp chưa có đăng ký" : "Hãy chọn một lớp học"} />}</section>
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
