import axios, { AxiosError } from "axios";
import { ElMessage } from "element-plus";
import type { ApiResult } from "@/types/api";

export const TOKEN_KEY = "researchflow_token";

export const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || "/api", timeout: 12000 });

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (response) => {
    if (response.config.responseType === "blob") return response;
    const result = response.data as ApiResult<unknown>;
    if (result.code !== 200) {
      if ([401, 1001].includes(result.code)) {
        localStorage.removeItem(TOKEN_KEY);
        if (location.pathname !== "/login") location.assign("/login");
      }
      return Promise.reject(new Error(result.message || "请求失败"));
    }
    return response;
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const message = error.response?.data?.message || (error.code === "ECONNABORTED" ? "请求超时，请稍后重试" : "无法连接服务，请检查后端是否启动");
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      if (location.pathname !== "/login") location.assign("/login");
    } else ElMessage.error(message);
    return Promise.reject(new Error(message));
  },
);

export async function apiRequest<T>(config: Parameters<typeof http.request>[0]): Promise<T> {
  const response = await http.request<ApiResult<T>>(config);
  return response.data.data;
}
