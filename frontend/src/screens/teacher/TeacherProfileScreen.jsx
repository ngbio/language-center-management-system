import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { authApis, endpoints } from "../../configs/Apis";
import { apiData, apiError } from "../../utils/api";
import { SESSION_KEYS, isTokenActive } from "../../utils/authSession";

const emptyForm = {
  fullName: "",
  phoneNumber: "",
  address: "",
  specialization: "",
  degree: "",
  experienceYears: 0,
};

export default function TeacherProfileScreen() {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (role !== "TEACHER" || !isTokenActive(token)) return undefined;
    let active = true;
    authApis().get(endpoints["teacher-profile"])
      .then((response) => {
        if (!active) return;
        const data = apiData(response);
        setProfile(data);
        setForm({
          fullName: data.fullName || "",
          phoneNumber: data.phoneNumber || "",
          address: data.address || "",
          specialization: data.specialization || "",
          degree: data.degree || "",
          experienceYears: data.experienceYears ?? 0,
        });
      })
      .catch((requestError) => { if (active) setError(apiError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [role, token]);

  if (!isTokenActive(token)) return <Navigate to="/login" replace />;
  if (role !== "TEACHER") return <Navigate to="/" replace />;

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
      const response = await authApis().put(endpoints["teacher-profile"], {
        ...form,
        experienceYears: Number(form.experienceYears),
      });
      setProfile(apiData(response));
      setSuccess("Hồ sơ giảng viên đã được cập nhật.");
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="student-profile-page">
      <div className="student-learning-hero"><div className="public-container">
        <span className="section-kicker">HỒ SƠ GIẢNG VIÊN</span>
        <h1>Thông tin cá nhân</h1>
        <p>Quản lý thông tin liên hệ và hồ sơ chuyên môn của bạn.</p>
      </div></div>
      <div className="public-container student-profile-content">
        {loading && <div className="learning-loading">Đang tải hồ sơ...</div>}
        {error && <div className="public-alert">{error}</div>}
        {success && <div className="profile-success">{success}</div>}
        {!loading && profile && (
          <form className="student-profile-card" onSubmit={submit}>
            <aside>
              <div className="profile-avatar"><span>{profile.fullName?.split(" ").filter(Boolean).slice(-2).map((part) => part.charAt(0)).join("") || "GV"}</span></div>
              <strong>{profile.fullName}</strong>
              <small>{profile.teacherCode}</small>
              <em>{profile.status}</em>
            </aside>
            <div className="profile-form-grid">
              <label>Họ và tên<input name="fullName" value={form.fullName} onChange={changeField} required maxLength={150} /></label>
              <label>Email<input value={profile.email || ""} disabled /></label>
              <label>Tên đăng nhập<input value={profile.username || ""} disabled /></label>
              <label>Số điện thoại<input name="phoneNumber" value={form.phoneNumber} onChange={changeField} maxLength={20} /></label>
              <label>Chuyên môn<input name="specialization" value={form.specialization} onChange={changeField} maxLength={150} /></label>
              <label>Bằng cấp<input name="degree" value={form.degree} onChange={changeField} maxLength={200} /></label>
              <label>Số năm kinh nghiệm<input type="number" name="experienceYears" value={form.experienceYears} onChange={changeField} min="0" max="80" /></label>
              <label className="profile-wide">Địa chỉ<input name="address" value={form.address} onChange={changeField} maxLength={255} /></label>
              <div className="profile-actions"><button type="submit" disabled={saving}>{saving ? "Đang lưu..." : "Lưu thay đổi"}</button></div>
            </div>
          </form>
        )}
      </div>
    </section>
  );
}
