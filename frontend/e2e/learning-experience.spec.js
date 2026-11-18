import { expect, test } from "@playwright/test";

const fulfill = (route, data) => route.fulfill({
  contentType: "application/json",
  body: JSON.stringify({ status: 200, data }),
});

test("quiz giữ lựa chọn khi chuyển câu và gửi toàn bộ đáp án", async ({ page }) => {
  const questions = [1, 2].map((id) => ({
    id, questionText: `Câu hỏi ${id}`, points: 1,
    options: [{ id: id * 10, optionText: `Lựa chọn ${id}` }, { id: id * 10 + 1, optionText: "Khác" }],
  }));
  const quiz = { id: 4, title: "Quiz hai câu", passingScore: 50, questions };
  let submitted;
  await page.route("**/api/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/learning/courses/1")) return fulfill(route, { title: "Tiếng Nhật", sections: [{ id: 1, title: "Nhập môn", contents: [{ id: 3, title: "Kana" }] }] });
    if (path.endsWith("/contents/3/quizzes")) return fulfill(route, [quiz]);
    if (path.endsWith("/evaluate")) {
      submitted = route.request().postDataJSON();
      return fulfill(route, { passed: true, score: 100, answers: [] });
    }
    return fulfill(route, []);
  });
  await page.goto("/on-tap/1");
  await page.getByRole("button", { name: "Quiz", exact: true }).click();
  await page.getByRole("button", { name: "Làm quiz" }).click();
  await page.getByRole("radio", { name: "Lựa chọn 1", exact: true }).check();
  await page.getByRole("button", { name: "Câu tiếp theo" }).click();
  await page.getByRole("radio", { name: "Lựa chọn 2", exact: true }).check();
  await page.getByRole("button", { name: "Câu trước" }).click();
  await expect(page.getByRole("radio", { name: "Lựa chọn 1", exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Câu tiếp theo" }).click();
  await page.getByRole("button", { name: "Nộp bài" }).click();
  await expect(page.getByText("Đạt · 100/100")).toBeVisible();
  expect(submitted.answers).toEqual([{ questionId: 1, selectedOptionId: 10 }, { questionId: 2, selectedOptionId: 20 }]);
});

for (const width of [390, 1440]) {
  test(`chi tiết khóa học giữ CTA học miễn phí và không tràn ở ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 });
    await page.addInitScript(() => localStorage.setItem("publicTheme", "light"));
    await page.route("**/api/**", (route) => {
      const path = new URL(route.request().url()).pathname;
      if (path.includes("/courses/slug/")) return fulfill(route, {
        id: 1, courseName: "Hiragana & Katakana", courseCode: "JA-N5-KANA-BASIC",
        languageCode: "JA", languageName: "Tiếng Nhật", levelName: "N5 · Nhập môn",
        tuitionFee: 0, totalSessions: 6, durationHours: 12,
        shortDescription: "Khởi đầu nhẹ nhàng với hai bảng chữ cái nền tảng của tiếng Nhật.",
        learningOutcomes: "Nhận diện và đọc bảng chữ Hiragana\nLàm quen với bảng chữ Katakana\nÔn tập qua flashcard và quiz",
        targetAudience: "Người mới bắt đầu học tiếng Nhật.", prerequisites: "Không yêu cầu kiến thức trước đó.",
      });
      if (path.endsWith("/courses/1/sections")) return fulfill(route, [{ id: 2, displayOrder: 1, title: "Làm quen bảng chữ cái" }, { id: 3, displayOrder: 2, title: "Luyện tập mỗi ngày" }]);
      if (path.endsWith("/sections/2/contents")) return fulfill(route, [{ id: 3, title: "Hàng nguyên âm", summary: "あ・い・う・え・お", contentType: "LESSON", contentHtml: "<p>Năm nguyên âm cơ bản.</p>" }]);
      return fulfill(route, []);
    });
    await page.goto("/khoa-hoc/kana");
    await expect(page.getByRole("heading", { name: "Hiragana & Katakana" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Học miễn phí ngay" })).toHaveAttribute("href", "/on-tap/1");
    await page.getByRole("button", { name: /Làm quen bảng chữ cái/ }).click();
    await expect(page.getByText("Hàng nguyên âm", { exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: `test-results/course-detail-${width}.png`, fullPage: true });
  });
}
