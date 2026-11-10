import { describe, expect, it } from "vitest";
import {
  formatAmount,
  formatDateTime,
  initials,
  membershipLabels,
  plural,
  statusLabels,
} from "./format";

describe("format helpers", () => {
  it("renders Russian status labels", () => {
    expect(statusLabels.IN_PROGRESS).toBe("В процессе");
    expect(statusLabels.SUBMITTED).toBe("Сдано на проверку");
    expect(statusLabels.OVERDUE).toBe("Просрочено");
    expect(statusLabels.ACCEPTED).toBe("Принято");
    expect(statusLabels.RETURNED).toBe("На доработке");
  });

  it("renders membership labels", () => {
    expect(membershipLabels.PENDING).toBe("Ожидает подтверждения");
    expect(membershipLabels.CONFIRMED).toBe("Подтверждён");
  });

  it("formats signed point amounts", () => {
    expect(formatAmount("BOA", 50)).toBe("+50 БОА");
    expect(formatAmount("AR", -20)).toBe("-20 АР");
    expect(formatAmount("BUA", 0)).toBe("0 БУА");
  });

  it("selects plural form", () => {
    expect(plural(1, "заявка", "заявки", "заявок")).toBe("заявка");
    expect(plural(3, "заявка", "заявки", "заявок")).toBe("заявки");
    expect(plural(11, "заявка", "заявки", "заявок")).toBe("заявок");
    expect(plural(25, "заявка", "заявки", "заявок")).toBe("заявок");
  });

  it("builds initials and handles empty date", () => {
    expect(initials("Иван Иванов")).toBe("ИИ");
    expect(formatDateTime(null)).toBe("—");
  });
});
