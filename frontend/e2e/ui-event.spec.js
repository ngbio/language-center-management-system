import { expect, test } from "@playwright/test";
import process from "node:process";

const baseURL = process.env.QA_EVENT_BASE_URL;
const event = process.env.QA_EXPECTED_EVENT || "christmas";
const targets = [
  ["/", null, ".hero-visual"],
  ["/khoa-hoc", null, ".catalog-hero"],
  ["/login", null, ".student-login-form"],
  ["/register", null, ".student-login-form"],
  ["/admin/login", null, ".login-form"],
  ["/staff/login", null, ".login-form"],
  ["/giao-vien/khoa-hoc", "TEACHER", ".my-courses-hero"],
  ["/admin/users", "ADMIN", ".admin-shell"],
  ["/staff/refunds", "CONSULTANT", ".admin-shell"],
  ["/on-tap/1", null, ".learning-topbar"],
];

test("Christmas window follows theme, snow preference and manual music playback", async ({ page }) => {
  test.skip(event !== "christmas", "Seasonal presentation is disabled");
  await page.addInitScript(() => {
    if (!localStorage.getItem("publicTheme")) localStorage.setItem("publicTheme", "light");
  });
  await page.route("**/api/**", (route) => route.fulfill({ contentType: "application/json", body: JSON.stringify({ status: 200, data: [] }) }));
  await page.goto(baseURL ? `${baseURL}/` : "/");
  const garden = page.locator(".christmas-window-garden");
  await expect(garden).toHaveCSS("background-image", /garden-day/);
  await page.getByRole("button", { name: "Chuyển sang giao diện tối" }).click();
  await expect(garden).toHaveCSS("background-image", /garden-night/);
  await page.getByRole("button", { name: /Tắt tuyết/ }).click();
  await expect(page.locator(".christmas-window-snow")).toBeHidden();
  await page.reload();
  await expect(garden).toHaveCSS("background-image", /garden-night/);
  await expect(page.locator(".christmas-window-snow")).toBeHidden();
  const audio = page.locator("audio");
  expect(await audio.evaluate((element) => element.paused)).toBe(true);
  await page.getByRole("button", { name: "Bật nhạc Giáng sinh" }).click();
  await expect(page.getByRole("button", { name: "Tắt nhạc Giáng sinh" })).toHaveAttribute("aria-pressed", "true");
  expect(await audio.evaluate((element) => element.paused)).toBe(false);
  await page.getByRole("button", { name: "Tắt nhạc Giáng sinh" }).click();
  expect(await audio.evaluate((element) => element.paused)).toBe(true);
  await page.getByRole("button", { name: /Bật tuyết/ }).click();
  await expect(page.locator(".christmas-window-snow")).toBeVisible();
  await page.emulateMedia({ reducedMotion: "reduce" });
  await expect(page.locator(".christmas-window-snow")).toBeHidden();
});

for (const [path, role, selector] of targets) {
  test(`${event}: ${path} giữ chức năng và bật/tắt đúng trang trí`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.addInitScript((role) => {
      localStorage.setItem("publicTheme", "light");
      if (!role) return;
      const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
      localStorage.setItem("token", `header.${payload}.signature`);
      localStorage.setItem("role", role);
      localStorage.setItem("email", "qa@example.com");
    }, role);
    await page.route("**/api/**", (route) => {
      const pathname = new URL(route.request().url()).pathname;
      let data = [];
      if (pathname.endsWith("/auth/me")) data = { roleCode: role, fullName: "QA" };
      if (pathname.endsWith("/users")) data = { content: [], totalPages: 0, totalElements: 0 };
      if (pathname.endsWith("/learning/courses/1")) data = {
        title: "Kana", sections: [{ id: 1, title: "Hiragana", contents: [{ id: 3, title: "Bài học", contentHtml: "<p>Học ngoại ngữ</p>" }] }],
      };
      return route.fulfill({ contentType: "application/json", body: JSON.stringify({ status: 200, data }) });
    });
    await page.goto(baseURL ? `${baseURL}${path}` : path, { waitUntil: "domcontentloaded" });
    await expect(page.locator(selector)).toBeVisible();
    await expect(page.locator("html")).toHaveAttribute("data-ui-event", event);
    if (event === "none") {
      await expect(page.locator(".christmas-greeting, .christmas-scene, .seasonal-accent")).toHaveCount(0);
      if (path === "/") await expect(page.locator(".hero-card-main")).toBeVisible();
    } else if (["/admin/users", "/staff/refunds", "/on-tap/1"].includes(path)) {
      await expect(page.locator(".seasonal-accent")).toBeVisible();
    }
    if (path === "/login") {
      const content = await page.locator(".student-login-form").evaluate((element) => getComputedStyle(element, "::after").content);
      expect(content === "none").toBe(event === "none");
      if (event === "christmas") await expect(page.locator(".student-login-visual h1")).not.toHaveCSS("color", "rgb(255, 255, 255)");
    }
    await page.screenshot({ path: `test-results/ui-event-${event}-${path.replaceAll("/", "-")}.png` });
    await page.setViewportSize({ width: 390, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  });
}
