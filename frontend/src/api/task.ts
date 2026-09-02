import { apiRequest } from "./http";
import type { Dashboard, ResearchTask, TaskForm, TaskStatus } from "@/types/model";

export const taskApi = {
  list: (projectId: number) => apiRequest<ResearchTask[]>({ url: `/projects/${projectId}/tasks` }),
  create: (projectId: number, data: TaskForm) => apiRequest<ResearchTask>({ url: `/projects/${projectId}/tasks`, method: "POST", data }),
  updateStatus: (id: number, status: TaskStatus) => apiRequest<ResearchTask>({ url: `/tasks/${id}/status`, method: "PUT", data: { status } }),
  remove: (id: number) => apiRequest<void>({ url: `/tasks/${id}`, method: "DELETE" }),
  dashboard: (projectId: number) => apiRequest<Dashboard>({ url: `/projects/${projectId}/dashboard` }),
};
