export interface User { id: number; username: string; nickname: string; email?: string; avatar?: string }
export type ProjectStatus = "PLANNING" | "RUNNING" | "PAUSED" | "COMPLETED" | "CANCELLED";
export interface Project { id: number; name: string; description?: string; ownerId: number; status: ProjectStatus; progress: number; startDate?: string; endDate?: string; createdAt: string; updatedAt: string }
export type MemberRole = "OWNER" | "ADMIN" | "MEMBER";
export interface Member { id: number; projectId: number; userId: number; username: string; nickname: string; avatar?: string; role: MemberRole; joinedAt: string }
export type TaskStatus = "TODO" | "IN_PROGRESS" | "REVIEW" | "DONE" | "CANCELLED";
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export interface ResearchTask { id: number; projectId: number; title: string; description?: string; assigneeId?: number; creatorId: number; priority: TaskPriority; status: TaskStatus; progress: number; startDate?: string; dueDate?: string; completedAt?: string; createdAt: string; updatedAt: string }
export interface Dashboard { totalTasks: number; completedTasks: number; inProgressTasks: number; overdueTasks: number; progress: number }
export type DocumentStatus = "WAITING" | "PROCESSING" | "SUCCESS" | "FAILED";
export type DocumentFileType = "PDF" | "DOCX" | "TXT" | "MARKDOWN";
export interface ProjectDocument { id: number; projectId: number; uploaderId: number; uploaderName?: string; originalName: string; fileType: DocumentFileType; fileSize: number; parseStatus: DocumentStatus; vectorStatus: DocumentStatus; parseError?: string; vectorError?: string; parsedAt?: string; vectorizedAt?: string; createdAt: string; updatedAt: string }
export interface DocumentChunk { id: number; pageNumber?: number; chunkIndex: number; content: string; charCount: number }
export interface DocumentChunkPage { total: number; page: number; size: number; records: DocumentChunk[] }
export interface SemanticSearchResult { documentId: number; documentName: string; pageNumber?: number; chunkIndex: number; score: number; content: string }

export interface ProjectForm { name: string; description?: string; status?: ProjectStatus; startDate?: string; endDate?: string }
export interface TaskForm { title: string; description?: string; assigneeId?: number; priority: TaskPriority; startDate?: string; dueDate?: string }
