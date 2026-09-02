import { apiRequest, http } from "./http";
import type { DocumentChunkPage, ProjectDocument } from "@/types/model";

export const documentApi = {
  list: (projectId: number) => apiRequest<ProjectDocument[]>({ url: `/projects/${projectId}/documents` }),
  upload: (projectId: number, file: File, onProgress?: (percentage: number) => void) => {
    const data = new FormData();
    data.append("file", file);
    return apiRequest<ProjectDocument>({
      url: `/projects/${projectId}/documents`, method: "POST", data,
      timeout: 60000,
      onUploadProgress: (event) => {
        if (event.total && onProgress) onProgress(Math.round((event.loaded * 100) / event.total));
      },
    });
  },
  remove: (documentId: number) => apiRequest<void>({ url: `/documents/${documentId}`, method: "DELETE" }),
  chunks: (documentId: number, page = 1, size = 20) => apiRequest<DocumentChunkPage>({ url: `/documents/${documentId}/chunks`, params: { page, size } }),
  vectorize: (documentId: number) => apiRequest<void>({ url: `/documents/${documentId}/vectorize`, method: "POST" }),
  download: (documentId: number) => http.get<Blob>(`/documents/${documentId}/download`, { responseType: "blob", timeout: 60000 }),
};
