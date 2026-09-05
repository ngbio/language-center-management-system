import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import api, { endpoints } from "../../configs/Apis";
import { apiError } from "../../utils/api";
import { getActiveSessionHome } from "../../utils/authSession";
import "../../styles/PublicSite.css";

const studentInitial = {
  username: "", password: "", fullName: "", email: "", phoneNumber: "",
  address: "", dateOfBirth: "", gender: "",
};

const teacherInitial = {
  username: "", password: "", fullName: "", email: "", phoneNumber: "",
  address: "", specialization: "", degree: "", experienceYears: 0,
};

const optionalToNull = (value) => value === "" ? null : value;

export default function RegisterScreen() {
  const [accountType, setAccountType] = useState("student");
  const [studentForm, setStudentForm] = useState(studentInitial);
  const [teacherForm, setTeacherForm] = useState(teacherInitial);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const activeSessionHome = getActiveSessionHome();
  const darkMode = localStorage.getItem("publicTheme") === "dark";

  if (activeSessionHome) return <Navigate to={activeSessionHome} replace />;

  const form = accountType === "student" ? studentForm : teacherForm;
  const setForm = accountType === "student" ? setStudentForm : setTeacherForm;
  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      const common = {
        ...form,
        phoneNumber: optionalToNull(form.phoneNumber),
        address: optionalToNull(form.address),
      };
      const payload = accountType === "student"
        ? {
            ...common,
            dateOfBirth: optionalToNull(form.dateOfBirth),
            gender: optionalToNull(form.gender),
            avatar: null,
          }
        : {
            ...common,
            specialization: optionalToNull(form.specialization),
            degree: optionalToNull(form.degree),
            experienceYears: Number(form.experienceYears || 0),
          };
      await api.post(accountType === "student" ? endpoints.register : endpoints["teacher-register"], payload);
      navigate("/login", {
        replace: true,
        state: {
          registrationSuccess: accountType === "student"
            ? "Đăng ký học viên thành công. Bạn có thể đăng nhập ngay."
            : "Đăng ký giáo viên thành công. Vui lòng chờ quản trị viên kích hoạt tài khoản.",
        },
      });
    } catch (requestError) {
      setError(apiError(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className={`public-site student-login-page register-page ${darkMode ? "dark" : ""}`}>
      <section className="student-login-visual">
        <Link className="public-brand" to="/">
          <span className="public-brand-mark">LC</span>
          <span><strong>Lingua Center</strong><small>Học ngoại ngữ mỗi ngày</small></span>
        </Link>
        <div><span className="section-kicker">BẮT ĐẦU HỌC TẬP</span><h1>Tạo tài khoản của bạn.</h1><p>Học viên có thể sử dụng ngay. Hồ sơ giáo viên cần được quản trị viên xác minh và kích hoạt.</p></div>
        <span className="student-login-japanese">始める</span>
      </section>
      <section className="student-login-panel register-panel">
        <form className="student-login-form register-form" onSubmit={submit}>
          <Link className="back-home" to="/login">← Về đăng nhập</Link>
          <span className="section-kicker">TẠO TÀI KHOẢN</span>
          <h2>Đăng ký</h2>
          <div className="register-tabs" role="tablist">
            <button type="button" className={accountType === "student" ? "active" : ""} onClick={() => { setAccountType("student"); setError(""); }}>Học viên</button>
            <button type="button" className={accountType === "teacher" ? "active" : ""} onClick={() => { setAccountType("teacher"); setError(""); }}>Giáo viên</button>
          </div>
          {accountType === "teacher" && <div className="register-note">Tài khoản giáo viên sẽ ở trạng thái chờ cho tới khi Admin kích hoạt.</div>}
          {error && <div className="public-alert">{error}</div>}
          <div className="register-grid">
            <label>Họ và tên<input name="fullName" required maxLength="150" value={form.fullName} onChange={update} /></label>
            <label>Tên đăng nhập<input name="username" required maxLength="100" value={form.username} onChange={update} /></label>
            <label>Email<input name="email" type="email" required maxLength="150" value={form.email} onChange={update} /></label>
            <label>Mật khẩu<input name="password" type="password" required minLength="6" maxLength="100" value={form.password} onChange={update} /></label>
            <label>Số điện thoại<input name="phoneNumber" maxLength="20" value={form.phoneNumber} onChange={update} /></label>
            {accountType === "student" ? (
              <>
                <label>Ngày sinh<input name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={update} /></label>
                <label>Giới tính<select name="gender" value={form.gender} onChange={update}><option value="">Chưa chọn</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></select></label>
              </>
            ) : (
              <>
                <label>Chuyên môn<input name="specialization" maxLength="150" value={form.specialization} onChange={update} /></label>
                <label>Bằng cấp<input name="degree" maxLength="200" value={form.degree} onChange={update} /></label>
                <label>Số năm kinh nghiệm<input name="experienceYears" type="number" min="0" value={form.experienceYears} onChange={update} /></label>
              </>
            )}
            <label className="register-full">Địa chỉ<input name="address" maxLength="255" value={form.address} onChange={update} /></label>
          </div>
          <button className="primary-cta student-login-submit" disabled={loading}>{loading ? "Đang gửi..." : accountType === "student" ? "Đăng ký học viên" : "Gửi đăng ký giáo viên"}</button>
          <small>Đã có tài khoản? <Link to="/login">Đăng nhập</Link>.</small>
        </form>
      </section>
    </main>
  );
}
