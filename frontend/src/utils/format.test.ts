import { describe, expect, it } from "vitest";
import { formatDate, initials, isOverdue } from "./format";
import type { ResearchTask } from "@/types/model";

describe("format utilities", () => {
  it("formats dates for the interface", () => {
    expect(formatDate("2026-08-16")).toBe("2026.08.16");
    expect(formatDate()).toBe("未设置");
  });

  it("creates stable display initials", () => {
    expect(initials("研究员")).toBe("研究");
    expect(initials()).toBe("研");
  });

  it("only marks unfinished past-due tasks as overdue", () => {
    const task = { dueDate: "2000-01-01", status: "TODO" } as ResearchTask;
    expect(isOverdue(task)).toBe(true);
    expect(isOverdue({ ...task, status: "DONE" })).toBe(false);
  });
});
