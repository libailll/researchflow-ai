import { apiRequest } from "./http";
import type { User } from "@/types/model";

export const authApi = {
  login: (username: string, password: string) => apiRequest<{ token: string; user: User }>({ url: "/auth/login", method: "POST", data: { username, password } }),
  register: (username: string, password: string, nickname: string) => apiRequest<void>({ url: "/auth/register", method: "POST", data: { username, password, nickname } }),
  logout: () => apiRequest<void>({ url: "/auth/logout", method: "POST" }),
};

