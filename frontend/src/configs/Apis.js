import axios from "axios";
import { SESSION_KEYS, clearSession } from "../utils/authSession";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api";

export const endpoints = {
  login: "/auth/login",
  "admin-login": "/admin/auth/login",
  register: "/auth/register",
  "teacher-register": "/auth/teacher/register",
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
  "course-by-slug": (slug) => `/courses/slug/${slug}`,
  "course-sections": (courseId) => `/courses/${courseId}/sections`,
  "section-contents": (sectionId) => `/sections/${sectionId}/contents`,
  "admin-courses": "/admin/courses",
  "admin-course-details": (courseId) => `/admin/courses/${courseId}`,

  rooms: "/rooms",
  "room-details": (roomId) => `/rooms/${roomId}`,
  "admin-rooms": "/admin/rooms",
  "admin-room-details": (roomId) => `/admin/rooms/${roomId}`,

  classes: "/classes",
  "class-details": (classId) => `/classes/${classId}`,
  "class-schedules": (classId) => `/classes/${classId}/schedules`,
  enrollments: "/enrollments",

  "admin-classes": "/admin/classes",
  "admin-class-details": (classId) => `/admin/classes/${classId}`,
  "assign-class-teacher": (classId) => `/admin/classes/${classId}/teacher`,
  "change-class-status": (classId) => `/admin/classes/${classId}/status`,

  "teacher-classes": "/teachers/me/classes",
  "my-courses": "/students/me/courses",
  "my-classes": "/students/me/classes",
  "my-schedules": "/students/me/schedules",
  "my-enrollments": "/students/me/enrollments",
};

export const authApis = () => {
  const token = localStorage.getItem(SESSION_KEYS.token);
  const instance = axios.create({
    baseURL: BASE_URL,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        clearSession();
        const loginPath = window.location.pathname.startsWith("/admin")
          ? "/admin/login"
          : "/login";
        if (window.location.pathname !== loginPath) window.location.assign(loginPath);
      }
      return Promise.reject(error);
    },
  );

  return instance;
};

export default axios.create({
  baseURL: BASE_URL,
});
