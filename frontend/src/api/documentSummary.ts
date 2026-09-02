import { apiRequest } from "./http";
import { downloadExport, type ExportFormat } from "@/utils/download";

export interface DocumentSummarySource {
  pageNumber?: number;
  chunkIndex: number;
  excerpt: string;
}

export interface DocumentSummary {
  id: number;
  documentId: number;
  projectId: number;
  creatorId: number;
  creatorName?: string;
  documentName: string;
  title: string;
  content: string;
  sources: DocumentSummarySource[];
  model?: string;
  createdAt: string;
  updatedAt: string;
}

export const documentSummaryApi = {
  generate: (documentId: number, title?: string) => apiRequest<DocumentSummary>({
    url: `/documents/${documentId}/ai/summary`, method: "POST", data: { title }, timeout: 300000,
  }),
  list: (documentId: number) =>
    apiRequest<DocumentSummary[]>({ url: `/documents/${documentId}/ai/summaries` }),
  detail: (summaryId: number) =>
    apiRequest<DocumentSummary>({ url: `/ai/document-summaries/${summaryId}` }),
  update: (summaryId: number, data: { title: string; content: string }) =>
    apiRequest<DocumentSummary>({ url: `/ai/document-summaries/${summaryId}`, method: "PUT", data }),
  remove: (summaryId: number) =>
    apiRequest<void>({ url: `/ai/document-summaries/${summaryId}`, method: "DELETE" }),
  export: (summaryId: number, title: string, format: ExportFormat) =>
    downloadExport(`/ai/document-summaries/${summaryId}/export`, title, format),
};
