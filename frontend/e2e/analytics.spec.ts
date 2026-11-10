import { expect, test } from "@playwright/test";

test("teacher analytics shows AR chart, rankings and budget", async ({ browser }) => {
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto("/login");
  await page.getByLabel("Email").fill("teacher@demo.local");
  await page.getByLabel("Пароль").fill("Demo123!");
  await page.getByRole("button", { name: "Войти" }).click();
  await page.getByRole("link", { name: "Аналитика" }).click();

  await expect(page.getByRole("heading", { name: "Аналитика" })).toBeVisible();
  await expect(page.locator(".line-chart")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Рейтинг по БУА" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Рейтинг по БОА" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Расход бюджета по месяцам" })).toBeVisible();
  await expect(page.locator(".bar-chart__column")).toHaveCount(6);

  await page.getByLabel("Группа").selectOption({ label: "ИВТ-22" });
  await expect(page.getByRole("heading", { name: /Динамика АР · ИВТ-22/ })).toBeVisible();

  await context.close();
});
