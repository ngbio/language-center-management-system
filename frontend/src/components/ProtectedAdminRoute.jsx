import { Navigate, Outlet, useLocation } from "react-router-dom";

export default function ProtectedAdminRoute() {
  const location = useLocation();
  const token = localStorage.getItem("token");
  const role = localStorage.getItem("role");

  if (!token || role !== "Administrator") {
    return <Navigate to="/admin/login" state={{ from: location }} replace />;
  }
  return <Outlet />;
}
