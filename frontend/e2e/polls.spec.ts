import { expect, test } from "@playwright/test";

async function login(page: import("@playwright/test").Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Пароль").fill("Demo123!");
  await page.getByRole("button", { name: "Войти" }).click();
  await expect(page.getByRole("link", { name: "Главная" })).toBeVisible();
}

test("teacher creates anonymous poll, student votes and sees aggregates", async ({ browser }) => {
  const title = `E2E опрос ${Date.now()}`;
  const teacherContext = await browser.newContext();
  const teacherPage = await teacherContext.newPage();
  await login(teacherPage, "teacher@demo.local");

  await teacherPage.getByRole("link", { name: "Опросы", exact: true }).click();
  await teacherPage.getByRole("link", { name: "Создать опрос" }).click();
  await expect(teacherPage).toHaveURL(/\/polls\/new$/);
  await teacherPage.getByLabel("Название опроса").fill(title);
  await teacherPage.getByLabel("Описание").fill("E2E-опрос о формате зачёта");
  await teacherPage.locator(".group-picker__item", { hasText: "ИВТ-21" }).click();
  await teacherPage.getByLabel("Текст вопроса").fill("Предпочтительный формат зачёта");
  await teacherPage.getByLabel("Вопрос 1, вариант 1").fill("Устно");
  await teacherPage.getByLabel("Вопрос 1, вариант 2").fill("Письменно");
  await teacherPage.locator(".checkbox-row input[type=checkbox]").check();
  await teacherPage.getByRole("button", { name: "Создать опрос" }).click();
  await expect(teacherPage).toHaveURL(/\/polls\/\d+$/);
  await expect(teacherPage.getByRole("heading", { name: title })).toBeVisible();
  await expect(teacherPage.getByText("Анонимный опрос")).toBeVisible();

  const studentContext = await browser.newContext();
  const studentPage = await studentContext.newPage();
  await login(studentPage, "student@demo.local");
  await studentPage.getByRole("link", { name: "Опросы", exact: true }).click();
  const card = studentPage.locator(".poll-card", { hasText: title });
  await expect(card).toBeVisible();
  await card.getByRole("link", { name: "Проголосовать" }).click();
  await studentPage.locator(".poll-option", { hasText: "Устно" }).click();
  await studentPage.getByRole("button", { name: "Отправить голос" }).click();
  await expect(studentPage.locator(".alert--success")).toContainText("Голос принят");
  await expect(studentPage.locator(".poll-result").first()).toBeVisible();
  await expect(studentPage.getByText("1 · 100%")).toBeVisible();

  await studentPage.reload();
  await expect(studentPage.getByText("Вы проголосовали")).toBeVisible();
  await expect(studentPage.locator(".poll-result").first()).toBeVisible();

  await teacherPage.reload();
  await expect(teacherPage.getByText("Анонимный опрос")).toBeVisible();
  await expect(teacherPage.locator(".poll-result").first()).toBeVisible();
  await expect(teacherPage.getByRole("heading", { name: "Кто как ответил" })).toHaveCount(0);

  await teacherContext.close();
  await studentContext.close();
});
