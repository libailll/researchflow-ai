import type { AxiosResponse } from "axios";
import { http } from "@/api/http";
import type { ApiResult } from "@/types/api";

export type ExportFormat = "pdf" | "docx";

export async function downloadExport(
  url: string,
  fallbackName: string,
  format: ExportFormat,
): Promise<void> {
  const response = await http.get<Blob>(url, {
    params: { format },
    responseType: "blob",
    timeout: 60000,
  });
  await ensureFileResponse(response);

  const blob = response.data;
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = responseFileName(response) || `${safeFileName(fallbackName)}.${format}`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(objectUrl);
}

async function ensureFileResponse(response: AxiosResponse<Blob>): Promise<void> {
  const header = response.headers["content-type"];
  const contentType = typeof header === "string" ? header : response.data.type;
  if (!contentType?.includes("application/json")) return;
  const result = JSON.parse(await response.data.text()) as ApiResult<unknown>;
  throw new Error(result.message || "报告导出失败");
}

function responseFileName(response: AxiosResponse<Blob>): string | undefined {
  const disposition = response.headers["content-disposition"] as string | undefined;
  if (!disposition) return undefined;
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) return decodeURIComponent(encoded.replace(/^"|"$/g, ""));
  return disposition.match(/filename="?([^";]+)"?/i)?.[1];
}

function safeFileName(value: string): string {
  return value.replace(/[\\/:*?"<>|\r\n]+/g, "_").trim() || "ResearchFlow 报告";
}
