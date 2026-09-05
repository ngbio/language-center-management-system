import { useCallback, useEffect, useState } from "react";
import { authApis, endpoints } from "../../configs/Apis";
import {
  EmptyState,
  ErrorAlert,
  LoadingRows,
  Modal,
  PageTitle,
  StatusBadge,
} from "../../components/AdminUi";
import { apiData, apiError, formatDate } from "../../utils/api";

const statuses = ["ACTIVE", "INACTIVE", "LOCKED"];

export default function UsersScreen() {
  const [result, setResult] = useState({
    content: [],
    page: 0,
    totalPages: 0,
    totalElements: 0,
  });
  const [filters, setFilters] = useState({
    keyword: "",
    roleCode: "",
    status: "",
  });
  const [selected, setSelected] = useState(null);
  const [sorting, setSorting] = useState("createdAt:desc");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(
    async (page = 0) => {
      setLoading(true);
      setError("");
      try {
        const [sort, direction] = sorting.split(":");
        setResult(
          apiData(
            await authApis().get(endpoints["admin-users"], {
              params: {
                keyword: filters.keyword || undefined,
                roleCode: filters.roleCode || undefined,
                status: filters.status || undefined,
                page,
                size: 10,
                sort,
                direction,
              },
            }),
          ),
        );
      } catch (requestError) {
        setError(apiError(requestError));
      } finally {
        setLoading(false);
      }
    },
    [filters, sorting],
  );

  useEffect(() => {
    // Fetch lại người dùng khi bộ lọc thay đổi.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(0);
  }, [load]);

  const view = async (id) => {
    try {
      setSelected(
        apiData(await authApis().get(endpoints["admin-user-details"](id))),
      );
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  const changeStatus = async (status) => {
    try {
      await authApis().patch(endpoints["change-user-status"](selected.id), {
        status,
      });
      setSelected(null);
      load(result.page);
    } catch (requestError) {
      setError(apiError(requestError));
    }
  };

  return (
    <>
      <PageTitle
        eyebrow="QUẢN TRỊ & PHÂN QUYỀN"
        title="Người dùng"
        description={`${result.totalElements || 0} tài khoản trong hệ thống`}
      />
      <ErrorAlert message={error} />
      <section className="panel table-panel">
        <div className="toolbar">
          <div className="search-box">
            ⌕
            <input
              placeholder="Tìm tên, email, username..."
              value={filters.keyword}
              onChange={(e) =>
                setFilters({ ...filters, keyword: e.target.value })
              }
            />
          </div>
          <select
            value={filters.roleCode}
            onChange={(e) =>
              setFilters({ ...filters, roleCode: e.target.value })
            }
          >
            <option value="">Tất cả vai trò</option>
            <option>ADMIN</option>
            <option>CONSULTANT</option>
            <option>TEACHER</option>
            <option>STUDENT</option>
          </select>
          <select value={sorting} onChange={(event) => setSorting(event.target.value)}>
            <option value="createdAt:desc">Mới tạo gần đây</option>
            <option value="createdAt:asc">Cũ nhất</option>
            <option value="fullName:asc">Tên A–Z</option>
            <option value="fullName:desc">Tên Z–A</option>
            <option value="email:asc">Email A–Z</option>
          </select>
          <select
            value={filters.status}
            onChange={(e) => setFilters({ ...filters, status: e.target.value })}
          >
            <option value="">Tất cả trạng thái</option>
            {statuses.map((value) => (
              <option key={value}>{value}</option>
            ))}
          </select>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Người dùng</th>
                <th>Liên hệ</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <LoadingRows columns={6} />
              ) : (
                result.content.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <div className="person-cell">
                        <span className="avatar">
                          {user.fullName?.charAt(0)}
                        </span>
                        <div>
                          <strong>{user.fullName}</strong>
                          <small>@{user.username}</small>
                        </div>
                      </div>
                    </td>
                    <td>
                      <strong className="regular">{user.email}</strong>
                      <small>{user.phoneNumber || "Chưa có SĐT"}</small>
                    </td>
                    <td>{user.roleName}</td>
                    <td>
                      <StatusBadge value={user.status} />
                    </td>
                    <td>{formatDate(user.createdAt)}</td>
                    <td>
                      <button
                        className="link-button"
                        onClick={() => view(user.id)}
                      >
                        Chi tiết →
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {!loading && !result.content.length && <EmptyState />}
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
      {selected && (
        <Modal title="Chi tiết người dùng" onClose={() => setSelected(null)}>
          <div className="detail-grid">
            <span>
              Họ tên<strong>{selected.fullName}</strong>
            </span>
            <span>
              Email<strong>{selected.email}</strong>
            </span>
            <span>
              Username<strong>{selected.username}</strong>
            </span>
            <span>
              Vai trò<strong>{selected.roleName}</strong>
            </span>
            <span>
              Điện thoại<strong>{selected.phoneNumber || "—"}</strong>
            </span>
            <span>
              Địa chỉ<strong>{selected.address || "—"}</strong>
            </span>
          </div>
          <div className="modal-actions">
            <select
              value={selected.status}
              onChange={(e) =>
                setSelected({ ...selected, status: e.target.value })
              }
            >
              {statuses.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
            <button
              className="primary-button"
              onClick={() => changeStatus(selected.status)}
            >
              Cập nhật trạng thái
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
