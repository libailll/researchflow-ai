import type { MemberRole, ProjectStatus, TaskPriority, TaskStatus } from "@/types/model";

export const PROJECT_STATUS: Record<ProjectStatus, string> = { PLANNING: "筹备中", RUNNING: "进行中", PAUSED: "已暂停", COMPLETED: "已完成", CANCELLED: "已取消" };
export const TASK_STATUS: Record<TaskStatus, string> = { TODO: "待开始", IN_PROGRESS: "进行中", REVIEW: "待审核", DONE: "已完成", CANCELLED: "已取消" };
export const TASK_PRIORITY: Record<TaskPriority, string> = { LOW: "低", MEDIUM: "中", HIGH: "高", URGENT: "紧急" };
export const MEMBER_ROLE: Record<MemberRole, string> = { OWNER: "所有者", ADMIN: "管理员", MEMBER: "成员" };
export const BOARD_COLUMNS: { status: TaskStatus; title: string }[] = [
  { status: "TODO", title: "待开始" },
  { status: "IN_PROGRESS", title: "进行中" },
  { status: "REVIEW", title: "待审核" },
  { status: "DONE", title: "已完成" },
];

