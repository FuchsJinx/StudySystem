import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EmptyState, MembershipBadge, ProgressBar, StatusBadge } from "./index";

describe("UI kit", () => {
  it("renders Russian submission status", () => {
    render(<StatusBadge status="OVERDUE" />);
    expect(screen.getByText("Просрочено")).toBeTruthy();
  });

  it("renders membership status", () => {
    render(<MembershipBadge status="CONFIRMED" />);
    expect(screen.getByText("Подтверждён")).toBeTruthy();
  });

  it("renders empty state title and description", () => {
    render(<EmptyState title="Нет данных" description="Пока пусто" />);
    expect(screen.getByText("Нет данных")).toBeTruthy();
    expect(screen.getByText("Пока пусто")).toBeTruthy();
  });

  it("renders progress bar with accessible values", () => {
    const { container } = render(<ProgressBar value={250} max={1000} label="АР" />);
    expect(screen.getByText("250 / 1000")).toBeTruthy();
    const fill = container.querySelector(".progress__fill") as HTMLElement;
    expect(fill.style.width).toBe("25%");
  });
});
