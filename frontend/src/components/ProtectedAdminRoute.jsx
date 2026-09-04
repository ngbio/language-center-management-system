import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { authApis, endpoints } from "../configs/Apis";
import { apiData } from "../utils/api";
import { SESSION_KEYS, isTokenActive } from "../utils/authSession";

export default function ProtectedAdminRoute() {
  const location = useLocation();
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const locallyAuthorized = isTokenActive(token) && role === "ADMIN";
  const [verification, setVerification] = useState(
    locallyAuthorized ? "checking" : "denied",
  );

  useEffect(() => {
    if (!locallyAuthorized) return;

    let active = true;
    authApis()
      .get(endpoints.profile)
      .then((response) => {
        const user = apiData(response);
        if (active) setVerification(user.roleCode === "ADMIN" ? "allowed" : "denied");
      })
      .catch(() => {
        if (active) setVerification("denied");
      });

    return () => {
      active = false;
    };
  }, [locallyAuthorized]);

  if (verification === "denied") {
    return <Navigate to="/admin/login" state={{ from: location }} replace />;
  }

  if (verification === "checking") {
    return <div className="route-loading">Đang xác thực quyền quản trị...</div>;
  }

  return <Outlet />;
}
