import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError, formatDate } from "../../utils/api";
import { isTokenActive, SESSION_KEYS } from "../../utils/authSession";
import "../../styles/Attendance.css";

const ATTENDANCE_OPTIONS = [
  ["PRESENT", "Có mặt"],
  ["ABSENT", "Vắng"],
  ["LATE", "Đi trễ"],
  ["EXCUSED", "Vắng có phép"],
];
const DAY_LABELS = ["", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy", "Chủ nhật"];

export default function TeacherAttendanceScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [classes, setClasses] = useState([]);
  const [classId, setClassId] = useState("");
  const [lessons, setLessons] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [selectedLesson, setSelectedLesson] = useState(null);
  const [sheet, setSheet] = useState(null);
  const [attendanceForm, setAttendanceForm] = useState({});
  const [editForm, setEditForm] = useState({ topic: "", meetingUrl: "" });
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [updatingAttendanceId, setUpdatingAttendanceId] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [attendanceFeedback, setAttendanceFeedback] = useState(null);

  useEffect(() => {
    if (!isTokenActive(token) || role !== "TEACHER") return undefined;
    let active = true;
    authApis().get(endpoints["teacher-classes"])
      .then((response) => {
        if (!active) return;
        const values = apiData(response) || [];
        setClasses(values);
        setClassId(values[0]?.id ? String(values[0].id) : "");
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [role, token]);

  useEffect(() => {
    if (!classId) return undefined;
    let active = true;
    authApis().get(endpoints["class-lessons"](classId))
      .then((lessonResponse) => {
        if (!active) return;
        setLessons(apiData(lessonResponse) || []);
        const selectedClass = classes.find((item) => String(item.id) === String(classId));
        setSchedules(selectedClass?.schedules || []);
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); });
    return () => { active = false; };
  }, [classId, classes]);

  const refreshLessons = async () => {
    const response = await authApis().get(endpoints["class-lessons"](classId));
    const values = apiData(response) || [];
    setLessons(values);
    if (selectedLesson) {
      const updated = values.find((item) => item.id === selectedLesson.id);
      if (updated) setSelectedLesson(updated);
    }
  };

  const generateLessons = async () => {
    setWorking(true); setError(""); setSuccess("");
    try {
      const response = await authApis().post(endpoints["generate-class-lessons"](classId));
      setLessons(apiData(response) || []);
      setSuccess("Đã sinh danh sách buổi học theo lịch cố định của lớp.");
    } catch (requestError) { setError(apiError(requestError)); }
    finally { setWorking(false); }
  };

  const chooseLesson = (lesson) => {
    setSelectedLesson(lesson);
    setSheet(null);
    setEditForm({ topic: lesson.topic || "", meetingUrl: lesson.meetingUrl || "" });
    setAttendanceFeedback(null);
    setError(""); setSuccess("");
  };

  const updateLesson = async (event) => {
    event.preventDefault(); setWorking(true); setError(""); setSuccess("");
    try {
      const response = await authApis().put(endpoints["lesson-details"](selectedLesson.id), editForm);
      setSelectedLesson(apiData(response));
      await refreshLessons();
      setSuccess("Đã cập nhật nội dung buổi học.");
    } catch (requestError) { setError(apiError(requestError)); }
    finally { setWorking(false); }
  };

  const openAttendance = async () => {
    setWorking(true); setError(""); setSuccess(""); setAttendanceFeedback(null);
    try {
      const response = await authApis().get(endpoints["lesson-attendance"](selectedLesson.id));
      const value = apiData(response);
      setSheet(value);
      setAttendanceForm(Object.fromEntries((value.students || []).map((student) => [student.studentId, { status: student.status || "", note: student.note || "" }])));
    } catch (requestError) {
      const message = apiError(requestError);
      setError(message);
      setAttendanceFeedback({ type: "error", message });
    }
    finally { setWorking(false); }
  };

  const changeAttendance = (studentId, field, value) => {
    setAttendanceForm((current) => ({ ...current, [studentId]: { ...current[studentId], [field]: value } }));
  };

  const saveAttendance = async (event) => {
    event.preventDefault();
    const missing = sheet.students.some((student) => !attendanceForm[student.studentId]?.status);
    if (missing) {
      const message = "Vui lòng chọn trạng thái cho toàn bộ học viên trước khi lưu.";
      setError(message);
      setAttendanceFeedback({ type: "error", message });
      return;
    }
    setWorking(true); setError(""); setSuccess(""); setAttendanceFeedback(null);
    try {
      const response = await authApis().put(endpoints["lesson-attendance"](selectedLesson.id), {
        attendances: sheet.students.map((student) => ({ studentId: student.studentId, ...attendanceForm[student.studentId] })),
      });
      const value = apiData(response);
      setSheet(value);
      await refreshLessons();
      const message = "Đã lưu điểm danh thành công.";
      setSuccess(message);
      setAttendanceFeedback({ type: "success", message });
    } catch (requestError) {
      const message = apiError(requestError);
      setError(message);
      setAttendanceFeedback({ type: "error", message });
    }
    finally { setWorking(false); }
  };

  const updateSingleAttendance = async (student) => {
    const formValue = attendanceForm[student.studentId];
    if (!formValue?.status) {
      const message = `Vui lòng chọn trạng thái cho ${student.studentName}.`;
      setError(message);
      setAttendanceFeedback({ type: "error", message });
      return;
    }
    setUpdatingAttendanceId(student.attendanceId);
    setError(""); setSuccess(""); setAttendanceFeedback(null);
    try {
      const response = await authApis().patch(endpoints["attendance-details"](student.attendanceId), {
        status: formValue.status,
        note: formValue.note,
      });
      const updated = apiData(response);
      setSheet((current) => ({
        ...current,
        students: current.students.map((item) => item.attendanceId === updated.id
          ? { ...item, status: updated.status, note: updated.note, attendanceTime: updated.attendanceTime }
          : item),
      }));
      const message = `Đã cập nhật điểm danh cho ${student.studentName}.`;
      setSuccess(message);
      setAttendanceFeedback({ type: "success", message });
    } catch (requestError) {
      const message = apiError(requestError);
      setError(message);
      setAttendanceFeedback({ type: "error", message });
    } finally {
      setUpdatingAttendanceId(null);
    }
  };

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "TEACHER") return <Navigate to="/" replace />;

  const selectedClass = classes.find((item) => String(item.id) === classId);
  const now = new Date();
  const localToday = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  const teacherCanGenerate = selectedClass?.startDate
    ? localToday >= String(selectedClass.startDate).slice(0, 10)
    : false;

  return <section className="attendance-page teacher-attendance-page">
    <div className="student-learning-hero"><div className="public-container"><span className="section-kicker">KHÔNG GIAN GIẢNG DẠY</span><h1>Buổi học & điểm danh</h1><p>Sinh buổi học, cập nhật nội dung và ghi nhận chuyên cần cho từng lớp phụ trách.</p></div></div>
    <div className="public-container attendance-content">
      {error && <div className="public-alert">{error}</div>}
      {success && <div className="profile-success">{success}</div>}
      {loading ? <div className="learning-loading">Đang tải lớp phụ trách...</div> : classes.length === 0 ? <div className="learning-empty"><span>教</span><h2>Chưa có lớp phụ trách</h2><p>Vui lòng liên hệ quản trị viên để được phân công lớp.</p></div> : <>
        <div className="teacher-attendance-toolbar"><label>Chọn lớp<select value={classId} onChange={(event) => { setClassId(event.target.value); setLessons([]); setSchedules([]); setSelectedLesson(null); setSheet(null); setError(""); }}>{classes.map((item) => <option value={item.id} key={item.id}>{item.classCode} · {item.className}</option>)}</select></label><button type="button" onClick={generateLessons} disabled={working || !teacherCanGenerate} title={teacherCanGenerate ? "Sinh buổi học từ lịch cố định" : "Chỉ được sinh buổi học từ ngày khai giảng"}>＋ Sinh buổi học theo lịch</button></div>
        {!teacherCanGenerate && selectedClass && <div className="teacher-generation-note">Có thể sinh buổi học từ ngày khai giảng {formatDate(selectedClass.startDate)}. Trước thời điểm này Admin vẫn có thể điều chỉnh lịch cố định.</div>}
        <section className="fixed-schedule-overview">
          <header><div><span className="section-kicker">LỊCH CỐ ĐỊNH</span><h2>{selectedClass?.className}</h2></div><small>{formatDate(selectedClass?.startDate)} – {formatDate(selectedClass?.endDate)}</small></header>
          <div>{schedules.length ? schedules.map((schedule) => <article key={schedule.id}><strong>{DAY_LABELS[Number(schedule.dayOfWeek)]}</strong><span>{schedule.startTime}–{schedule.endTime}</span><small>{schedule.deliveryMode === "ONLINE" ? "Trực tuyến" : `${schedule.roomName || "Phòng đang cập nhật"}${schedule.roomCode ? ` (${schedule.roomCode})` : ""}${schedule.roomLocation ? ` · ${schedule.roomLocation}` : ""}`}</small></article>) : <p>Lớp chưa có lịch học cố định nên chưa thể sinh buổi học.</p>}</div>
        </section>
        <div className="teacher-lesson-layout">
          <aside className="teacher-lesson-list"><header><h2>Danh sách buổi học</h2><span>{lessons.length}</span></header>{lessons.length === 0 ? <p>Chưa có buổi học. Hãy sinh lesson từ lịch cố định.</p> : lessons.map((lesson, index) => <button type="button" className={selectedLesson?.id === lesson.id ? "active" : ""} onClick={() => chooseLesson(lesson)} key={lesson.id}><span className="lesson-index">{String(index + 1).padStart(2, "0")}</span><span><strong>{lesson.topic || `Buổi ${index + 1}`}</strong><small>{formatDate(lesson.lessonDate)} · {lesson.startTime}–{lesson.endTime}</small></span><em className={`attendance-status status-${lesson.status.toLowerCase()}`}>{lesson.status}</em></button>)}</aside>
          <section className="teacher-lesson-detail">{!selectedLesson ? <div className="attendance-empty">Chọn một buổi học để cập nhật và điểm danh.</div> : <>
            <header><div><span className="section-kicker">BUỔI HỌC {formatDate(selectedLesson.lessonDate)}</span><h2>{selectedLesson.topic || "Chưa có chủ đề"}</h2><p>{selectedLesson.roomName || (selectedLesson.deliveryMode === "ONLINE" ? "Trực tuyến" : "Phòng đang cập nhật")} · {selectedLesson.startTime}–{selectedLesson.endTime}</p></div><button type="button" className="attendance-primary" onClick={openAttendance} disabled={working}>{sheet ? "Tải lại bảng điểm danh" : "Điểm danh / cập nhật"}</button></header>
            <form className="lesson-edit-form" onSubmit={updateLesson}><label>Chủ đề buổi học<input value={editForm.topic} maxLength={255} onChange={(event) => setEditForm({ ...editForm, topic: event.target.value })} /></label><label>Link học trực tuyến<input type="url" value={editForm.meetingUrl} maxLength={500} onChange={(event) => setEditForm({ ...editForm, meetingUrl: event.target.value })} placeholder="https://..." /></label><button disabled={working || selectedLesson.status === "COMPLETED" || selectedLesson.status === "CANCELLED"}>Cập nhật lesson</button></form>
            {sheet && <form className="attendance-sheet" onSubmit={saveAttendance} noValidate><header><h3>Danh sách học viên</h3><span>{sheet.students.length} học viên hợp lệ</span></header>{sheet.students.length === 0 ? <div className="attendance-empty">Lớp chưa có học viên đã xác nhận và thanh toán.</div> : <>{sheet.students.map((student) => <div className="attendance-student-row" key={student.studentId}><div><strong>{student.studentName}</strong><small>{student.studentCode}</small></div><select value={attendanceForm[student.studentId]?.status || ""} onChange={(event) => changeAttendance(student.studentId, "status", event.target.value)}><option value="">Chọn trạng thái</option>{ATTENDANCE_OPTIONS.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select><input value={attendanceForm[student.studentId]?.note || ""} onChange={(event) => changeAttendance(student.studentId, "note", event.target.value)} maxLength={500} placeholder="Ghi chú (không bắt buộc)" />{student.attendanceId ? <button className="attendance-row-update" type="button" disabled={updatingAttendanceId !== null || working} onClick={() => updateSingleAttendance(student)}>{updatingAttendanceId === student.attendanceId ? "Đang cập nhật..." : "Cập nhật riêng"}</button> : <small className="attendance-new-label">Chưa lưu</small>}</div>)}{attendanceFeedback && <div className={`attendance-feedback ${attendanceFeedback.type}`}>{attendanceFeedback.message}</div>}<footer><button className="attendance-primary" disabled={working || updatingAttendanceId !== null}>{working ? "Đang lưu..." : "Lưu điểm danh"}</button></footer></>}</form>}
          </>}</section>
        </div>
      </>}
    </div>
  </section>;
}
