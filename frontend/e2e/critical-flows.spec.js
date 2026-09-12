import { expect, test } from "@playwright/test";

const apiResponse = (data, status = 200) => ({ status, message: "OK", data });
const json = (route, data, status = 200) =>
  route.fulfill({ status, contentType: "application/json", body: JSON.stringify(apiResponse(data, status)) });

async function useSession(page, role) {
  await page.addInitScript((sessionRole) => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
    localStorage.setItem("token", `header.${payload}.signature`);
    localStorage.setItem("role", sessionRole);
    localStorage.setItem("email", `${sessionRole.toLowerCase()}@example.com`);
  }, role);
}

test("học viên gửi kèm lý do khi hủy đăng ký", async ({ page }) => {
  await useSession(page, "STUDENT");
  let cancellationPayload;
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === "/api/students/me/enrollments") {
      return json(route, [{
        id: 12, className: "English A1", classCode: "EN-A1-01", courseName: "English A1",
        enrollmentDate: "2026-09-01", amountDue: 1000000, enrollmentStatus: "CONFIRMED",
        paymentStatus: "PENDING", paymentDeadline: "2026-09-15T12:00:00",
      }]);
    }
    if (path === "/api/students/me/payments") return json(route, []);
    if (path === "/api/enrollments/12/cancel-request" && request.method() === "POST") {
      cancellationPayload = request.postDataJSON();
      return json(route, { id: 12, enrollmentStatus: "CANCELLED" });
    }
    return json(route, {});
  });
  page.on("dialog", async (dialog) => {
    await dialog.accept(dialog.type() === "prompt" ? "Không thể tiếp tục học" : undefined);
  });

  await page.goto("/lich-su-dang-ky");
  await page.getByRole("button", { name: "Hủy đăng ký" }).click();
  await expect.poll(() => cancellationPayload).toEqual({ cancellationReason: "Không thể tiếp tục học" });
});

test("admin tạo tài khoản tư vấn viên từ màn hình người dùng", async ({ page }) => {
  await useSession(page, "ADMIN");
  let createdAccount;
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === "/api/auth/me") return json(route, { roleCode: "ADMIN" });
    if (path === "/api/admin/users" && request.method() === "GET") {
      return json(route, { content: [], page: 0, totalPages: 0, totalElements: 0 });
    }
    if (path === "/api/admin/users" && request.method() === "POST") {
      createdAccount = request.postDataJSON();
      return json(route, { id: 20, ...createdAccount, status: "ACTIVE" }, 201);
    }
    return json(route, []);
  });

  await page.goto("/admin/users");
  await page.getByRole("button", { name: "Thêm Admin / Tư vấn viên" }).click();
  await page.getByLabel("Họ tên").fill("Nguyễn Tư Vấn");
  await page.getByLabel("Tên đăng nhập").fill("consultant02");
  await page.getByLabel("Email").fill("consultant02@example.com");
  await page.getByLabel("Mật khẩu ban đầu").fill("Consultant@123");
  await page.getByLabel("Số điện thoại").fill("0901234567");
  await page.getByRole("button", { name: "Tạo tài khoản" }).click();

  await expect.poll(() => createdAccount?.roleCode).toBe("CONSULTANT");
  expect(createdAccount).toMatchObject({
    username: "consultant02",
    fullName: "Nguyễn Tư Vấn",
    email: "consultant02@example.com",
    phoneNumber: "0901234567",
  });
});

test("admin sinh lesson từ lịch cố định của lớp", async ({ page }) => {
  await useSession(page, "ADMIN");
  let generateCalls = 0;
  const courseClass = {
    id: 1, classCode: "EN-A1-01", className: "English A1", courseId: 2,
    courseCode: "EN-A1", courseName: "English A1", levelCode: "A1", teacherId: null,
    teacherName: null, startDate: "2026-09-15", endDate: "2026-12-15", maxStudents: 20,
    enrolledStudents: 2, availableSeats: 18, appliedTuitionFee: 1000000, status: "OPEN",
  };
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === "/api/auth/me") return json(route, { roleCode: "ADMIN" });
    if (path === "/api/admin/classes" && request.method() === "GET") {
      return json(route, { content: [courseClass], page: 0, totalPages: 1, totalElements: 1 });
    }
    if (path === "/api/courses") return json(route, { content: [{ id: 2, courseCode: "EN-A1", courseName: "English A1" }] });
    if (["/api/admin/teachers", "/api/admin/levels", "/api/admin/rooms"].includes(path)) return json(route, []);
    if (path === "/api/admin/classes/1") return json(route, courseClass);
    if (path === "/api/classes/1/schedules") {
      return json(route, [{ id: 5, dayOfWeek: 2, startTime: "18:00", endTime: "20:00", deliveryMode: "ONLINE", meetingUrl: "https://meet.example.com/class" }]);
    }
    if (path === "/api/classes/1/lessons" && request.method() === "GET") return json(route, []);
    if (path === "/api/classes/1/lessons/generate" && request.method() === "POST") {
      generateCalls += 1;
      return json(route, [{ id: 30, lessonDate: "2026-09-15", status: "SCHEDULED" }], 201);
    }
    return json(route, []);
  });
  page.on("dialog", (dialog) => dialog.accept());

  await page.goto("/admin/classes");
  await page.getByRole("button", { name: "Quản lý →" }).click();
  await page.getByRole("button", { name: "＋ Sinh buổi học" }).click();
  await expect.poll(() => generateCalls).toBe(1);
});
