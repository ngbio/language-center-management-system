import axios from "axios";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api";

export const endpoints = {
  login: "/auth/login",
  "admin-login": "/admin/auth/login",
  register: "/auth/register",
  profile: "/auth/me",
  "register-teacher": "/admin/teachers",
  "admin-teachers": "/admin/teachers",
  "admin-users": "/admin/users",
  "admin-user-details": (userId) => `/admin/users/${userId}`,
  "change-user-status": (userId) => `/admin/users/${userId}/status`,

  languages: "/languages",
  "language-details": (languageId) => `/languages/${languageId}`,
  "language-levels": (languageId) => `/languages/${languageId}/levels`,
  "admin-languages": "/admin/languages",
  "admin-language-details": (languageId) =>
    `/admin/languages/${languageId}`,
  "change-language-status": (languageId) =>
    `/admin/languages/${languageId}/status`,

  levels: "/levels",
  "level-details": (levelId) => `/levels/${levelId}`,
  "admin-levels": "/admin/levels",
  "admin-level-details": (levelId) => `/admin/levels/${levelId}`,
  "change-level-status": (levelId) => `/admin/levels/${levelId}/status`,

  courses: "/courses",
  "course-details": (courseId) => `/courses/${courseId}`,
  "admin-courses": "/admin/courses",
  "admin-course-details": (courseId) => `/admin/courses/${courseId}`,

  rooms: "/rooms",
  "room-details": (roomId) => `/rooms/${roomId}`,
  "admin-rooms": "/admin/rooms",
  "admin-room-details": (roomId) => `/admin/rooms/${roomId}`,

  classes: "/classes",
  "class-details": (classId) => `/classes/${classId}`,

  "admin-classes": "/admin/classes",
  "admin-class-details": (classId) => `/admin/classes/${classId}`,
  "assign-class-teacher": (classId) => `/admin/classes/${classId}/teacher`,
  "change-class-status": (classId) => `/admin/classes/${classId}/status`,

  "teacher-classes": "/teachers/me/classes",
};

export const authApis = () => {
  const token = localStorage.getItem("token");

  return axios.create({
    baseURL: BASE_URL,
    headers: token
      ? {
          Authorization: `Bearer ${token}`,
        }
      : {},
  });
};

export default axios.create({
  baseURL: BASE_URL,
});
