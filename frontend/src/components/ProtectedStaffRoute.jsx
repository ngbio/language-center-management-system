import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { authApis, endpoints } from "../configs/Apis";
import { apiData } from "../utils/api";
import { SESSION_KEYS, isTokenActive } from "../utils/authSession";

export default function ProtectedStaffRoute() {
  const location = useLocation();
  const token = localStorage.getItem(SESSION_KEYS.token);
  const role = localStorage.getItem(SESSION_KEYS.role);
  const locallyAuthorized = isTokenActive(token) && role === "CONSULTANT";
  const [verification, setVerification] = useState(locallyAuthorized ? "checking" : "denied");

  useEffect(() => {
    if (!locallyAuthorized) return;
    let active = true;
    authApis().get(endpoints.profile)
      .then((response) => {
        if (active) {
          setVerification(apiData(response).roleCode === "CONSULTANT" ? "allowed" : "denied");
        }
      })
      .catch(() => active && setVerification("denied"));
    return () => { active = false; };
  }, [locallyAuthorized]);

  if (verification === "denied") {
    return <Navigate to="/staff/login" state={{ from: location }} replace />;
  }
  if (verification === "checking") {
    return <div className="route-loading">Đang xác thực quyền nhân viên...</div>;
  }
  return <Outlet />;
}
