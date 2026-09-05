import { useCallback, useEffect, useState } from "react";
import { authApis, endpoints } from "../../../configs/Apis";
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
  const [keyword, setKeyword] = useState("");
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
        const [classesResponse, coursesResponse, teachersResponse, levelsResponse] =
          await Promise.all([
            api.get(endpoints["admin-classes"], {
              params: {
                keyword,
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
          ]);
        setResult(apiData(classesResponse));
        setCourses(apiData(coursesResponse).content);
        setTeachers(apiData(teachersResponse));
        setLevels(apiData(levelsResponse) || []);
      } catch (requestError) {
        setError(apiError(requestError));
      } finally {
        setLoading(false);
      }
    },
    [keyword, statusFilter, courseFilter, levelFilter, sorting],
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
      await authApis().post(endpoints["admin-classes"], payload);
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
                        onClick={() => setSelected({ ...item })}
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
        <Modal title="Tạo lớp học" onClose={() => setForm(null)}>
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
              <button className="primary-button">Tạo lớp</button>
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
