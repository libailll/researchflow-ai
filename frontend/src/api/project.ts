import { apiRequest } from "./http";
import type { Member, MemberRole, Project, ProjectForm } from "@/types/model";

export const projectApi = {
  list: () => apiRequest<Project[]>({ url: "/projects" }),
  create: (data: ProjectForm) => apiRequest<Project>({ url: "/projects", method: "POST", data }),
  update: (id: number, data: ProjectForm) => apiRequest<Project>({ url: `/projects/${id}`, method: "PUT", data }),
  remove: (id: number) => apiRequest<void>({ url: `/projects/${id}`, method: "DELETE" }),
  members: (id: number) => apiRequest<Member[]>({ url: `/projects/${id}/members` }),
  addMember: (id: number, userId: number, role: MemberRole) => apiRequest<Member>({ url: `/projects/${id}/members`, method: "POST", data: { userId, role } }),
  removeMember: (id: number, userId: number) => apiRequest<void>({ url: `/projects/${id}/members/${userId}`, method: "DELETE" }),
};

