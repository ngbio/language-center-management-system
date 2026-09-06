import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";
import { SESSION_KEYS, isTokenActive } from "../../utils/authSession";

const emptyForm = {
  fullName: "",
  phoneNumber: "",
  address: "",
  dateOfBirth: "",
  gender: "",
  avatar: "",
};

export default function StudentProfileScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (role !== "STUDENT" || !isTokenActive(token)) return undefined;
    let active = true;
    authApis().get(endpoints["student-profile"])
      .then((response) => {
        if (!active) return;
        const data = apiData(response);
        setProfile(data);
        setForm({
          fullName: data.fullName || "",
          phoneNumber: data.phoneNumber || "",
          address: data.address || "",
          dateOfBirth: data.dateOfBirth ? String(data.dateOfBirth).slice(0, 10) : "",
          gender: data.gender || "",
          avatar: data.avatar || "",
        });
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [role, token]);

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "STUDENT") return <Navigate to="/" replace />;

  const changeField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const response = await authApis().put(endpoints["student-profile"], {
        ...form,
        dateOfBirth: form.dateOfBirth || null,
        gender: form.gender || null,
      });
      const data = apiData(response);
      setProfile(data);
      setSuccess("Thông tin cá nhân đã được cập nhật.");
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="student-profile-page">
      <div className="student-learning-hero"><div className="public-container">
        <span className="section-kicker">TÀI KHOẢN HỌC VIÊN</span>
        <h1>Thông tin cá nhân</h1>
        <p>Cập nhật thông tin liên hệ và hồ sơ học viên của bạn.</p>
      </div></div>
      <div className="public-container student-profile-content">
        {loading && <div className="learning-loading">Đang tải hồ sơ...</div>}
        {error && <div className="public-alert">{error}</div>}
        {success && <div className="profile-success">{success}</div>}
        {!loading && profile && (
          <form className="student-profile-card" onSubmit={submit}>
            <aside>
              <div className="profile-avatar">
                {form.avatar ? <img src={form.avatar} alt="Ảnh đại diện" /> : <span>{profile.fullName?.charAt(0)?.toUpperCase() || "H"}</span>}
              </div>
              <strong>{profile.fullName}</strong>
              <small>{profile.studentCode}</small>
              <em>{profile.status}</em>
            </aside>
            <div className="profile-form-grid">
              <label>Họ và tên<input name="fullName" value={form.fullName} onChange={changeField} required maxLength={150} /></label>
              <label>Email<input value={profile.email || ""} disabled /></label>
              <label>Tên đăng nhập<input value={profile.username || ""} disabled /></label>
              <label>Số điện thoại<input name="phoneNumber" value={form.phoneNumber} onChange={changeField} maxLength={20} /></label>
              <label>Ngày sinh<input type="date" name="dateOfBirth" value={form.dateOfBirth} onChange={changeField} /></label>
              <label>Giới tính<select name="gender" value={form.gender} onChange={changeField}><option value="">Chưa cập nhật</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></select></label>
              <label className="profile-wide">Địa chỉ<input name="address" value={form.address} onChange={changeField} maxLength={255} /></label>
              <label className="profile-wide">URL ảnh đại diện<input type="url" name="avatar" value={form.avatar} onChange={changeField} maxLength={500} placeholder="https://..." /></label>
              <div className="profile-actions"><button type="submit" disabled={saving}>{saving ? "Đang lưu..." : "Lưu thay đổi"}</button></div>
            </div>
          </form>
        )}
      </div>
    </section>
  );
}
