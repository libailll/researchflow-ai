import { apiRequest } from "./http";
import type { User } from "@/types/model";

export const userApi = {
  me: () => apiRequest<User>({ url: "/users/me" }),
  updateMe: (data: Pick<User, "nickname" | "email" | "avatar">) => apiRequest<User>({ url: "/users/me", method: "PUT", data }),
};

