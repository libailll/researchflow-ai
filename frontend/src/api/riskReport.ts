import { apiRequest } from "./http";
import type { SemanticSearchResult } from "@/types/model";
import { downloadExport, type ExportFormat } from "@/utils/download";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface RiskAnalysisSnapshot {
  generatedAt?: string;
  totalTasks?: number;
  activeTasks?: number;
  completedTasks?: number;
  overdueTasks?: number;
  dueSoonTasks?: number;
  urgentTasks?: number;
  unassignedTasks?: number;
  lowProgressNearDeadlineTasks?: number;
  overloadedMembers?: number;
  projectProgress?: number;
  daysRemaining?: number | null;
  scoreBreakdown?: Record<string, number>;
  workload?: Array<{ userId: number; name?: string; role: string; activeTasks: number }>;
}

export interface ProjectRiskReport {
  id: number;
  projectId: number;
  creatorId: number;
  creatorName?: string;
  title: string;
  riskLevel: RiskLevel;
  riskScore: number;
  content: string;
  analysisSnapshot: RiskAnalysisSnapshot;
  sources: SemanticSearchResult[];
  model?: string;
  createdAt: string;
  updatedAt: string;
}

export const riskReportApi = {
  generate: (projectId: number, data: { title?: string }) =>
    apiRequest<ProjectRiskReport>({
      url: `/projects/${projectId}/ai/risk-report`,
      method: "POST",
      data,
      timeout: 210000,
    }),
  list: (projectId: number) =>
    apiRequest<ProjectRiskReport[]>({ url: `/projects/${projectId}/ai/risk-reports` }),
  detail: (reportId: number) =>
    apiRequest<ProjectRiskReport>({ url: `/ai/risk-reports/${reportId}` }),
  update: (reportId: number, data: { title: string; content: string }) =>
    apiRequest<ProjectRiskReport>({ url: `/ai/risk-reports/${reportId}`, method: "PUT", data }),
  remove: (reportId: number) =>
    apiRequest<void>({ url: `/ai/risk-reports/${reportId}`, method: "DELETE" }),
  export: (reportId: number, title: string, format: ExportFormat) =>
    downloadExport(`/ai/risk-reports/${reportId}/export`, title, format),
};
