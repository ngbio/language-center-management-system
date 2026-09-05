import { useCallback, useEffect, useMemo, useState } from "react";
import { authApis, endpoints } from "../../configs/Apis";
import {
  EmptyState,
  ErrorAlert,
  LoadingRows,
  Modal,
  PageTitle,
  StatusBadge,
} from "../AdminUi";
import { apiData, apiError, formatMoney } from "../../utils/api";

const definitions = {
  languages: {
    eyebrow: "DANH MỤC ĐÀO TẠO",
    title: "Ngôn ngữ",
    description: "Quản lý các ngôn ngữ đang giảng dạy",
    code: "languageCode",
    name: "languageName",
    fields: [
      ["languageCode", "Mã ngôn ngữ", "text"],
      ["languageName", "Tên ngôn ngữ", "text"],
      ["description", "Mô tả", "textarea"],
      ["status", "Trạng thái", "status"],
    ],
    initial: {
      languageCode: "",
      languageName: "",
      description: "",
      status: "ACTIVE",
    },
  },
  levels: {
    eyebrow: "DANH MỤC ĐÀO TẠO",
    title: "Trình độ",
    description: "Sắp xếp cấp độ theo từng ngôn ngữ",
    code: "levelCode",
    name: "levelName",
    fields: [
      ["languageId", "Ngôn ngữ", "language"],
      ["levelCode", "Mã trình độ", "text"],
      ["levelName", "Tên trình độ", "text"],
      ["displayOrder", "Thứ tự hiển thị", "number"],
      ["description", "Mô tả", "textarea"],
      ["status", "Trạng thái", "status"],
    ],
    initial: {
      languageId: "",
      levelCode: "",
      levelName: "",
      displayOrder: 1,
      description: "",
      status: "ACTIVE",
    },
  },
  courses: {
    eyebrow: "CHƯƠNG TRÌNH HỌC",
    title: "Khóa học",
    description: "Quản lý nội dung, thời lượng và học phí",
    code: "courseCode",
    name: "courseName",
    fields: [
      ["levelId", "Trình độ", "level"],
      ["courseCode", "Mã khóa học", "text"],
      ["courseName", "Tên khóa học", "text"],
      ["slug", "Slug", "text"],
      ["tuitionFee", "Học phí", "number"],
      ["totalSessions", "Số buổi", "number"],
      ["durationHours", "Tổng số giờ", "number"],
      ["description", "Mô tả", "textarea"],
      ["shortDescription", "Mô tả ngắn", "textarea"],
      ["thumbnailUrl", "Ảnh đại diện", "text"],
      ["bannerUrl", "Ảnh banner", "text"],
      ["targetAudience", "Đối tượng học", "textarea"],
      ["prerequisites", "Yêu cầu đầu vào", "textarea"],
      ["learningOutcomes", "Kết quả đầu ra", "textarea"],
      ["syllabusSummary", "Tổng quan giáo trình", "textarea"],
      ["certificateInfo", "Thông tin chứng chỉ", "textarea"],
      ["status", "Trạng thái", "status"],
      ["publicationStatus", "Trạng thái xuất bản", "publicationStatus"],
      ["featured", "Khóa học nổi bật", "checkbox"],
    ],
    initial: {
      levelId: "",
      courseCode: "",
      courseName: "",
      slug: "",
      tuitionFee: "",
      totalSessions: 1,
      durationHours: 1,
      description: "",
      shortDescription: "",
      thumbnailUrl: "",
      bannerUrl: "",
      targetAudience: "",
      prerequisites: "",
      learningOutcomes: "",
      syllabusSummary: "",
      certificateInfo: "",
      status: "ACTIVE",
      publicationStatus: "DRAFT",
      featured: false,
    },
  },
  rooms: {
    eyebrow: "CƠ SỞ VẬT CHẤT",
    title: "Phòng học",
    description: "Quản lý sức chứa và tình trạng phòng",
    code: "roomCode",
    name: "roomName",
    fields: [
      ["roomCode", "Mã phòng", "text"],
      ["roomName", "Tên phòng", "text"],
      ["capacity", "Sức chứa", "number"],
      ["location", "Vị trí", "text"],
      ["status", "Trạng thái", "roomStatus"],
    ],
    initial: {
      roomCode: "",
      roomName: "",
      capacity: 1,
      location: "",
      status: "ACTIVE",
    },
  },
};

const adminCatalogs = new Set(["languages", "levels", "courses", "rooms"]);

const statusOptions = {
  languages: ["ACTIVE", "INACTIVE"],
  levels: ["ACTIVE", "INACTIVE"],
  courses: ["ACTIVE", "INACTIVE"],
  rooms: ["ACTIVE", "MAINTENANCE", "INACTIVE"],
};

const listEndpoint = (type) =>
  adminCatalogs.has(type) ? endpoints[`admin-${type}`] : endpoints[type];

const detailEndpoint = (type, id) => {
  const singular = type.slice(0, -1);
  const key = adminCatalogs.has(type)
    ? `admin-${singular}-details`
    : `${singular}-details`;
  return endpoints[key](id);
};

export default function AdminCatalogScreen({ type }) {
  const config = definitions[type];
  const [items, setItems] = useState([]);
  const [languages, setLanguages] = useState([]);
  const [levels, setLevels] = useState([]);
  const [form, setForm] = useState(null);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [languageFilter, setLanguageFilter] = useState("");
  const [levelFilter, setLevelFilter] = useState("");
  const [courseSort, setCourseSort] = useState("courseCode:asc");
  const [coursePage, setCoursePage] = useState(0);
  const [courseTotalPages, setCourseTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const api = authApis();
      const [sort, direction] = courseSort.split(":");
      const calls = [
        api.get(listEndpoint(type), {
          params: {
            ...(type === "courses" ? {
              page: coursePage, size: 10,
              keyword: keyword || undefined,
              languageId: languageFilter || undefined,
              levelId: levelFilter || undefined,
              sort,
              direction,
            } : {}),
            status: statusFilter || undefined,
          },
        }),
      ];
      if (type === "levels") calls.push(api.get(endpoints["admin-languages"]));
      if (type === "courses") calls.push(api.get(endpoints.languages));
      if (type === "courses") calls.push(api.get(endpoints["admin-levels"], { params: { languageId: languageFilter || undefined } }));
      const responses = await Promise.all(calls);
      const primary = apiData(responses[0]);
      setItems(primary?.content || primary || []);
      if (type === "courses") setCourseTotalPages(primary?.totalPages || 0);
      if (responses[1]) setLanguages(apiData(responses[1]) || []);
      if (responses[2]) setLevels(apiData(responses[2]) || []);
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  }, [type, statusFilter, keyword, languageFilter, levelFilter, courseSort, coursePage]);

  useEffect(() => {
    // Fetch lại danh mục khi loại màn hình thay đổi.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);
  const visible = useMemo(
    () =>
      items.filter((item) => type === "courses" ||
        `${item[config.code]} ${item[config.name]}`
          .toLowerCase()
          .includes(keyword.toLowerCase()),
      ),
    [items, keyword, config, type],
  );

  const save = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const payload = { ...form };
      [
        "languageId",
        "levelId",
        "displayOrder",
        "capacity",
        "totalSessions",
        "durationHours",
      ].forEach((key) => {
        if (payload[key] !== undefined && payload[key] !== "")
          payload[key] = Number(payload[key]);
      });
      if (payload.tuitionFee !== undefined)
        payload.tuitionFee = Number(payload.tuitionFee);
      const api = authApis();
      if (form.id)
        await api.put(detailEndpoint(type, form.id), payload);
      else await api.post(listEndpoint(type), payload);
      setForm(null);
      load();
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const remove = async (item) => {
    if (!window.confirm(`Xóa ${item[config.name]}?`)) return;
    try {
      await authApis().delete(detailEndpoint(type, item.id));
      load();
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const renderField = ([key, label, kind]) => {
    const common = {
      value: form[key] ?? "",
      onChange: (e) => setForm({ ...form, [key]: e.target.value }),
      required:
        ![
          "description",
          "shortDescription",
          "thumbnailUrl",
          "bannerUrl",
          "targetAudience",
          "prerequisites",
          "learningOutcomes",
          "syllabusSummary",
          "certificateInfo",
          "location",
          "durationHours",
          "featured",
        ].includes(key),
    };
    if (kind === "textarea")
      return (
        <label className="field-wide" key={key}>
          {label}
          <textarea {...common} rows="3" />
        </label>
      );
    if (kind === "status")
      return (
        <label key={key}>
          {label}
          <select {...common}>
            <option>ACTIVE</option>
            <option>INACTIVE</option>
          </select>
        </label>
      );
    if (kind === "roomStatus")
      return (
        <label key={key}>
          {label}
          <select {...common}>
            <option>ACTIVE</option>
            <option>MAINTENANCE</option>
            <option>INACTIVE</option>
          </select>
        </label>
      );
    if (kind === "publicationStatus")
      return (
        <label key={key}>
          {label}
          <select {...common}>
            <option>DRAFT</option>
            <option>PUBLISHED</option>
            <option>ARCHIVED</option>
          </select>
        </label>
      );
    if (kind === "checkbox")
      return (
        <label key={key}>
          <input
            type="checkbox"
            checked={Boolean(form[key])}
            onChange={(event) =>
              setForm({ ...form, [key]: event.target.checked })
            }
          />
          {label}
        </label>
      );
    if (kind === "language")
      return (
        <label key={key}>
          {label}
          <select {...common}>
            <option value="">Chọn ngôn ngữ</option>
            {languages.map((item) => (
              <option value={item.id} key={item.id}>
                {item.languageCode} — {item.languageName}
              </option>
            ))}
          </select>
        </label>
      );
    if (kind === "level")
      return (
        <label key={key}>
          {label}
          <select {...common}>
            <option value="">Chọn trình độ</option>
            {levels.map((item) => (
              <option value={item.id} key={item.id}>
                {item.languageCode} · {item.levelCode} — {item.levelName}
              </option>
            ))}
          </select>
        </label>
      );
    return (
      <label key={key}>
        {label}
        <input
          type={kind}
          min={kind === "number" ? 0 : undefined}
          {...common}
        />
      </label>
    );
  };

  return (
    <>
      <PageTitle
        eyebrow={config.eyebrow}
        title={config.title}
        description={config.description}
        action={
          <button
            className="primary-button"
            onClick={() => setForm({ ...config.initial })}
          >
            ＋ Thêm {config.title.toLowerCase()}
          </button>
        }
      />
      <ErrorAlert message={error} />
      <section className="panel table-panel">
        <div className="toolbar">
          <div className="search-box">
            ⌕
            <input
              placeholder={`Tìm ${config.title.toLowerCase()}...`}
              value={keyword}
              onChange={(e) => { setKeyword(e.target.value); if (type === "courses") setCoursePage(0); }}
            />
          </div>
          <select
            value={statusFilter}
            onChange={(event) => { setStatusFilter(event.target.value); if (type === "courses") setCoursePage(0); }}
          >
            <option value="">Tất cả trạng thái</option>
            {statusOptions[type].map((status) => (
              <option value={status} key={status}>
                {status}
              </option>
            ))}
          </select>
          {type === "courses" && <select value={languageFilter} onChange={(event) => { setLanguageFilter(event.target.value); setLevelFilter(""); setCoursePage(0); }}><option value="">Tất cả ngôn ngữ</option>{languages.map((language) => <option value={language.id} key={language.id}>{language.languageName}</option>)}</select>}
          {type === "courses" && <select value={levelFilter} onChange={(event) => { setLevelFilter(event.target.value); setCoursePage(0); }}><option value="">Tất cả trình độ</option>{levels.map((level) => <option value={level.id} key={level.id}>{level.levelCode} · {level.levelName}</option>)}</select>}
          {type === "courses" && <select value={courseSort} onChange={(event) => { setCourseSort(event.target.value); setCoursePage(0); }}><option value="courseCode:asc">Mã A–Z</option><option value="courseName:asc">Tên A–Z</option><option value="courseName:desc">Tên Z–A</option><option value="tuitionFee:asc">Học phí thấp nhất</option><option value="tuitionFee:desc">Học phí cao nhất</option><option value="createdAt:desc">Mới nhất</option></select>}
          <span className="record-count">{visible.length} kết quả</span>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Mã</th>
                <th>{config.title}</th>
                {type === "courses" && <th>Học phí</th>}
                {type === "rooms" && <th>Sức chứa</th>}
                {type === "levels" && <th>Ngôn ngữ</th>}
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <LoadingRows columns={5} />
              ) : (
                visible.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <span className="code-pill">{item[config.code]}</span>
                    </td>
                    <td>
                      <strong>{item[config.name]}</strong>
                      <small>
                        {item.description || item.location || "Không có mô tả"}
                      </small>
                    </td>
                    {type === "courses" && (
                      <td>{formatMoney(item.tuitionFee)}</td>
                    )}
                    {type === "rooms" && <td>{item.capacity} người</td>}
                    {type === "levels" && <td>{item.languageName}</td>}
                    <td>
                      <StatusBadge value={item.status} />
                    </td>
                    <td>
                      <div className="row-actions">
                        <button onClick={() => setForm({ ...item })}>
                          Sửa
                        </button>
                        <button
                          className="danger-link"
                          onClick={() => remove(item)}
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {!loading && !visible.length && <EmptyState />}
        </div>
      </section>
      {type === "courses" && courseTotalPages > 1 && <div className="pagination"><button disabled={coursePage === 0} onClick={() => setCoursePage((value) => value - 1)}>←</button><span>Trang {coursePage + 1} / {courseTotalPages}</span><button disabled={coursePage + 1 >= courseTotalPages} onClick={() => setCoursePage((value) => value + 1)}>→</button></div>}
      {form && (
        <Modal
          title={`${form.id ? "Cập nhật" : "Thêm"} ${config.title.toLowerCase()}`}
          onClose={() => setForm(null)}
        >
          <form onSubmit={save}>
            <div className="form-grid">{config.fields.map(renderField)}</div>
            <div className="modal-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setForm(null)}
              >
                Hủy
              </button>
              <button className="primary-button">
                {form.id ? "Lưu thay đổi" : "Tạo mới"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
