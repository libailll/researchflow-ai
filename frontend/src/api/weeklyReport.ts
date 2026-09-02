import { apiRequest } from "./http";
import type { SemanticSearchResult } from "@/types/model";
import { downloadExport, type ExportFormat } from "@/utils/download";

export interface WeeklyReport {
  id: number;
  projectId: number;
  creatorId: number;
  creatorName?: string;
  title: string;
  periodStart: string;
  periodEnd: string;
  content: string;
  sources: SemanticSearchResult[];
  model?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WeeklyReportGenerateForm {
  periodStart: string;
  periodEnd: string;
  title?: string;
}

export const weeklyReportApi = {
  generate: (projectId: number, data: WeeklyReportGenerateForm) =>
    apiRequest<WeeklyReport>({
      url: `/projects/${projectId}/ai/weekly-report`,
      method: "POST",
      data,
      timeout: 180000,
    }),
  list: (projectId: number) =>
    apiRequest<WeeklyReport[]>({ url: `/projects/${projectId}/ai/weekly-reports` }),
  detail: (reportId: number) =>
    apiRequest<WeeklyReport>({ url: `/ai/weekly-reports/${reportId}` }),
  update: (reportId: number, data: { title: string; content: string }) =>
    apiRequest<WeeklyReport>({ url: `/ai/weekly-reports/${reportId}`, method: "PUT", data }),
  remove: (reportId: number) =>
    apiRequest<void>({ url: `/ai/weekly-reports/${reportId}`, method: "DELETE" }),
  export: (reportId: number, title: string, format: ExportFormat) =>
    downloadExport(`/ai/weekly-reports/${reportId}/export`, title, format),
};
