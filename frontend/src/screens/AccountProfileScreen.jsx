import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ErrorAlert, PageTitle, StatusBadge } from "../components/AdminUi";
import { authApis, endpoints } from "../configs/Apis";
import { apiData, apiError, formatDateTime } from "../utils/api";

export default function AccountProfileScreen() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    authApis().get(endpoints.profile)
      .then((response) => { if (active) setProfile(apiData(response)); })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return <>
    <PageTitle eyebrow="TÀI KHOẢN" title="Hồ sơ cá nhân"
      description="Thông tin tài khoản đang đăng nhập trong hệ thống" />
    <ErrorAlert message={error} />
    {loading && <section className="panel account-profile-panel">Đang tải hồ sơ...</section>}
    {!loading && profile && <section className="panel account-profile-panel">
      <div className="account-profile-heading">
        <span className="avatar">{profile.fullName?.charAt(0)?.toUpperCase() || "U"}</span>
        <div><h2>{profile.fullName}</h2><p>@{profile.username}</p></div>
        <StatusBadge value={profile.status} />
      </div>
      <div className="detail-grid account-profile-details">
        <span>Email<strong>{profile.email}</strong></span>
        <span>Số điện thoại<strong>{profile.phoneNumber || "—"}</strong></span>
        <span>Vai trò<strong>{profile.roleName} ({profile.roleCode})</strong></span>
        <span>Địa chỉ<strong>{profile.address || "—"}</strong></span>
        <span>Ngày tạo<strong>{formatDateTime(profile.createdAt)}</strong></span>
        <span>Cập nhật gần nhất<strong>{formatDateTime(profile.updatedAt)}</strong></span>
      </div>
      <div className="account-profile-actions"><Link className="primary-button" to="../change-password">Đổi mật khẩu</Link></div>
    </section>}
  </>;
}
