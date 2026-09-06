import { Navigate, Route, Routes } from "react-router-dom";
import AdminLayout from "./components/AdminLayout";
import ProtectedAdminRoute from "./components/ProtectedAdminRoute";
import ProtectedStaffRoute from "./components/ProtectedStaffRoute";
import StaffLayout from "./components/StaffLayout";
import PublicLayout from "./components/public/PublicLayout";
import ClassListScreen from "./screens/admin/classes/ClassListScreen";
import CourseListScreen from "./screens/admin/courses/CourseListScreen";
import DashboardScreen from "./screens/admin/DashboardScreen";
import LanguageListScreen from "./screens/admin/languages/LanguageListScreen";
import LevelListScreen from "./screens/admin/levels/LevelListScreen";
import LoginScreen from "./screens/admin/LoginScreen";
import RoomListScreen from "./screens/admin/rooms/RoomListScreen";
import UsersScreen from "./screens/admin/UsersScreen";
import EnrollmentManagementScreen from "./screens/admin/enrollments/EnrollmentManagementScreen";
import CourseDetailScreen from "./screens/public/CourseDetailScreen";
import HomeScreen from "./screens/public/HomeScreen";
import MyCoursesScreen from "./screens/public/MyCoursesScreen";
import MyClassesScreen from "./screens/public/MyClassesScreen";
import EnrollmentHistoryScreen from "./screens/public/EnrollmentHistoryScreen";
import StudentLoginScreen from "./screens/public/StudentLoginScreen";
import RegisterScreen from "./screens/public/RegisterScreen";
import PaymentResultScreen from "./screens/public/PaymentResultScreen";
import StudentProfileScreen from "./screens/public/StudentProfileScreen";
import TeacherCoursesScreen from "./screens/teacher/TeacherCoursesScreen";
import TeacherClassesScreen from "./screens/teacher/TeacherClassesScreen";
import TeacherProfileScreen from "./screens/teacher/TeacherProfileScreen";
import StaffLoginScreen from "./screens/staff/StaffLoginScreen";
import CoursesScreen from "./screens/public/CoursesScreen";
import LanguagesScreen from "./screens/public/LanguagesScreen";
import OpenClassesScreen from "./screens/public/OpenClassesScreen";
import "./App.css";

function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomeScreen />} />
        <Route path="/khoa-hoc" element={<CoursesScreen />} />
        <Route path="/ngon-ngu" element={<LanguagesScreen />} />
        <Route path="/lop-hoc" element={<OpenClassesScreen />} />
        <Route path="/khoa-hoc/:slug" element={<CourseDetailScreen />} />
        <Route path="/khoa-hoc-cua-toi" element={<MyCoursesScreen />} />
        <Route path="/lop-hoc-cua-toi" element={<MyClassesScreen />} />
        <Route path="/lich-su-dang-ky" element={<EnrollmentHistoryScreen />} />
        <Route path="/thong-tin-ca-nhan" element={<StudentProfileScreen />} />
        <Route path="/thanh-toan/ket-qua" element={<PaymentResultScreen />} />
        <Route path="/giao-vien/khoa-hoc" element={<TeacherCoursesScreen />} />
        <Route path="/giao-vien/lop-hoc" element={<TeacherClassesScreen />} />
        <Route path="/giao-vien/thong-tin-ca-nhan" element={<TeacherProfileScreen />} />
      </Route>
      <Route path="/login" element={<StudentLoginScreen />} />
      <Route path="/register" element={<RegisterScreen />} />
      <Route path="/admin/login" element={<LoginScreen />} />
      <Route path="/staff/login" element={<StaffLoginScreen />} />
      <Route element={<ProtectedAdminRoute />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<DashboardScreen />} />
          <Route path="users" element={<UsersScreen />} />
          <Route path="languages" element={<LanguageListScreen />} />
          <Route path="levels" element={<LevelListScreen />} />
          <Route path="courses" element={<CourseListScreen />} />
          <Route path="rooms" element={<RoomListScreen />} />
          <Route path="classes" element={<ClassListScreen />} />
          <Route path="enrollments" element={<EnrollmentManagementScreen />} />
        </Route>
      </Route>
      <Route element={<ProtectedStaffRoute />}>
        <Route path="/staff" element={<StaffLayout />}>
          <Route index element={<Navigate to="enrollments" replace />} />
          <Route path="enrollments" element={<EnrollmentManagementScreen />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
