export const SESSION_KEYS = {
  token: "token",
  role: "role",
  email: "email",
};

export const clearSession = () => {
  Object.values(SESSION_KEYS).forEach((key) => localStorage.removeItem(key));
  // Clean up keys used by the previous split-session implementation.
  ["adminToken", "adminRole", "adminEmail", "publicToken", "publicRole", "publicEmail", "studentEmail"]
    .forEach((key) => localStorage.removeItem(key));
};

export const isTokenActive = (token) => {
  if (!token) return false;

  try {
    const payloadPart = token.split(".")[1];
    if (!payloadPart) return false;
    const normalized = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const payload = JSON.parse(atob(normalized));
    return typeof payload.exp === "number" && payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
};
