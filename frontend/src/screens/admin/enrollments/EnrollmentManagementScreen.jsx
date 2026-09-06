import { useEffect, useMemo, useState } from "react";
import { authApis, endpoints } from "../../../configs/Apis";
import { EmptyState, ErrorAlert, LoadingRows, PageTitle, StatusBadge } from "../../../components/AdminUi";
import { apiData, apiError, formatDate, formatMoney } from "../../../utils/api";

export default function EnrollmentManagementScreen() {
  const [classes, setClasses] = useState([]);
  const [classId, setClassId] = useState("");
  const [enrollments, setEnrollments] = useState([]);
  const [studentId, setStudentId] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const selectedClass = useMemo(
    () => classes.find((item) => String(item.id) === String(classId)),
    [classes, classId],
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
      () => authApis().post(endpoints["staff-enrollments"], { courseClassId: Number(classId), studentId: Number(studentId) }),
      "Đã xếp học viên vào lớp.",
    ).then(() => setStudentId(""));
  };

  const changeStatus = (item, status) => perform(
    () => authApis().patch(endpoints["change-enrollment-status"](item.id), { status }),
    `Đã chuyển trạng thái đăng ký sang ${status}.`,
  );

  const transfer = (item) => {
    const candidates = classes.filter((target) => target.id !== item.courseClassId && target.courseId === item.courseId && target.status === "OPEN");
    const description = candidates.map((target) => `${target.id}: ${target.className} (${target.classCode})`).join("\n");
    const targetId = window.prompt(`Nhập ID lớp muốn chuyển đến:\n${description}`);
    if (!targetId) return;
    perform(
      () => authApis().post(endpoints["transfer-enrollment"](item.id), { targetCourseClassId: Number(targetId) }),
      "Đã chuyển lớp cho học viên.",
    );
  };

  return <>
    <PageTitle eyebrow="TUYỂN SINH & XẾP LỚP" title="Quản lý đăng ký học" description="Xếp lớp, duyệt, hủy hoặc chuyển lớp cho học viên" />
    <ErrorAlert message={error} />
    {notice && <div className="alert enrollment-success">{notice}</div>}
    <section className="panel enrollment-admin-controls">
      <label>Chọn lớp học<select value={classId} onChange={(event) => setClassId(event.target.value)}><option value="">Chọn lớp</option>{classes.map((item) => <option key={item.id} value={item.id}>{item.classCode} · {item.className}</option>)}</select></label>
      <form onSubmit={assignStudent}><label>ID học viên<input type="number" min="1" value={studentId} onChange={(event) => setStudentId(event.target.value)} placeholder="Ví dụ: 1" required /></label><button className="primary-button" disabled={!classId || saving}>＋ Xếp vào lớp</button></form>
      {selectedClass && <p>{selectedClass.courseName} · {formatDate(selectedClass.startDate)} · {selectedClass.availableSeats} chỗ còn lại</p>}
    </section>
    <section className="panel table-panel"><div className="table-wrap"><table><thead><tr><th>Học viên</th><th>Lớp học</th><th>Ngày đăng ký</th><th>Học phí</th><th>Đăng ký</th><th>Thanh toán</th><th>Thao tác</th></tr></thead><tbody>
      {loading ? <LoadingRows columns={7} /> : enrollments.map((item) => <tr key={item.id}><td><strong>{item.studentName}</strong><small>{item.studentCode} · ID {item.studentId}</small></td><td><strong className="regular">{item.className}</strong><small>{item.classCode}</small></td><td>{formatDate(item.enrollmentDate)}</td><td>{formatMoney(item.amountDue)}</td><td><StatusBadge value={item.enrollmentStatus} /></td><td><StatusBadge value={item.paymentStatus} /></td><td><div className="enrollment-row-actions">{item.enrollmentStatus === "PENDING" && <button disabled={saving} onClick={() => changeStatus(item, "CONFIRMED")}>Xác nhận</button>}{item.enrollmentStatus !== "CANCELLED" && item.paymentStatus !== "PAID" && <button disabled={saving} onClick={() => changeStatus(item, "CANCELLED")}>Hủy</button>}{item.enrollmentStatus === "PENDING" && item.paymentStatus === "PENDING" && <button disabled={saving} onClick={() => transfer(item)}>Chuyển lớp</button>}</div></td></tr>)}
    </tbody></table></div>{!loading && !enrollments.length && <EmptyState message={classId ? "Lớp chưa có đăng ký" : "Hãy chọn một lớp học"} />}</section>
  </>;
}
