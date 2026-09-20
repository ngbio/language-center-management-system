import { expect, test } from "@playwright/test";

for (const width of [320, 390, 768, 1280]) {
  for (const account of ["Học viên", "Giáo viên"]) {
    test(`registration password toggle: ${account} at ${width}px`, async ({ page }) => {
      await page.setViewportSize({ width, height: 844 });
      await page.goto("/register");
      await page.getByRole("button", { name: account, exact: true }).click();

      const input = page.locator('input[name="password"]');
      const toggle = page.locator(".password-toggle");
      await input.fill("Example123!");
      await expect(input).toHaveAttribute("type", "password");
      await expect(toggle).toHaveCount(1);
      await expect(toggle.locator("svg")).toHaveCount(1);
      await expect(toggle).toHaveAccessibleName("Hiện mật khẩu");
      await expect(toggle).toHaveAttribute("aria-pressed", "false");

      await toggle.click();
      await expect(input).toHaveAttribute("type", "text");
      await expect(input).toHaveValue("Example123!");
      await expect(toggle).toHaveAccessibleName("Ẩn mật khẩu");
      await expect(toggle).toHaveAttribute("aria-pressed", "true");
      await expect(toggle.locator("svg")).toHaveCount(1);

      await toggle.click();
      await expect(input).toHaveAttribute("type", "password");
      await expect(input).toHaveValue("Example123!");
      await expect(toggle).toHaveAccessibleName("Hiện mật khẩu");
      await expect(toggle.locator("svg")).toHaveCount(1);
    });
  }
}
