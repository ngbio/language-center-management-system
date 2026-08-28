import { Navigate, Route, Routes } from "react-router-dom";
import AdminLayout from "./components/AdminLayout";
import ProtectedAdminRoute from "./components/ProtectedAdminRoute";
import CatalogScreen from "./screens/admin/CatalogScreen";
import ClassesScreen from "./screens/admin/ClassesScreen";
import DashboardScreen from "./screens/admin/DashboardScreen";
import LoginScreen from "./screens/admin/LoginScreen";
import UsersScreen from "./screens/admin/UsersScreen";
import "./App.css";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/admin" replace />} />
      <Route path="/admin/login" element={<LoginScreen />} />
      <Route element={<ProtectedAdminRoute />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<DashboardScreen />} />
          <Route path="users" element={<UsersScreen />} />
          <Route
            path="languages"
            element={<CatalogScreen type="languages" />}
          />
          <Route path="levels" element={<CatalogScreen type="levels" />} />
          <Route path="courses" element={<CatalogScreen type="courses" />} />
          <Route path="rooms" element={<CatalogScreen type="rooms" />} />
          <Route path="classes" element={<ClassesScreen />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/admin" replace />} />
    </Routes>
  );
}

export default App;
