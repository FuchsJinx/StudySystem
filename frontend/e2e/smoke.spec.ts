import { expect, test } from "@playwright/test";

async function login(page: import("@playwright/test").Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Пароль").fill("Demo123!");
  await page.getByRole("button", { name: "Войти" }).click();
  await expect(page.getByRole("link", { name: "Главная" })).toBeVisible();
}

test("teacher creates checkpoint and student submits it", async ({ browser }) => {
  const title = `E2E задача ${Date.now()}`;
  const teacherContext = await browser.newContext();
  const teacherPage = await teacherContext.newPage();
  await login(teacherPage, "teacher@demo.local");

  await teacherPage.getByRole("link", { name: "Задачи", exact: true }).click();
  await expect(teacherPage).toHaveURL(/\/checkpoints$/);
  await teacherPage.getByRole("button", { name: "Создать задачу" }).first().click();
  await teacherPage.getByLabel("Название").fill(title);
  await teacherPage.getByLabel("Описание").fill("Задача, созданная автоматическим E2E-тестом");
  await teacherPage.locator(".group-picker__item", { hasText: "ИВТ-21" }).click();
  await teacherPage.getByRole("button", { name: "Создать задачу" }).last().click();
  await expect(teacherPage).toHaveURL(/\/checkpoints\/\d+$/);
  await expect(teacherPage.getByRole("heading", { name: title })).toBeVisible();

  const studentContext = await browser.newContext();
  const studentPage = await studentContext.newPage();
  await login(studentPage, "student@demo.local");
  await studentPage.getByRole("link", { name: "Мои задачи" }).click();
  await expect(studentPage).toHaveURL(/\/tasks$/);
  const taskCard = studentPage.locator(".task-card", { hasText: title });
  await expect(taskCard).toBeVisible();
  await taskCard.getByRole("button", { name: "Сдать работу" }).click();
  await studentPage.getByLabel("Ссылка на работу").fill("https://example.com/e2e-work");
  await studentPage.getByRole("button", { name: "Отправить на проверку" }).click();
  await expect(studentPage.getByText("Работа по")).toBeVisible();

  await teacherPage.reload();
  const row = teacherPage.locator("tr", { hasText: "Иван Иванов" });
  await expect(row.getByText("Сдано на проверку")).toBeVisible();
  await row.getByRole("checkbox").check();
  await teacherPage.getByRole("button", { name: /Принять выбранные/ }).click();
  await teacherPage.getByRole("button", { name: "Принять", exact: true }).click();
  await expect(teacherPage.getByText(/Принято работ: 1/)).toBeVisible();

  await studentPage.reload();
  await expect(studentPage.locator(".task-card", { hasText: title }).getByText("Принято")).toBeVisible();

  await teacherContext.close();
  await studentContext.close();
});

test("student cannot see teacher page", async ({ browser }) => {
  const context = await browser.newContext();
  const page = await context.newPage();
  await login(page, "student@demo.local");
  await page.goto("/checkpoints");
  await expect(page).toHaveURL("http://127.0.0.1:5173/");
  await context.close();
});
