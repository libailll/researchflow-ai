import { TOKEN_KEY, apiRequest } from "./http";
import type { ResearchTask, SemanticSearchResult } from "@/types/model";

export interface AiHistoryMessage { role: "user" | "assistant"; content: string }
export interface AiStreamEvent {
  type: "reasoning" | "content" | "sources" | "tool" | "action" | "done" | "error";
  content?: string; message?: string; model?: string; sources?: SemanticSearchResult[];
  name?: string; label?: string; status?: "running" | "success" | "error"; summary?: string;
  actionType?: "CREATE_TASK" | "UPDATE_TASK"; description?: string; payload?: Record<string, unknown>;
}
export interface AgentActionProposal { actionType: "CREATE_TASK" | "UPDATE_TASK"; label: string; description: string; payload: Record<string, unknown> }
export interface AgentActionResult { auditId: number; actionType: string; status: "SUCCESS"; task: ResearchTask }
export interface AiConversation { id: number; projectId: number; title: string; lastMessage?: string; createdAt: string; updatedAt: string }
export interface AiPersistedMessage { id: number; role: "USER" | "ASSISTANT"; content: string; reasoning?: string; sources: SemanticSearchResult[]; model?: string; createdAt: string }
export interface AiConversationDetail { conversation: AiConversation; messages: AiPersistedMessage[] }

export const aiConversationApi = {
  list: (projectId: number) => apiRequest<AiConversation[]>({ url: `/projects/${projectId}/ai/conversations` }),
  create: (projectId: number, title: string) => apiRequest<AiConversation>({ url: `/projects/${projectId}/ai/conversations`, method: "POST", data: { title } }),
  detail: (conversationId: number) => apiRequest<AiConversationDetail>({ url: `/ai/conversations/${conversationId}` }),
  rename: (conversationId: number, title: string) => apiRequest<AiConversation>({ url: `/ai/conversations/${conversationId}`, method: "PUT", data: { title } }),
  remove: (conversationId: number) => apiRequest<void>({ url: `/ai/conversations/${conversationId}`, method: "DELETE" }),
  clear: (conversationId: number) => apiRequest<void>({ url: `/ai/conversations/${conversationId}/messages`, method: "DELETE" }),
};

export const aiActionApi = {
  execute: (projectId: number, conversationId: number | undefined, action: AgentActionProposal) =>
    apiRequest<AgentActionResult>({
      url: `/projects/${projectId}/ai/actions/execute`, method: "POST",
      data: { actionType: action.actionType, conversationId, payload: action.payload },
    }),
};

export const searchProjectDocuments = (projectId: number, query: string, topK = 5) =>
  apiRequest<SemanticSearchResult[]>({
    url: `/projects/${projectId}/ai/search`, method: "POST", data: { query, topK }, timeout: 60000,
  });

export async function streamAiChat(
  projectId: number,
  conversationId: number | undefined,
  message: string,
  history: AiHistoryMessage[],
  signal: AbortSignal,
  onEvent: (event: AiStreamEvent) => void,
) {
  const baseUrl = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
  const response = await fetch(`${baseUrl}/projects/${projectId}/ai/chat/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY) || ""}`,
    },
    body: JSON.stringify({ conversationId, message, history }),
    signal,
  });
  const contentType = response.headers.get("content-type") || "";
  if (!response.ok || !response.body || !contentType.includes("text/event-stream")) {
    let errorMessage = "AI 服务暂时不可用，请稍后重试";
    try { const payload = await response.json(); errorMessage = payload.message || payload.detail || errorMessage; } catch { /* ignore non-JSON responses */ }
    throw new Error(errorMessage);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  const dispatch = (block: string) => {
    const data = block.split(/\r?\n/).find((line) => line.startsWith("data:"))?.slice(5).trim();
    if (!data) return;
    let event: AiStreamEvent;
    try { event = JSON.parse(data) as AiStreamEvent; } catch { return; }
    onEvent(event);
  };
  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value, { stream: !done });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() || "";
    for (const block of events) dispatch(block);
    if (done) { if (buffer.trim()) dispatch(buffer); break; }
  }
}
