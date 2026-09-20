import { expect, test } from "@playwright/test";

const response = (data) => ({
  status: 200,
  contentType: "application/json",
  body: JSON.stringify({ status: 200, message: "OK", data }),
});

async function mockPublicApi(page) {
  await page.route("**/api/**", (route) => route.fulfill(response([])));
}

async function mockAdminApi(page) {
  await page.addInitScript(() => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
    localStorage.setItem("token", `header.${payload}.signature`);
    localStorage.setItem("role", "ADMIN");
    localStorage.setItem("email", "admin@example.com");
  });
  await page.route("**/api/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/auth/me") {
      return route.fulfill(response({ roleCode: "ADMIN", fullName: "Administrator" }));
    }
    if (path === "/api/admin/dashboard/summary") {
      return route.fulfill(response({ users: 0, courses: 0, classes: 0, enrollments: 0 }));
    }
    return route.fulfill(response([]));
  });
}

for (const width of [320, 390, 768]) {
  test(`public layout usable at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 844 });
    await mockPublicApi(page);
    await page.goto("/");

    await expect(page.getByRole("link", { name: "Tải ứng dụng Android" })).toHaveAttribute(
      "href",
      "/downloads/lingua-center-1.0.0.apk",
    );
    await expect(page.locator(".public-menu-button")).toBeVisible();
    await page.locator(".public-menu-button").click();
    await expect(page.locator(".public-main-nav")).toHaveClass(/is-open/);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await page.screenshot({ path: `test-results/mobile-public-${width}.png`, fullPage: true });
  });

  test(`admin layout usable at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 844 });
    await mockAdminApi(page);
    await page.goto("/admin");

    const menu = page.locator(".menu-button");
    await expect(menu).toBeVisible();
    await menu.click();
    await expect(page.locator(".sidebar")).toHaveClass(/is-open/);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await page.screenshot({ path: `test-results/mobile-admin-${width}.png`, fullPage: true });
  });
}
