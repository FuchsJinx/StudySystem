import { expect, test } from "@playwright/test";

async function login(page: import("@playwright/test").Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Пароль").fill("Demo123!");
  await page.getByRole("button", { name: "Войти" }).click();
  await expect(page.getByRole("link", { name: "Главная" })).toBeVisible();
}

test("AI drafts a checkpoint description and gives student rating tips", async ({ browser }) => {
  const title = `E2E ИИ задача ${Date.now()}`;
  const teacherContext = await browser.newContext();
  const teacherPage = await teacherContext.newPage();
  await login(teacherPage, "teacher@demo.local");
  await teacherPage.getByRole("link", { name: "Задачи", exact: true }).click();
  await teacherPage.getByRole("button", { name: "Создать задачу" }).first().click();
  await teacherPage.getByLabel("Название").fill(title);
  await teacherPage.getByLabel("Тезисы задания").fill(
    "Цель: подготовить первый раздел курсовой. Шаги: введение, обзор литературы, постановка задачи.",
  );
  await teacherPage.getByRole("button", { name: "Сгенерировать описание" }).click();
  const description = teacherPage.getByLabel("Описание");
  await expect(description).not.toHaveValue("");
  await expect(description).toHaveValue(/Цель работы/);
  await expect(teacherPage.getByText(/Демо-режим ИИ/)).toBeVisible();
  await teacherPage.getByRole("button", { name: "Отмена" }).click();

  const studentContext = await browser.newContext();
  const studentPage = await studentContext.newPage();
  await login(studentPage, "student@demo.local");
  await studentPage.getByRole("link", { name: "Профиль", exact: true }).click();
  await studentPage.getByRole("button", { name: "Получить советы" }).click();
  await expect(studentPage.locator(".ai-tips li").first()).toBeVisible();
  expect(await studentPage.locator(".ai-tips li").count()).toBeGreaterThanOrEqual(3);
  await expect(studentPage.getByText(/Демо-режим ИИ/)).toBeVisible();

  await teacherContext.close();
  await studentContext.close();
});
