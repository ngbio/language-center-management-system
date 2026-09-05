import { Navigate, Route, Routes } from "react-router-dom";
import AdminLayout from "./components/AdminLayout";
import ProtectedAdminRoute from "./components/ProtectedAdminRoute";
import PublicLayout from "./components/public/PublicLayout";
import ClassListScreen from "./screens/admin/classes/ClassListScreen";
import CourseListScreen from "./screens/admin/courses/CourseListScreen";
import DashboardScreen from "./screens/admin/DashboardScreen";
import LanguageListScreen from "./screens/admin/languages/LanguageListScreen";
import LevelListScreen from "./screens/admin/levels/LevelListScreen";
import LoginScreen from "./screens/admin/LoginScreen";
import RoomListScreen from "./screens/admin/rooms/RoomListScreen";
import UsersScreen from "./screens/admin/UsersScreen";
import CourseDetailScreen from "./screens/public/CourseDetailScreen";
import HomeScreen from "./screens/public/HomeScreen";
import MyCoursesScreen from "./screens/public/MyCoursesScreen";
import MyClassesScreen from "./screens/public/MyClassesScreen";
import StudentLoginScreen from "./screens/public/StudentLoginScreen";
import RegisterScreen from "./screens/public/RegisterScreen";
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
      </Route>
      <Route path="/login" element={<StudentLoginScreen />} />
      <Route path="/register" element={<RegisterScreen />} />
      <Route path="/admin/login" element={<LoginScreen />} />
      <Route element={<ProtectedAdminRoute />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<DashboardScreen />} />
          <Route path="users" element={<UsersScreen />} />
          <Route path="languages" element={<LanguageListScreen />} />
          <Route path="levels" element={<LevelListScreen />} />
          <Route path="courses" element={<CourseListScreen />} />
          <Route path="rooms" element={<RoomListScreen />} />
          <Route path="classes" element={<ClassListScreen />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
