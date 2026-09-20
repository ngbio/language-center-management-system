import { expect, test } from "@playwright/test";

const course = { id: 1, title: "Tiếng Anh giao tiếp miễn phí", sections: [{ id: 2, title: "Chào hỏi",
  contents: [{ id: 3, title: "Bài 1: Hello", summary: "Ôn lại lời chào.", contentHtml: "<h2>Hello</h2><p>Hello nghĩa là xin chào.</p>", documentUrl: null }] }] };
const card = { id: 5, contentId: 3, frontText: "Hello", backText: "Xin chào", exampleSentence: "Hello, Lan!", status: "ACTIVE", masteryLevel: "NEW", nextReviewAt: null };
const question = { id: 10, questionText: "Hello nghĩa là gì?", questionType: "SINGLE_CHOICE", points: 1,
  options: [{ id: 11, optionText: "Xin chào" }, { id: 12, optionText: "Tạm biệt" }] };
const quiz = { id: 4, title: "Quiz lời chào", passingScore: 50, maxAttempts: 2, status: "PUBLISHED", questions: [question] };
const result = { attemptId: null, score: 100, passed: true, answers: [{ questionId: 10, selectedOptionId: 11, correctOptionId: 11, correct: true, pointsAwarded: 1, explanation: "Hello là lời chào." }] };
const respond = (route, data, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify({ status, message: status === 403 ? "Bạn chưa có quyền học" : "OK", data }) });

async function session(page, role) {
  await page.addInitScript((value) => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
    localStorage.setItem("token", `header.${payload}.signature`);
    localStorage.setItem("role", value);
    localStorage.setItem("email", "student@example.com");
  }, role);
}
async function mockLearning(page, student = false) {
  const calls = [];
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    calls.push({ path, method: request.method(), body: request.postData() });
    if (path.endsWith("/learning/courses/1")) return respond(route, course);
    if (path.endsWith("/contents/3/flashcards")) return respond(route, [card]);
    if (path.endsWith("/contents/3/quizzes")) return respond(route, [quiz]);
    if (path.endsWith("/flashcards/5/review")) return respond(route, { ...card, masteryLevel: "REMEMBERED", nextReviewAt: "2099-01-01T10:00:00" });
    if (path.endsWith("/quizzes/4/attempts") && request.method() === "POST") return respond(route, { id: 9, quiz });
    if (path.endsWith("/quizzes/4/attempts")) return respond(route, [{ id: 9, attemptNumber: 1, score: 100, submittedAt: "2026-09-13T10:00:00", quiz, result }]);
    if (path.endsWith("/evaluate") || path.endsWith("/submit")) return respond(route, { ...result, attemptId: student ? 9 : null });
    if (path.endsWith("/unread-count")) return respond(route, { unreadCount: 0 });
    return respond(route, []);
  });
  return calls;
}

test("khách học free, lật flashcard và làm quiz không tạo lịch sử", async ({ page }) => {
  const calls = await mockLearning(page);
  await page.goto("/on-tap/1");
  await expect(page.getByRole("heading", { name: course.title })).toBeVisible();
  await expect(page.frameLocator("iframe").getByText("Hello nghĩa là xin chào.")).toBeVisible();
  await page.getByRole("button", { name: "Flashcard", exact: true }).click();
  await page.getByRole("button", { name: "Xem đáp án" }).click();
  await expect(page.getByText("Xin chào", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Đã nhớ", exact: true }).click();
  await page.getByRole("button", { name: "Quiz", exact: true }).click();
  await page.getByRole("button", { name: "Làm quiz" }).click();
  await page.getByLabel("Xin chào", { exact: true }).check();
  await page.getByRole("button", { name: "Nộp bài" }).click();
  await expect(page.getByText("Đạt · 100/100")).toBeVisible();
  const sent = calls.find((c) => c.path.endsWith("/evaluate"));
  expect(JSON.parse(sent.body)).toEqual({ answers: [{ questionId: 10, selectedOptionId: 11 }] });
  expect(calls.some((c) => c.path.includes("/students/me/learning"))).toBe(false);
});

test("học viên free lưu lịch ôn và xem lịch sử quiz", async ({ page }) => {
  await session(page, "STUDENT");
  const calls = await mockLearning(page, true);
  await page.goto("/on-tap/1");
  await page.getByRole("button", { name: "Flashcard", exact: true }).click();
  await page.getByRole("button", { name: "Xem đáp án" }).click();
  await page.getByRole("button", { name: "Đã nhớ", exact: true }).click();
  await expect(page.getByText(/Lần ôn tiếp theo/)).toBeVisible();
  await page.getByRole("button", { name: "Quiz", exact: true }).click();
  await page.getByRole("button", { name: "Làm quiz" }).click();
  await page.getByLabel("Xin chào", { exact: true }).check();
  await page.getByRole("button", { name: "Nộp bài" }).click();
  await expect(page.getByText("Đạt · 100/100")).toBeVisible();
  await page.getByRole("button", { name: "Lịch sử", exact: true }).click();
  await page.getByRole("button", { name: /Lần 1/ }).click();
  await expect(page.getByText("Chính xác. Hello là lời chào.")).toBeVisible();
  expect(calls.some((c) => c.path.endsWith("/flashcards/5/review"))).toBe(true);
  expect(calls.some((c) => c.path.endsWith("/attempts/9/submit"))).toBe(true);
  expect(calls.some((c) => c.path.includes("/enrollments") || c.path.includes("/payments"))).toBe(false);
});

test("khóa trả phí bị chặn không tải bài tập", async ({ page }) => {
  await page.route("**/api/**", (route) => respond(route, null, 403));
  await page.goto("/on-tap/1");
  await expect(page.getByRole("alert")).toContainText("Bạn chưa có quyền học");
  await expect(page.getByRole("button", { name: "Flashcard", exact: true })).toHaveCount(0);
});

for (const width of [320, 390, 768, 1280]) {
  test(`giao diện học không tràn ngang ở ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await mockLearning(page);
    await page.goto("/on-tap/1");
    await page.getByRole("button", { name: "Flashcard", exact: true }).click();
    await page.getByRole("button", { name: "Xem đáp án" }).click();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await page.screenshot({ path: `test-results/learning-${width}.png`, fullPage: true });
  });
}

test("admin tạo flashcard và quiz nháp, thêm câu hỏi rồi xuất bản", async ({ page }) => {
  await session(page, "ADMIN");
  let cards = [], quizzes = [], questionSaved = false, published = false;
  await page.route("**/api/**", async (route) => {
    const req = route.request(), path = new URL(req.url()).pathname;
    if (path === "/api/auth/me") return respond(route, { roleCode: "ADMIN", fullName: "Admin" });
    if (path === "/api/admin/contents/3") return respond(route, { id: 3, title: "Bài 1" });
    if (path.endsWith("/contents/3/flashcards")) {
      if (req.method() === "POST") cards = [{ ...req.postDataJSON(), id: 5 }];
      return respond(route, cards);
    }
    if (path.endsWith("/contents/3/quizzes")) {
      if (req.method() === "POST") quizzes = [{ ...req.postDataJSON(), id: 4, locked: false, questions: [] }];
      return respond(route, quizzes);
    }
    if (path.endsWith("/quizzes/4/questions")) {
      questionSaved = true;
      quizzes[0].questions = [{ ...req.postDataJSON(), id: 10, options: req.postDataJSON().options.map((o, i) => ({ ...o, id: i + 11 })) }];
      return respond(route, quizzes[0]);
    }
    if (path.endsWith("/quizzes/4") && req.method() === "PUT") {
      quizzes[0] = { ...quizzes[0], ...req.postDataJSON() }; published = quizzes[0].status === "PUBLISHED";
      return respond(route, quizzes[0]);
    }
    return respond(route, []);
  });
  await page.goto("/admin/courses/1/contents/3/practice");
  await page.getByRole("button", { name: "Thêm flashcard" }).click();
  await page.getByLabel("Mặt trước", { exact: true }).fill("Hello");
  await page.getByLabel("Mặt sau", { exact: true }).fill("Xin chào");
  await page.getByRole("button", { name: "Lưu flashcard" }).click();
  await expect(page.getByText("Hello", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Thêm quiz", exact: true }).click();
  await page.getByLabel("Tên quiz").fill("Quiz mới");
  await page.getByRole("button", { name: "Lưu quiz", exact: true }).click();
  await page.getByRole("button", { name: "Câu hỏi", exact: true }).click();
  await page.getByRole("button", { name: "Thêm câu hỏi", exact: true }).click();
  await page.getByLabel("Nội dung câu hỏi").fill("Hello?");
  await page.getByLabel("Lựa chọn 1", { exact: true }).fill("Xin chào");
  await page.getByLabel("Lựa chọn 2", { exact: true }).fill("Tạm biệt");
  await page.getByRole("button", { name: "Lưu câu hỏi", exact: true }).click();
  await expect.poll(() => questionSaved).toBe(true);
  await page.getByRole("button", { name: "Cài đặt quiz", exact: true }).click();
  await page.getByLabel("Trạng thái quiz").selectOption("PUBLISHED");
  await page.getByRole("button", { name: "Lưu quiz", exact: true }).click();
  await expect.poll(() => published).toBe(true);
});
