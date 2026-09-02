import { apiRequest } from "./http";

export type NotificationType =
  | "TASK_ASSIGNED"
  | "TASK_UPDATED"
  | "TASK_STATUS_CHANGED"
  | "TASK_DUE_SOON"
  | "TASK_OVERDUE"
  | "WEEKLY_REPORT_READY"
  | "DOCUMENT_SUMMARY_READY"
  | "RISK_REPORT_READY";

export interface UserNotification {
  id: number;
  projectId?: number;
  type: NotificationType;
  title: string;
  content: string;
  targetType?: string;
  targetId?: number;
  targetPath?: string;
  read: boolean;
  readAt?: string;
  createdAt: string;
}

export const notificationApi = {
  list: () => apiRequest<UserNotification[]>({ url: "/notifications" }),
  unreadCount: () => apiRequest<{ count: number }>({ url: "/notifications/unread-count" }),
  markRead: (id: number) => apiRequest<UserNotification>({ url: `/notifications/${id}/read`, method: "PUT" }),
  markAllRead: () => apiRequest<void>({ url: "/notifications/read-all", method: "PUT" }),
};
