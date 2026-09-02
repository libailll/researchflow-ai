import type { ResearchTask } from "@/types/model";

export const formatDate = (value?: string) => value ? value.slice(0, 10).replaceAll("-", ".") : "未设置";
export const initials = (value?: string) => (value || "研").trim().slice(0, 2).toUpperCase();
export const isOverdue = (task: ResearchTask) => Boolean(task.dueDate && !["DONE", "CANCELLED"].includes(task.status) && new Date(task.dueDate) < new Date(new Date().toDateString()));

