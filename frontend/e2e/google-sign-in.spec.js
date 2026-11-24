import { expect, test } from "@playwright/test";

const response = (data, status = 200, message = "OK") => ({
  status,
  contentType: "application/json",
  body: JSON.stringify({ status, message, data }),
});

async function mockGooglePopup(page, errorCode) {
  // Exercise the real UI/backend exchange without credentials or a live Google account.
  await page.route("**/src/services/googleAuth.js*", (route) => route.fulfill({
    contentType: "application/javascript",
    body: errorCode
      ? `export async function getGoogleIdToken(){throw Object.assign(new Error('Popup cancelled'),{code:${JSON.stringify(errorCode)}})}; export function googleAuthError(e){return e.message}`
      : "export async function getGoogleIdToken(){return 'verified-firebase-id-token'}; export function googleAuthError(e){return e.response?.data?.message || e.message}",
  }));
  await page.route("**/api/**", (route) => route.fulfill(response([])));
}

function sessionData() {
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
  return { token: `header.${payload}.signature`, userId: 7, email: "student@example.com", roleCode: "STUDENT", role: "Học viên" };
}

test("Google login exchanges Firebase token for application session", async ({ page }) => {
  await mockGooglePopup(page);
  let payload;
  const data = sessionData();
  await page.route("**/api/auth/google", (route) => {
    payload = route.request().postDataJSON();
    return route.fulfill(response(data));
  });
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.getByRole("button", { name: "Tiếp tục với Google" }).click();
  await expect(page).toHaveURL(/\/$/);
  expect(payload).toEqual({ idToken: "verified-firebase-id-token" });
  expect(await page.evaluate(() => ({ token: localStorage.getItem("token"), role: localStorage.getItem("role") })))
    .toEqual({ token: data.token, role: "STUDENT" });
});

test("existing account requires password, wrong password never creates a session", async ({ page }) => {
  await mockGooglePopup(page);
  const payloads = [];
  await page.route("**/api/auth/google", (route) => {
    const payload = route.request().postDataJSON();
    payloads.push(payload);
    if (!payload.password) return route.fulfill(response({ requiresPassword: true }, 409, "Xác nhận liên kết"));
    if (payload.password !== "Correct123!") return route.fulfill(response(null, 401, "Mật khẩu xác nhận liên kết không đúng."));
    return route.fulfill(response(sessionData()));
  });
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.getByRole("button", { name: "Tiếp tục với Google" }).click();
  await expect(page.getByLabel("Mật khẩu hiện tại", { exact: true })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem("token"))).toBeNull();
  await page.getByLabel("Mật khẩu hiện tại", { exact: true }).fill("Wrong123!");
  await page.getByRole("button", { name: "Xác nhận liên kết Google" }).click();
  await expect(page.getByRole("alert")).toContainText("Mật khẩu xác nhận liên kết không đúng");
  expect(await page.evaluate(() => localStorage.getItem("token"))).toBeNull();
  await page.getByLabel("Mật khẩu hiện tại", { exact: true }).fill("Correct123!");
  await page.getByLabel("Mật khẩu hiện tại", { exact: true }).press("Enter");
  await expect(page).toHaveURL(/\/$/);
  expect(payloads[2]).toEqual({ idToken: "verified-firebase-id-token", password: "Correct123!" });
});

test("popup cancellation leaves ordinary email login available", async ({ page }) => {
  await mockGooglePopup(page, "auth/popup-closed-by-user");
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.getByRole("button", { name: "Tiếp tục với Google" }).click();
  await expect(page.getByRole("button", { name: "Đăng nhập", exact: true })).toBeEnabled();
  await expect(page.locator(".google-sign-in [role=alert]")).toHaveCount(0);
  expect(await page.evaluate(() => localStorage.getItem("token"))).toBeNull();
});

test("Google registration is offered to students, not teacher applications", async ({ page }) => {
  await mockGooglePopup(page);
  await page.goto("/register", { waitUntil: "domcontentloaded" });
  await expect(page.getByRole("button", { name: "Tiếp tục với Google" })).toBeVisible();
  await page.getByRole("button", { name: "Giáo viên", exact: true }).click();
  await expect(page.getByRole("button", { name: "Tiếp tục với Google" })).toHaveCount(0);
});

test("locked account error does not persist Firebase token or application session", async ({ page }) => {
  await mockGooglePopup(page);
  await page.route("**/api/auth/google", (route) => route.fulfill(response(null, 401, "Tài khoản không ở trạng thái hoạt động.")));
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.getByRole("button", { name: "Tiếp tục với Google" }).click();
  await expect(page.getByRole("alert")).toContainText("Tài khoản không ở trạng thái hoạt động");
  expect(await page.evaluate(() => Object.values(localStorage))).not.toContain("verified-firebase-id-token");
  expect(await page.evaluate(() => localStorage.getItem("token"))).toBeNull();
});
