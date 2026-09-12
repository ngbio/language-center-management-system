import axios from "axios";
import { SESSION_KEYS, clearSession } from "../utils/authSession";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api";

export const endpoints = {
  login: "/auth/login",
  "admin-login": "/admin/auth/login",
  "staff-login": "/staff/auth/login",
  register: "/auth/register",
  "teacher-register": "/auth/teacher/register",
  teachers: "/teachers",
  profile: "/auth/me",
  "change-password": "/auth/change-password",
  "firebase-chat-token": "/chat/firebase-token",
  "admin-teachers": "/admin/teachers",
  "admin-users": "/admin/users",
  "admin-create-user": "/admin/users",
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
  "admin-course-sections": (courseId) => `/admin/courses/${courseId}/sections`,
  "admin-section-details": (sectionId) => `/admin/sections/${sectionId}`,
  "admin-sections-reorder": "/admin/sections/reorder",
  "admin-section-contents": (sectionId) => `/admin/sections/${sectionId}/contents`,
  "admin-content-details": (contentId) => `/admin/contents/${contentId}`,
  "admin-content-publication": (contentId) => `/admin/contents/${contentId}/publication-status`,
  "admin-contents-reorder": "/admin/contents/reorder",

  "admin-rooms": "/admin/rooms",
  "admin-room-details": (roomId) => `/admin/rooms/${roomId}`,

  classes: "/classes",
  "class-details": (classId) => `/classes/${classId}`,
  "class-schedules": (classId) => `/classes/${classId}/schedules`,
  "schedule-details": (scheduleId) => `/schedules/${scheduleId}`,
  enrollments: "/enrollments",
  "my-payments": "/students/me/payments",
  "staff-enrollments": "/staff/enrollments",
  "staff-enrollment-details": (enrollmentId) => `/staff/enrollments/${enrollmentId}`,
  "class-enrollments": (classId) => `/classes/${classId}/enrollments`,
  "change-enrollment-status": (enrollmentId) => `/staff/enrollments/${enrollmentId}/status`,
  "transfer-enrollment": (enrollmentId) => `/staff/enrollments/${enrollmentId}/transfer`,
  "cancel-enrollment": (enrollmentId) => `/enrollments/${enrollmentId}/cancel-request`,
  "enrollment-payments": (enrollmentId) => `/enrollments/${enrollmentId}/payments`,
  "enrollment-refunds": (enrollmentId) => `/enrollments/${enrollmentId}/refunds`,
  "enrollment-invoice": (enrollmentId) => `/enrollments/${enrollmentId}/invoice`,
  "enrollment-invoice-pdf": (enrollmentId) => `/enrollments/${enrollmentId}/invoice.pdf`,
  "staff-refund": (enrollmentId) => `/staff/enrollments/${enrollmentId}/refunds`,
  "staff-refunds": "/staff/refunds",
  "staff-refresh-refund": (refundId) => `/staff/refunds/${refundId}/refresh`,

  "admin-classes": "/admin/classes",
  "admin-class-details": (classId) => `/admin/classes/${classId}`,
  "assign-class-teacher": (classId) => `/admin/classes/${classId}/teacher`,
  "change-class-status": (classId) => `/admin/classes/${classId}/status`,
  "admin-dashboard-summary": "/admin/dashboard/summary",
  "admin-report-revenue": "/admin/reports/revenue",
  "admin-report-enrollments": "/admin/reports/enrollments",
  "admin-report-popular-courses": "/admin/reports/popular-courses",
  "admin-report-teacher-load": "/admin/reports/teacher-load",
  "admin-report-upcoming-classes": "/admin/reports/upcoming-classes",
  "admin-system-logs": "/admin/system-logs",

  "teacher-classes": "/teachers/me/classes",
  "teacher-courses": "/teachers/me/courses",
  "teacher-profile": "/teachers/me/profile",
  "my-courses": "/students/me/courses",
  "my-classes": "/students/me/classes",
  "my-enrollments": "/students/me/enrollments",
  "student-profile": "/students/me/profile",
  "my-notifications": "/students/me/notifications",
  "unread-notification-count": "/students/me/notifications/unread-count",
  "read-notification": (notificationId) => `/students/me/notifications/${notificationId}/read`,
  "read-all-notifications": "/students/me/notifications/read-all",
  "my-attendance": "/students/me/attendance",
  "my-lessons": "/students/me/lessons",
  "class-lessons": (classId) => `/classes/${classId}/lessons`,
  "generate-class-lessons": (classId) => `/classes/${classId}/lessons/generate`,
  "lesson-details": (lessonId) => `/lessons/${lessonId}`,
  "reschedule-lesson": (lessonId) => `/lessons/${lessonId}/reschedule`,
  "cancel-lesson": (lessonId) => `/lessons/${lessonId}/cancel`,
  "lesson-attendance": (lessonId) => `/lessons/${lessonId}/attendance`,
  "attendance-details": (attendanceId) => `/attendance/${attendanceId}`,
  "upload-image": "/uploads/images",
  "class-attendance-summary": (classId) => `/classes/${classId}/attendance-summary`,
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
          : window.location.pathname.startsWith("/staff")
            ? "/staff/login"
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
