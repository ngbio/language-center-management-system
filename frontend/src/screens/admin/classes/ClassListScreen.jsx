import { useCallback, useEffect, useState } from "react";
import { authApis, endpoints } from "../../../configs/Apis";
import useDebouncedValue from "../../../hooks/useDebouncedValue";
import {
  EmptyState,
  ErrorAlert,
  LoadingRows,
  Modal,
  PageTitle,
  StatusBadge,
} from "../../../components/AdminUi";
import {
  apiData,
  apiError,
  formatDate,
  formatMoney,
} from "../../../utils/api";

const initialForm = {
  classCode: "",
  className: "",
  startDate: "",
  endDate: "",
  maxStudents: 20,
  appliedTuitionFee: "",
  courseId: "",
  teacherId: "",
};

const initialScheduleForm = {
  dayOfWeek: "1",
  startTime: "",
  endTime: "",
  deliveryMode: "IN_PERSON",
  roomId: "",
  meetingUrl: "",
};

const dayLabels = ["", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy", "Chủ nhật"];

const classStatusTransitions = {
  DRAFT: ["OPEN", "CANCELLED"],
  OPEN: ["FULL", "IN_PROGRESS", "CANCELLED"],
  FULL: ["OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};

export default function ClassListScreen() {
  const [result, setResult] = useState({ content: [], page: 0, totalPages: 0 });
  const [courses, setCourses] = useState([]);
  const [levels, setLevels] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [scheduleForm, setScheduleForm] = useState(null);
  const [lessonForm, setLessonForm] = useState(null);
  const [rescheduleForm, setRescheduleForm] = useState(null);
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebouncedValue(keyword);
  const [statusFilter, setStatusFilter] = useState("");
  const [courseFilter, setCourseFilter] = useState("");
  const [levelFilter, setLevelFilter] = useState("");
  const [sorting, setSorting] = useState("startDate:asc");
  const [form, setForm] = useState(null);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(
    async (page = 0) => {
      setLoading(true);
      setError("");
      try {
        const api = authApis();
        const [sort, direction] = sorting.split(":");
        const [classesResponse, coursesResponse, teachersResponse, levelsResponse, roomsResponse] =
          await Promise.all([
            api.get(endpoints["admin-classes"], {
              params: {
                keyword: debouncedKeyword || undefined,
                status: statusFilter || undefined,
                courseId: courseFilter || undefined,
                levelId: levelFilter || undefined,
                page,
                size: 10,
                sort,
                direction,
              },
            }),
            api.get(endpoints.courses, {
              params: { page: 0, size: 100 },
            }),
            api.get(endpoints["admin-teachers"]),
            api.get(endpoints["admin-levels"]),
            api.get(endpoints["admin-rooms"], { params: { status: "ACTIVE" } }),
          ]);
        setResult(apiData(classesResponse));
        setCourses(apiData(coursesResponse).content);
        setTeachers(apiData(teachersResponse));
        setLevels(apiData(levelsResponse) || []);
        setRooms(apiData(roomsResponse) || []);
      } catch (requestError) {
        setError(apiError(requestError));
      } finally {
        setLoading(false);
      }
    },
    [debouncedKeyword, statusFilter, courseFilter, levelFilter, sorting],
  );

  useEffect(() => {
    // Fetch lại danh sách khi bộ lọc thay đổi.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const save = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const payload = {
        ...form,
        courseId: Number(form.courseId),
        maxStudents: Number(form.maxStudents),
        appliedTuitionFee: Number(form.appliedTuitionFee),
        teacherId: form.teacherId ? Number(form.teacherId) : null,
      };
      if (form.id) {
        await authApis().put(endpoints["admin-class-details"](form.id), payload);
      } else {
        await authApis().post(endpoints["admin-classes"], payload);
      }
      setForm(null);
      load();
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const updateStatus = async (status) => {
    try {
      await authApis().patch(endpoints["change-class-status"](selected.id), {
        status,
      });
      setSelected(null);
      load();
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const assignTeacher = async () => {
    try {
      await authApis().patch(endpoints["assign-class-teacher"](selected.id), {
        teacherId: Number(selected.teacherId),
      });
      setSelected(null);
      load();
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const loadSchedules = async (classId) => {
    setScheduleLoading(true);
    try {
      const response = await authApis().get(endpoints["class-schedules"](classId));
      setSchedules(apiData(response) || []);
    } catch (requestError) {
      setSchedules([]);
      setError(apiError(requestError));
    } finally {
      setScheduleLoading(false);
    }
  };

  const manageClass = async (item) => {
    setScheduleForm(null);
    setLessonForm(null);
    setRescheduleForm(null);
    setError("");
    try {
      const response = await authApis().get(endpoints["admin-class-details"](item.id));
      setSelected(apiData(response));
      await Promise.all([loadSchedules(item.id), loadLessons(item.id)]);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const deleteClass = async () => {
    if (!window.confirm(`Xóa lớp nháp “${selected.className}”?`)) return;
    try {
      await authApis().delete(endpoints["admin-class-details"](selected.id));
      setSelected(null);
      await load(result.page);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const editClass = () => {
    setForm({
      id: selected.id,
      classCode: selected.classCode,
      className: selected.className,
      startDate: String(selected.startDate).slice(0, 10),
      endDate: String(selected.endDate).slice(0, 10),
      maxStudents: selected.maxStudents,
      appliedTuitionFee: selected.appliedTuitionFee,
      courseId: String(selected.courseId),
      teacherId: selected.teacherId ? String(selected.teacherId) : "",
    });
    setSelected(null);
  };

  const loadLessons = async (classId) => {
    try {
      const response = await authApis().get(endpoints["class-lessons"](classId));
      setLessons(apiData(response) || []);
    } catch (requestError) {
      setLessons([]);
      setError(apiError(requestError));
    }
  };

  const editSchedule = (schedule) => {
    setScheduleForm({
      id: schedule.id,
      dayOfWeek: String(schedule.dayOfWeek),
      startTime: String(schedule.startTime).slice(0, 5),
      endTime: String(schedule.endTime).slice(0, 5),
      deliveryMode: schedule.deliveryMode,
      roomId: schedule.roomId ? String(schedule.roomId) : "",
      meetingUrl: schedule.meetingUrl || "",
    });
  };

  const saveSchedule = async (event) => {
    event.preventDefault();
    setError("");
    const payload = {
      dayOfWeek: Number(scheduleForm.dayOfWeek),
      startTime: scheduleForm.startTime,
      endTime: scheduleForm.endTime,
      deliveryMode: scheduleForm.deliveryMode,
      roomId: scheduleForm.deliveryMode === "IN_PERSON" ? Number(scheduleForm.roomId) : null,
      meetingUrl: scheduleForm.deliveryMode === "ONLINE" ? scheduleForm.meetingUrl : null,
    };
    try {
      if (scheduleForm.id) {
        await authApis().put(endpoints["schedule-details"](scheduleForm.id), payload);
      } else {
        await authApis().post(endpoints["class-schedules"](selected.id), payload);
      }
      setScheduleForm(null);
      await loadSchedules(selected.id);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const deleteSchedule = async (schedule) => {
    if (!window.confirm(`Xóa lịch ${dayLabels[schedule.dayOfWeek]} ${String(schedule.startTime).slice(0, 5)}?`)) return;
    setError("");
    try {
      await authApis().delete(endpoints["schedule-details"](schedule.id));
      await loadSchedules(selected.id);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const saveLesson = async (event) => {
    event.preventDefault();
    setError("");
    try {
      await authApis().put(endpoints["lesson-details"](lessonForm.id), {
        topic: lessonForm.topic,
      });
      setLessonForm(null);
      await loadLessons(selected.id);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const rescheduleLesson = async (event) => {
    event.preventDefault();
    setError("");
    try {
      await authApis().patch(endpoints["reschedule-lesson"](rescheduleForm.id), {
        lessonDate: rescheduleForm.lessonDate,
        reason: rescheduleForm.reason,
      });
      setRescheduleForm(null);
      await loadLessons(selected.id);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const cancelLesson = async (lesson) => {
    if (!window.confirm(`Hủy buổi học ngày ${formatDate(lesson.lessonDate)}?`)) return;
    setError("");
    try {
      await authApis().patch(endpoints["cancel-lesson"](lesson.id));
      await loadLessons(selected.id);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  return (
    <>
      <PageTitle
        eyebrow="VẬN HÀNH ĐÀO TẠO"
        title="Lớp học"
        description="Theo dõi các lớp đang mở đăng ký"
        action={
          <button
            className="primary-button"
            onClick={() => setForm({ ...initialForm })}
          >
            ＋ Tạo lớp học
          </button>
        }
      />
      <ErrorAlert message={error} />
      <section className="panel table-panel">
        <div className="toolbar">
          <div className="search-box">
            ⌕
            <input
              placeholder="Tìm mã hoặc tên lớp..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            {["DRAFT", "OPEN", "FULL", "IN_PROGRESS", "COMPLETED", "CANCELLED"].map(
              (status) => (
                <option value={status} key={status}>
                  {status}
                </option>
              ),
            )}
          </select>
          <select value={courseFilter} onChange={(event) => setCourseFilter(event.target.value)}><option value="">Tất cả khóa học</option>{courses.map((course) => <option value={course.id} key={course.id}>{course.courseName}</option>)}</select>
          <select value={levelFilter} onChange={(event) => setLevelFilter(event.target.value)}><option value="">Tất cả trình độ</option>{levels.map((level) => <option value={level.id} key={level.id}>{level.languageCode} · {level.levelCode}</option>)}</select>
          <select value={sorting} onChange={(event) => setSorting(event.target.value)}><option value="startDate:asc">Khai giảng gần nhất</option><option value="startDate:desc">Khai giảng mới nhất</option><option value="className:asc">Tên lớp A–Z</option><option value="className:desc">Tên lớp Z–A</option><option value="appliedTuitionFee:asc">Học phí thấp nhất</option><option value="appliedTuitionFee:desc">Học phí cao nhất</option></select>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Lớp học</th>
                <th>Khóa học</th>
                <th>Thời gian</th>
                <th>Sĩ số</th>
                <th>Học phí</th>
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <LoadingRows columns={7} />
              ) : (
                result.content.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.className}</strong>
                      <small>{item.classCode}</small>
                    </td>
                    <td>
                      <strong className="regular">{item.courseName}</strong>
                      <small>
                        {item.courseCode} · {item.levelCode}
                      </small>
                    </td>
                    <td>
                      <strong className="regular">
                        {formatDate(item.startDate)}
                      </strong>
                      <small>đến {formatDate(item.endDate)}</small>
                    </td>
                    <td>
                      {item.enrolledStudents}/{item.maxStudents}
                      <small>{item.availableSeats} chỗ trống</small>
                    </td>
                    <td>{formatMoney(item.appliedTuitionFee)}</td>
                    <td>
                      <StatusBadge value={item.status} />
                    </td>
                    <td>
                      <button
                        className="link-button"
                        onClick={() => manageClass(item)}
                      >
                        Quản lý →
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {!loading && !result.content.length && (
            <EmptyState message="Chưa có lớp đang mở" />
          )}
        </div>
        <div className="pagination">
          <span>
            Trang {result.page + 1} / {Math.max(result.totalPages, 1)}
          </span>
          <div>
            <button
              disabled={result.page === 0}
              onClick={() => load(result.page - 1)}
            >
              ←
            </button>
            <button
              disabled={result.page + 1 >= result.totalPages}
              onClick={() => load(result.page + 1)}
            >
              →
            </button>
          </div>
        </div>
      </section>

      {form && (
        <Modal title={form.id ? "Cập nhật lớp học" : "Tạo lớp học"} onClose={() => setForm(null)}>
          <form onSubmit={save}>
            <div className="form-grid">
              <label>
                Khóa học
                <select
                  required
                  value={form.courseId}
                  onChange={(e) =>
                    setForm({ ...form, courseId: e.target.value })
                  }
                >
                  <option value="">Chọn khóa học</option>
                  {courses.map((course) => (
                    <option value={course.id} key={course.id}>
                      {course.courseCode} — {course.courseName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Giảng viên
                <select
                  value={form.teacherId}
                  onChange={(e) =>
                    setForm({ ...form, teacherId: e.target.value })
                  }
                >
                  <option value="">Chưa phân công</option>
                  {teachers.map((teacher) => (
                    <option value={teacher.id} key={teacher.id}>
                      {teacher.teacherCode} — {teacher.fullName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Mã lớp
                <input
                  required
                  value={form.classCode}
                  onChange={(e) =>
                    setForm({ ...form, classCode: e.target.value })
                  }
                />
              </label>
              <label>
                Tên lớp
                <input
                  required
                  value={form.className}
                  onChange={(e) =>
                    setForm({ ...form, className: e.target.value })
                  }
                />
              </label>
              <label>
                Ngày bắt đầu
                <input
                  required
                  type="date"
                  value={form.startDate}
                  onChange={(e) =>
                    setForm({ ...form, startDate: e.target.value })
                  }
                />
              </label>
              <label>
                Ngày kết thúc
                <input
                  required
                  type="date"
                  value={form.endDate}
                  onChange={(e) =>
                    setForm({ ...form, endDate: e.target.value })
                  }
                />
              </label>
              <label>
                Sĩ số tối đa
                <input
                  required
                  type="number"
                  min="1"
                  value={form.maxStudents}
                  onChange={(e) =>
                    setForm({ ...form, maxStudents: e.target.value })
                  }
                />
              </label>
              <label>
                Học phí áp dụng
                <input
                  required
                  type="number"
                  min="0"
                  value={form.appliedTuitionFee}
                  onChange={(e) =>
                    setForm({ ...form, appliedTuitionFee: e.target.value })
                  }
                />
              </label>
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setForm(null)}
              >
                Hủy
              </button>
              <button className="primary-button">{form.id ? "Lưu thay đổi" : "Tạo lớp"}</button>
            </div>
          </form>
        </Modal>
      )}

      {selected && (
        <Modal
          title={`Quản lý ${selected.classCode}`}
          onClose={() => setSelected(null)}
        >
          <div className="detail-grid">
            <span>
              Tên lớp<strong>{selected.className}</strong>
            </span>
            <span>
              Khóa học<strong>{selected.courseName}</strong>
            </span>
            <span>
              Giảng viên
              <strong>{selected.teacherName || "Chưa phân công"}</strong>
            </span>
            <span>
              Sĩ số
              <strong>
                {selected.enrolledStudents}/{selected.maxStudents}
              </strong>
            </span>
          </div>
          <div className="modal-actions">
            <button type="button" className="primary-button" onClick={editClass}>Sửa thông tin lớp</button>
            {selected.status === "DRAFT" && <button type="button" className="secondary-button danger-link" onClick={deleteClass}>Xóa lớp nháp</button>}
          </div>
          <div className="section-label">Phân công giảng viên</div>
          <div className="inline-action">
            <select
              value={selected.teacherId || ""}
              onChange={(e) =>
                setSelected({ ...selected, teacherId: e.target.value })
              }
            >
              <option value="">Chọn giảng viên</option>
              {teachers.map((teacher) => (
                <option value={teacher.id} key={teacher.id}>
                  {teacher.teacherCode} — {teacher.fullName}
                </option>
              ))}
            </select>
            <button
              className="secondary-button"
              onClick={assignTeacher}
              disabled={!selected.teacherId}
            >
              Gán giảng viên
            </button>
          </div>
          <div className="section-label schedule-section-heading"><span>Lịch học cố định</span><button type="button" className="link-button" onClick={() => setScheduleForm({ ...initialScheduleForm })}>＋ Thêm lịch</button></div>
          <ErrorAlert message={error} />
          {scheduleLoading ? <div className="schedule-admin-empty">Đang tải lịch học...</div> : schedules.length ? <div className="schedule-admin-list">{schedules.map((schedule) => <article key={schedule.id}><div><strong>{dayLabels[schedule.dayOfWeek]}</strong><small>{String(schedule.startTime).slice(0, 5)} – {String(schedule.endTime).slice(0, 5)}</small></div><div><strong>{schedule.deliveryMode === "ONLINE" ? "Trực tuyến" : schedule.roomName || schedule.roomCode}</strong><small>{schedule.deliveryMode === "ONLINE" ? schedule.meetingUrl : schedule.roomLocation || "Chưa có vị trí"}</small></div><div><button type="button" onClick={() => editSchedule(schedule)}>Sửa</button><button type="button" className="danger-link" onClick={() => deleteSchedule(schedule)}>Xóa</button></div></article>)}</div> : <div className="schedule-admin-empty">Lớp chưa có lịch học cố định.</div>}
          {scheduleForm && <form className="schedule-admin-form" onSubmit={saveSchedule}><div className="form-grid"><label>Thứ<select required value={scheduleForm.dayOfWeek} onChange={(event) => setScheduleForm({ ...scheduleForm, dayOfWeek: event.target.value })}>{dayLabels.slice(1).map((label, index) => <option value={index + 1} key={label}>{label}</option>)}</select></label><label>Hình thức<select value={scheduleForm.deliveryMode} onChange={(event) => setScheduleForm({ ...scheduleForm, deliveryMode: event.target.value, roomId: "", meetingUrl: "" })}><option value="IN_PERSON">Tại trung tâm</option><option value="ONLINE">Trực tuyến</option></select></label><label>Giờ bắt đầu<input required type="time" value={scheduleForm.startTime} onChange={(event) => setScheduleForm({ ...scheduleForm, startTime: event.target.value })} /></label><label>Giờ kết thúc<input required type="time" value={scheduleForm.endTime} onChange={(event) => setScheduleForm({ ...scheduleForm, endTime: event.target.value })} /></label>{scheduleForm.deliveryMode === "IN_PERSON" ? <label>Phòng học<select required value={scheduleForm.roomId} onChange={(event) => setScheduleForm({ ...scheduleForm, roomId: event.target.value })}><option value="">Chọn phòng</option>{rooms.map((room) => <option value={room.id} key={room.id}>{room.roomCode} — {room.roomName}</option>)}</select></label> : <label>Link phòng học<input required type="url" maxLength="500" value={scheduleForm.meetingUrl} onChange={(event) => setScheduleForm({ ...scheduleForm, meetingUrl: event.target.value })} placeholder="https://..." /></label>}</div><div className="modal-actions"><button type="button" className="secondary-button" onClick={() => setScheduleForm(null)}>Hủy</button><button className="primary-button">{scheduleForm.id ? "Lưu lịch" : "Thêm lịch"}</button></div></form>}
          <div className="section-label">Danh sách buổi học</div>
          {lessons.length ? <div className="lesson-admin-list">{lessons.map((lesson, index) => <article key={lesson.id}><span>{String(index + 1).padStart(2, "0")}</span><div><strong>{lesson.topic || `Buổi học ${index + 1}`}</strong><small>{formatDate(lesson.lessonDate)} · {String(lesson.startTime).slice(0, 5)}–{String(lesson.endTime).slice(0, 5)} · {lesson.status}</small>{lesson.originalLessonDate && <small>Đã dời từ {formatDate(lesson.originalLessonDate)} · {lesson.rescheduleReason}</small>}</div><div><button type="button" disabled={lesson.status === "COMPLETED" || lesson.status === "CANCELLED"} onClick={() => setLessonForm({ id: lesson.id, topic: lesson.topic || "" })}>Sửa</button><button type="button" disabled={lesson.status === "COMPLETED" || lesson.status === "CANCELLED"} onClick={() => setRescheduleForm({ id: lesson.id, lessonDate: String(lesson.lessonDate).slice(0, 10), reason: "" })}>Dời ngày</button><button type="button" className="danger-link" disabled={lesson.status === "COMPLETED" || lesson.status === "CANCELLED"} onClick={() => cancelLesson(lesson)}>Hủy buổi</button></div></article>)}</div> : <div className="schedule-admin-empty">Lớp chưa có buổi học được sinh.</div>}
          {lessonForm && <form className="schedule-admin-form" onSubmit={saveLesson}><div className="form-grid"><label>Chủ đề buổi học<input maxLength="255" value={lessonForm.topic} onChange={(event) => setLessonForm({ ...lessonForm, topic: event.target.value })} /></label></div><p className="form-note">Link học trực tuyến được quản lý tại lịch học cố định của lớp.</p><div className="modal-actions"><button type="button" className="secondary-button" onClick={() => setLessonForm(null)}>Hủy</button><button className="primary-button">Lưu nội dung</button></div></form>}
          {rescheduleForm && <form className="schedule-admin-form" onSubmit={rescheduleLesson}><div className="form-grid"><label>Ngày học mới<input required type="date" min={String(selected.startDate).slice(0, 10)} max={String(selected.endDate).slice(0, 10)} value={rescheduleForm.lessonDate} onChange={(event) => setRescheduleForm({ ...rescheduleForm, lessonDate: event.target.value })} /></label><label>Lý do dời lịch<textarea required maxLength="500" value={rescheduleForm.reason} onChange={(event) => setRescheduleForm({ ...rescheduleForm, reason: event.target.value })} placeholder="Ví dụ: Nghỉ lễ, giáo viên xin nghỉ, phòng học gặp sự cố..." /></label></div><p className="form-note">Thông báo tự động cho giảng viên và học viên sẽ được bổ sung trong module Notification.</p><div className="modal-actions"><button type="button" className="secondary-button" onClick={() => setRescheduleForm(null)}>Hủy</button><button className="primary-button">Xác nhận dời ngày</button></div></form>}
          <div className="section-label">Chuyển trạng thái</div>
          <div className="status-actions">
            {(classStatusTransitions[selected.status] || []).map((status) => (
              <button key={status} onClick={() => updateStatus(status)}>
                {status}
              </button>
            ))}
            {!classStatusTransitions[selected.status]?.length && (
              <span>Trạng thái này không thể chuyển tiếp.</span>
            )}
          </div>
        </Modal>
      )}
    </>
  );
}
