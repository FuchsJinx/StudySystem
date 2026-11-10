import { expect, test } from "@playwright/test";

async function login(page: import("@playwright/test").Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Пароль").fill("Demo123!");
  await page.getByRole("button", { name: "Войти" }).click();
  await expect(page.getByRole("link", { name: "Главная" })).toBeVisible();
}

test("admin catalog CRUD and settings changes", async ({ browser }) => {
  const suffix = Date.now().toString().slice(-6);
  const facultyName = `E2E-фак-${suffix}`;
  const groupName = `E2E-гр-${suffix}`;

  const context = await browser.newContext();
  const page = await context.newPage();
  await login(page, "admin@demo.local");
  await page.getByRole("link", { name: "Администрирование" }).click();
  await expect(page.getByRole("heading", { name: "Панель администратора" })).toBeVisible();
  await expect(page.getByText("Студенты", { exact: true })).toBeVisible();

  const facultiesCard = page.locator(".card", { hasText: "Факультеты" }).first();
  await facultiesCard.getByLabel("Новый факультет").fill(facultyName);
  await facultiesCard.getByRole("button", { name: "Добавить" }).click();
  await expect(facultiesCard.getByText(facultyName)).toBeVisible();

  const groupsCard = page.locator(".card", { hasText: "Группы" }).first();
  await groupsCard.getByLabel("Новая группа").fill(groupName);
  await groupsCard.getByLabel("Факультет группы").selectOption({ label: facultyName });
  await groupsCard.getByRole("button", { name: "Добавить" }).click();
  await expect(groupsCard.getByText(groupName)).toBeVisible();

  const groupRow = groupsCard.locator("li", { hasText: groupName });
  await groupRow.getByRole("button", { name: "Удалить" }).click();
  await page.getByRole("button", { name: "Подтвердить" }).click();
  await expect(groupsCard.getByText(groupName)).toHaveCount(0);

  const facultyRow = facultiesCard.locator("li", { hasText: facultyName });
  await facultyRow.getByRole("button", { name: "Удалить" }).click();
  await page.getByRole("button", { name: "Подтвердить" }).click();
  await expect(facultiesCard.getByText(facultyName)).toHaveCount(0);

  await page.getByRole("tab", { name: "Настройки" }).click();
  const amountField = page.getByLabel(/Размер списания АР/);
  await expect(amountField).toBeVisible();
  await amountField.fill("25");
  await page.getByRole("button", { name: "Сохранить настройки" }).click();
  await expect(page.getByText("Настройки сохранены")).toBeVisible();
  await amountField.fill("20");
  await page.getByRole("button", { name: "Сохранить настройки" }).click();

  await context.close();
});

test("admin imports students from CSV via UI", async ({ browser }) => {
  const suffix = Date.now().toString().slice(-6);
  const email = `e2e-import-${suffix}@test.local`;
  const facultyName = `E2E-импорт-${suffix}`;
  const groupName = `E2E-ИМП-${suffix}`;
  const csv = [
    "email,fullName,facultyName,groupName,passwordOptional",
    `${email},Импортов Иван,${facultyName},${groupName},`,
  ].join("\n");

  const context = await browser.newContext();
  const page = await context.newPage();
  await login(page, "admin@demo.local");
  await page.getByRole("link", { name: "Администрирование" }).click();
  await page.getByRole("tab", { name: "Импорт" }).click();
  await page.getByLabel("Файл").setInputFiles({
    name: "students.csv",
    mimeType: "text/csv",
    buffer: Buffer.from(csv, "utf-8"),
  });
  await page.getByRole("button", { name: "Импортировать" }).click();
  await expect(page.getByText("Создано", { exact: true })).toBeVisible();
  await expect(page.getByText("Ошибок нет")).toBeVisible();

  await page.getByRole("tab", { name: "Пользователи" }).click();
  await page.getByLabel("Роль").selectOption("STUDENT");
  await expect(page.getByRole("cell", { name: email })).toBeVisible();

  await context.close();
});
