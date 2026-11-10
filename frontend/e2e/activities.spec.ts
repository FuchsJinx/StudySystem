import { expect, test } from "@playwright/test";

async function login(page: import("@playwright/test").Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Пароль").fill("Demo123!");
  await page.getByRole("button", { name: "Войти" }).click();
  await expect(page.getByRole("link", { name: "Главная" })).toBeVisible();
}

test("social activity: enrollment, award, public feed and profile points", async ({ browser }) => {
  const title = `E2E активность ${Date.now()}`;
  const teacherContext = await browser.newContext();
  const teacherPage = await teacherContext.newPage();
  await login(teacherPage, "teacher@demo.local");

  await teacherPage.getByRole("link", { name: "Активности", exact: true }).click();
  await expect(teacherPage).toHaveURL(/\/activities$/);
  await teacherPage.getByRole("button", { name: "Создать активность" }).first().click();
  await teacherPage.getByLabel("Название").fill(title);
  await teacherPage.getByLabel("Описание").fill("E2E-проверка общественной активности");
  await teacherPage.getByRole("button", { name: /Общественная/ }).click();
  await teacherPage.getByLabel("Максимум баллов").fill("100");
  await teacherPage.locator(".group-picker__item", { hasText: "ИВТ-21" }).click();
  await teacherPage.getByRole("button", { name: "Создать", exact: true }).click();
  await expect(teacherPage).toHaveURL(/\/activities\/\d+$/);
  await expect(teacherPage.getByRole("heading", { name: title })).toBeVisible();

  const studentContext = await browser.newContext();
  const studentPage = await studentContext.newPage();
  await login(studentPage, "student@demo.local");
  await studentPage.getByRole("link", { name: "Активности", exact: true }).click();
  const card = studentPage.locator(".activity-card", { hasText: title });
  await expect(card).toBeVisible();
  await card.getByRole("button", { name: "Записаться" }).click();
  await expect(studentPage.getByText(`Вы записаны на «${title}»`)).toBeVisible();

  await teacherPage.reload();
  const row = teacherPage.locator("tr", { hasText: "Иван Иванов" });
  await expect(row.getByText("Записан")).toBeVisible();
  await row.getByRole("checkbox").check();
  await teacherPage.getByRole("button", { name: /Начислить выбранным/ }).click();
  await teacherPage.getByRole("button", { name: "Начислить", exact: true }).click();
  await expect(teacherPage.getByText(/Начислено 50 баллов/)).toBeVisible();

  await studentPage.goto("/feed");
  const feedEntry = studentPage.locator(".award-item", { hasText: title });
  await expect(feedEntry).toBeVisible();
  await expect(feedEntry.getByText("+50 БОА")).toBeVisible();

  await studentPage.goto("/profile");
  const boaText = await studentPage.locator(".stat-simple--gold strong").innerText();
  expect(Number(boaText)).toBeGreaterThanOrEqual(50);

  await teacherContext.close();
  await studentContext.close();
});
