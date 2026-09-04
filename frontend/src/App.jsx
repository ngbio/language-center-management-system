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
import StudentLoginScreen from "./screens/public/StudentLoginScreen";
import "./App.css";

function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomeScreen />} />
        <Route path="/khoa-hoc/:slug" element={<CourseDetailScreen />} />
      </Route>
      <Route path="/login" element={<StudentLoginScreen />} />
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
