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
      ["tuitionFee", "Học phí", "number"],
      ["totalSessions", "Số buổi", "number"],
      ["durationHours", "Tổng số giờ", "number"],
      ["description", "Mô tả", "textarea"],
      ["status", "Trạng thái", "status"],
    ],
    initial: {
      levelId: "",
      courseCode: "",
      courseName: "",
      tuitionFee: "",
      totalSessions: 1,
      durationHours: 1,
      description: "",
      status: "ACTIVE",
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const api = authApis();
      const calls = [
        api.get(listEndpoint(type), {
          params: {
            ...(type === "courses" ? { page: 0, size: 100 } : {}),
            status: statusFilter || undefined,
          },
        }),
      ];
      if (type === "levels") calls.push(api.get(endpoints["admin-languages"]));
      if (type === "courses") calls.push(api.get(endpoints.languages));
      if (type === "courses") calls.push(api.get(endpoints.levels));
      const responses = await Promise.all(calls);
      const primary = apiData(responses[0]);
      setItems(primary?.content || primary || []);
      if (responses[1]) setLanguages(apiData(responses[1]) || []);
      if (responses[2]) setLevels(apiData(responses[2]) || []);
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  }, [type, statusFilter]);

  useEffect(() => {
    // Fetch lại danh mục khi loại màn hình thay đổi.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);
  const visible = useMemo(
    () =>
      items.filter((item) =>
        `${item[config.code]} ${item[config.name]}`
          .toLowerCase()
          .includes(keyword.toLowerCase()),
      ),
    [items, keyword, config],
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
      required: !["description", "location", "durationHours"].includes(key),
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
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            {statusOptions[type].map((status) => (
              <option value={status} key={status}>
                {status}
              </option>
            ))}
          </select>
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
