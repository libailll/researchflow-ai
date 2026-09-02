<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { ChatDotRound, Delete, EditPen, Plus, Promotion, RefreshRight, VideoPause } from "@element-plus/icons-vue";
import EmptyState from "@/components/common/EmptyState.vue";
import { aiActionApi, aiConversationApi, streamAiChat, type AgentActionProposal, type AiConversation, type AiHistoryMessage, type AiStreamEvent } from "@/api/ai";
import { useWorkspaceStore } from "@/stores/workspace";
import type { SemanticSearchResult } from "@/types/model";
import { formatDate } from "@/utils/format";

interface ToolTrace { name: string; label: string; status: "running" | "success" | "error"; summary?: string }
interface UiAction extends AgentActionProposal { status: "pending" | "executing" | "success" | "cancelled" | "error"; auditId?: number; error?: string }
interface UiMessage { id: number; role: "user" | "assistant"; content: string; reasoning?: string; sources?: SemanticSearchResult[]; tools?: ToolTrace[]; actions?: UiAction[]; error?: boolean }

const workspace = useWorkspaceStore();
const input = ref(""); const generating = ref(false); const messageId = ref(1); const chatBody = ref<HTMLElement>();
const messages = ref<UiMessage[]>([]); const conversations = ref<AiConversation[]>([]);
const activeConversationId = ref<number>(); const conversationsLoading = ref(false); const historyLoading = ref(false);
let controller: AbortController | undefined;
const suggestions = ["分析当前项目进度和风险", "列出项目中的延期任务", "总结当前项目文档的核心内容"];
const canSend = computed(() => !!workspace.activeProjectId && !!input.value.trim() && !generating.value);
const actionFieldLabels: Record<string, string> = { taskId: "任务ID", title: "标题", description: "说明", assigneeId: "负责人ID", priority: "优先级", progress: "进度", startDate: "开始日期", dueDate: "截止日期" };

function history(): AiHistoryMessage[] {
  return messages.value.filter((item) => item.content.trim() && !item.error).slice(-20).map(({ role, content }) => ({ role, content }));
}
function conversationTitle(question: string) { return question.replace(/\s+/g, " ").slice(0, 36) || "新对话"; }
async function scrollToBottom() { await nextTick(); if (chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight; }

async function loadConversations(selectFirst = true) {
  if (!workspace.activeProjectId) { conversations.value = []; return; }
  conversationsLoading.value = true;
  try {
    conversations.value = await aiConversationApi.list(workspace.activeProjectId);
    if (selectFirst && !activeConversationId.value && conversations.value.length) await selectConversation(conversations.value[0].id);
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { conversationsLoading.value = false; }
}

async function selectConversation(conversationId: number) {
  if (generating.value) stop();
  activeConversationId.value = conversationId; historyLoading.value = true;
  try {
    const detail = await aiConversationApi.detail(conversationId);
    messages.value = detail.messages.map((item) => ({
      id: messageId.value++, role: item.role === "USER" ? "user" : "assistant",
      content: item.content, reasoning: item.reasoning, sources: item.sources,
    }));
    await scrollToBottom();
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { historyLoading.value = false; }
}

function startNewConversation() {
  if (generating.value) stop();
  activeConversationId.value = undefined; messages.value = []; input.value = "";
}

async function ensureConversation(question: string) {
  if (activeConversationId.value) return activeConversationId.value;
  const conversation = await aiConversationApi.create(workspace.activeProjectId!, conversationTitle(question));
  conversations.value.unshift(conversation); activeConversationId.value = conversation.id;
  return conversation.id;
}

async function send(preset?: string) {
  const question = (preset ?? input.value).trim();
  if (!question || !workspace.activeProjectId || generating.value) return;
  const previousHistory = history(); input.value = "";
  let pendingAnswer: UiMessage | undefined;
  try {
    const conversationId = await ensureConversation(question);
    messages.value.push({ id: messageId.value++, role: "user", content: question });
    const answer: UiMessage = { id: messageId.value++, role: "assistant", content: "" };
    pendingAnswer = answer; messages.value.push(answer); generating.value = true; controller = new AbortController(); await scrollToBottom();
    await streamAiChat(workspace.activeProjectId, conversationId, question, previousHistory, controller.signal, (event: AiStreamEvent) => {
      if (event.type === "content") answer.content += event.content || "";
      if (event.type === "reasoning") answer.reasoning = (answer.reasoning || "") + (event.content || "");
      if (event.type === "sources") answer.sources = event.sources || [];
      if (event.type === "action" && event.actionType && event.payload) {
        answer.actions ||= [];
        answer.actions.push({
          actionType: event.actionType, label: event.label || "Agent 操作",
          description: event.description || "待确认操作", payload: event.payload, status: "pending",
        });
      }
      if (event.type === "tool" && event.name) {
        answer.tools ||= [];
        if (event.status === "running") {
          answer.tools.push({ name: event.name, label: event.label || event.name, status: "running" });
        } else {
          for (let index = answer.tools.length - 1; index >= 0; index -= 1) {
            const tool = answer.tools[index];
            if (tool.name === event.name && tool.status === "running") {
              tool.status = event.status || "success"; tool.summary = event.summary; break;
            }
          }
        }
      }
      if (event.type === "error") throw new Error(event.message || "AI 服务暂时不可用");
      scrollToBottom();
    });
    if (!answer.content.trim()) answer.content = "没有收到有效回答，请重试。";
  } catch (error) {
    if ((error as Error).name === "AbortError") { if (pendingAnswer) pendingAnswer.content ||= "已停止生成。"; }
    else if (pendingAnswer) { pendingAnswer.error = true; pendingAnswer.content = (error as Error).message; ElMessage.error(pendingAnswer.content); }
    else ElMessage.error((error as Error).message);
  } finally { generating.value = false; controller = undefined; await loadConversations(false); await scrollToBottom(); }
}

function actionFields(action: UiAction) {
  return Object.entries(action.payload)
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([key, value]) => ({ key, label: actionFieldLabels[key] || key, value: key === "progress" ? `${value}%` : String(value) }));
}

async function confirmAction(action: UiAction) {
  if (!workspace.activeProjectId || action.status !== "pending") return;
  try {
    await ElMessageBox.confirm(`确认${action.label}“${action.description}”？执行后将写入项目数据。`, "确认 Agent 操作", {
      type: "warning", confirmButtonText: "确认执行", cancelButtonText: "暂不执行",
    });
    action.status = "executing";
    const result = await aiActionApi.execute(workspace.activeProjectId, activeConversationId.value, action);
    action.status = "success"; action.auditId = result.auditId;
    await workspace.loadActiveProject();
    ElMessage.success(`${action.label}成功`);
  } catch (error) {
    if (error === "cancel" || error === "close") { action.status = "cancelled"; return; }
    action.status = "error"; action.error = (error as Error).message;
    ElMessage.error(action.error);
  }
}

function declineAction(action: UiAction) { if (action.status === "pending") action.status = "cancelled"; }

function stop() { controller?.abort(); }
async function clearMessages() {
  if (!activeConversationId.value) { messages.value = []; return; }
  try {
    await ElMessageBox.confirm("确定清空当前会话的全部消息？", "清空会话", { type: "warning", confirmButtonText: "清空", cancelButtonText: "取消" });
    if (generating.value) stop();
    await aiConversationApi.clear(activeConversationId.value); messages.value = []; await loadConversations(false);
    ElMessage.success("当前会话已清空");
  } catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); }
}
async function renameConversation(conversation: AiConversation) {
  try {
    const result = await ElMessageBox.prompt("输入新的会话名称", "重命名会话", { inputValue: conversation.title, inputPattern: /\S+/, inputErrorMessage: "名称不能为空", confirmButtonText: "保存", cancelButtonText: "取消" });
    const updated = await aiConversationApi.rename(conversation.id, result.value.trim());
    conversations.value = conversations.value.map((item) => item.id === updated.id ? updated : item);
  } catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); }
}
async function deleteConversation(conversation: AiConversation) {
  try {
    await ElMessageBox.confirm(`确定删除会话“${conversation.title}”？`, "删除会话", { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" });
    if (activeConversationId.value === conversation.id && generating.value) stop();
    await aiConversationApi.remove(conversation.id);
    conversations.value = conversations.value.filter((item) => item.id !== conversation.id);
    if (activeConversationId.value === conversation.id) {
      activeConversationId.value = undefined; messages.value = [];
      if (conversations.value.length) await selectConversation(conversations.value[0].id);
    }
    ElMessage.success("会话已删除");
  } catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); }
}
function retry() {
  const userIndex = [...messages.value].map((item) => item.role).lastIndexOf("user");
  if (userIndex < 0) return;
  const question = messages.value[userIndex].content; messages.value.splice(userIndex); send(question);
}
function handleKeydown(event: KeyboardEvent) { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); send(); } }

watch(() => workspace.activeProjectId, async () => {
  if (generating.value) stop(); activeConversationId.value = undefined; messages.value = []; await loadConversations();
}, { immediate: true });
onUnmounted(stop);
</script>

<template>
  <EmptyState v-if="!workspace.activeProject" title="先创建或选择一个项目" description="AI 助手会在当前项目上下文中与你协作。" />
  <section v-else class="assistant-page">
    <header class="assistant-heading"><div><p>RESEARCH COPILOT</p><h2>{{ workspace.activeProject.name }} · AI 助手</h2><span>由 DeepSeek 与项目知识库共同驱动，对话和引用会自动保存。</span></div><el-button text :icon="Delete" :disabled="!activeConversationId || !messages.length" @click="clearMessages">清空当前会话</el-button></header>
    <section class="assistant-shell">
      <aside class="assistant-context">
        <div class="history-brand"><span class="ai-orb"><el-icon><ChatDotRound /></el-icon></span><div><p>RESEARCH MEMORY</p><strong>研究会话</strong></div></div>
        <el-button class="new-chat" :icon="Plus" @click="startNewConversation">新建对话</el-button>
        <div class="history-label"><span>最近会话</span><small>{{ conversations.length }}</small></div>
        <div v-loading="conversationsLoading" class="history-list">
          <article v-for="conversation in conversations" :key="conversation.id" :class="{ active: activeConversationId === conversation.id }" @click="selectConversation(conversation.id)">
            <div><strong>{{ conversation.title }}</strong><span>{{ conversation.lastMessage || "尚未开始提问" }}</span><small>{{ formatDate(conversation.updatedAt) }}</small></div>
            <span class="history-actions"><el-button text :icon="EditPen" title="重命名" @click.stop="renameConversation(conversation)" /><el-button text type="danger" :icon="Delete" title="删除" @click.stop="deleteConversation(conversation)" /></span>
          </article>
          <div v-if="!conversations.length && !conversationsLoading" class="history-empty">还没有历史会话</div>
        </div>
        <div class="history-status"><span class="online"><b /> RAG 知识检索已启用</span><small>会话仅对你本人可见</small></div>
      </aside>
      <main class="conversation-panel">
        <div ref="chatBody" v-loading="historyLoading" class="chat-body">
          <section v-if="!messages.length" class="welcome-state"><span class="welcome-icon"><el-icon><ChatDotRound /></el-icon></span><p>RESEARCHFLOW AI</p><h3>今天想一起研究什么？</h3><span>我可以协助解释概念、拆解研究问题、整理计划和改进表达。</span><div class="suggestions"><button v-for="suggestion in suggestions" :key="suggestion" @click="send(suggestion)">{{ suggestion }}<el-icon><Promotion /></el-icon></button></div></section>
          <div v-for="message in messages" :key="message.id" class="message" :class="[message.role, { error: message.error }]">
            <span class="message-avatar">{{ message.role === "user" ? "你" : "AI" }}</span><div class="message-content"><small>{{ message.role === "user" ? "你" : "ResearchFlow AI" }}</small><div v-if="message.tools?.length" class="message-tools"><strong>Agent 工具调用</strong><article v-for="(tool,index) in message.tools" :key="`${tool.name}-${index}`" :class="tool.status"><i /><span><b>{{ tool.label }}</b><small>{{ tool.status === "running" ? "正在获取真实项目数据…" : tool.summary }}</small></span></article></div><section v-for="(action,index) in message.actions" :key="`${action.actionType}-${index}`" class="agent-action" :class="action.status"><header><div><span>需要你的确认</span><strong>{{ action.label }}</strong><small>{{ action.description }}</small></div><b>{{ action.status === "success" ? "已执行" : action.status === "cancelled" ? "已取消" : action.status === "error" ? "执行失败" : "待确认" }}</b></header><dl><div v-for="field in actionFields(action)" :key="field.key"><dt>{{ field.label }}</dt><dd>{{ field.value }}</dd></div></dl><footer v-if="action.status === 'pending'"><el-button @click="declineAction(action)">暂不执行</el-button><el-button type="primary" @click="confirmAction(action)">确认执行</el-button></footer><footer v-else-if="action.status === 'executing'"><span>正在重新校验权限并执行…</span></footer><footer v-else-if="action.status === 'success'"><span>操作已写入，审计编号 #{{ action.auditId }}</span></footer><footer v-else-if="action.status === 'error'"><span>{{ action.error }}</span></footer></section><details v-if="message.reasoning"><summary>查看思考过程</summary><p>{{ message.reasoning }}</p></details><p>{{ message.content }}<i v-if="generating && message === messages[messages.length - 1]" class="cursor" /></p><div v-if="message.sources?.length" class="message-sources"><strong>参考文档</strong><article v-for="(source,index) in message.sources" :key="`${source.documentId}-${source.chunkIndex}`"><b>[{{ index + 1 }}]</b><span><em>{{ source.documentName }}</em><small>{{ source.pageNumber ? `第 ${source.pageNumber} 页` : `片段 ${source.chunkIndex + 1}` }} · 相关度 {{ Math.round(source.score * 100) }}%</small></span></article></div><el-button v-if="message.error" text :icon="RefreshRight" @click="retry">重新生成</el-button></div>
          </div>
        </div>
        <footer class="composer"><div class="composer-box"><el-input v-model="input" type="textarea" resize="none" :autosize="{ minRows: 2, maxRows: 5 }" maxlength="4000" placeholder="输入你的研究问题，Enter 发送，Shift + Enter 换行" @keydown="handleKeydown" /><el-button v-if="generating" circle class="stop-button" :icon="VideoPause" title="停止生成" @click="stop" /><el-button v-else circle type="primary" :icon="Promotion" :disabled="!canSend" title="发送" @click="send()" /></div><span>AI 可能会出错，请核对重要研究结论与引用。</span></footer>
      </main>
    </section>
  </section>
</template>

<style scoped>
.assistant-page{height:calc(100vh - 182px);min-height:620px;display:flex;flex-direction:column}.assistant-heading{margin-bottom:21px;display:flex;align-items:center;justify-content:space-between}.assistant-heading p{margin:0 0 5px;color:#8996a9;font-size:8px;font-weight:800;letter-spacing:.18em}.assistant-heading h2{margin:0;font-size:27px;letter-spacing:-.04em}.assistant-heading span{display:block;margin-top:8px;color:#7b8799;font-size:10px}.assistant-shell{min-height:0;flex:1;display:grid;grid-template-columns:255px minmax(0,1fr);overflow:hidden;border:1px solid #dfe5ed;border-radius:18px;background:white;box-shadow:0 18px 48px rgba(18,41,77,.08)}.assistant-context{padding:31px 25px;color:white;background:radial-gradient(circle at 15% 5%,rgba(101,212,194,.16),transparent 30%),linear-gradient(180deg,#102a55,#0c1e3e)}.ai-orb{width:52px;height:52px;display:grid;place-items:center;border:1px solid rgba(105,216,199,.3);border-radius:16px;color:#6bd8c7;background:rgba(101,212,194,.09);font-size:24px}.assistant-context>p{margin:23px 0 6px;color:#65d4c2;font-size:8px;font-weight:900;letter-spacing:.17em}.assistant-context h3{margin:0;font:600 17px Georgia,serif}.online{margin-top:12px;display:flex;align-items:center;gap:6px;color:#91a7c7;font-size:8px}.online b{width:6px;height:6px;border-radius:50%;background:#61d2bd;box-shadow:0 0 10px #61d2bd}.context-card{margin-top:35px;padding:17px;border:1px solid rgba(255,255,255,.1);border-radius:12px;background:rgba(255,255,255,.045)}.context-card small,.context-card strong,.context-card span{display:block}.context-card small{color:#7189ad;font-size:7px}.context-card strong{margin:7px 0;color:#e7eef9;font-size:10px}.context-card span{color:#7790b4;font-size:8px;line-height:1.6}.privacy-note{margin-top:auto;padding-top:25px;display:flex;align-items:flex-start;gap:8px;color:#6f86a9;font-size:8px;line-height:1.6}.assistant-context{display:flex;flex-direction:column}.conversation-panel{min-width:0;display:flex;flex-direction:column}.chat-body{min-height:0;flex:1;padding:30px 36px;overflow-y:auto;background:linear-gradient(180deg,#fff,#fbfcfe)}.welcome-state{height:100%;display:grid;place-content:center;justify-items:center;text-align:center}.welcome-icon{width:62px;height:62px;display:grid;place-items:center;border-radius:20px;color:#285bd4;background:#e8efff;font-size:27px}.welcome-state>p{margin:19px 0 5px;color:#6aa7a0;font-size:8px;font-weight:900;letter-spacing:.18em}.welcome-state h3{margin:0;font:600 25px Georgia,"Noto Serif SC",serif}.welcome-state>span{margin-top:10px;color:#8a96a7;font-size:9px}.suggestions{width:min(580px,100%);margin-top:28px;display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.suggestions button{min-height:72px;padding:13px;display:flex;align-items:center;justify-content:space-between;gap:9px;border:1px solid #e1e6ee;border-radius:12px;color:#526078;background:white;font-size:9px;text-align:left;transition:.18s}.suggestions button:hover{border-color:#92acdf;color:#285bd4;transform:translateY(-2px);box-shadow:0 9px 24px rgba(37,81,153,.08)}.message{max-width:820px;margin:0 auto 24px;display:flex;align-items:flex-start;gap:12px}.message.user{flex-direction:row-reverse}.message-avatar{width:32px;height:32px;flex:0 0 auto;display:grid;place-items:center;border-radius:10px;color:#1c4167;background:#6bd3c2;font-size:8px;font-weight:900}.message.assistant .message-avatar{color:white;background:#234f94}.message-content{max-width:78%}.message-content>small{display:block;margin:0 0 6px;color:#98a2b1;font-size:7px}.message.user .message-content>small{text-align:right}.message-content>p{margin:0;padding:13px 16px;border:1px solid #e3e8ef;border-radius:4px 14px 14px;color:#35445c;background:white;font-size:11px;line-height:1.85;white-space:pre-wrap;box-shadow:0 5px 18px rgba(18,45,83,.04)}.message.user .message-content>p{border:0;border-radius:14px 4px 14px 14px;color:white;background:#285bd4}.message.error .message-content>p{border-color:#f4c5c9;color:#a84e57;background:#fff5f6}.message-content details{margin-bottom:7px;padding:9px 12px;border-radius:9px;color:#7e899a;background:#f2f4f7;font-size:8px}.message-content details p{white-space:pre-wrap}.cursor{display:inline-block;width:2px;height:13px;margin-left:3px;vertical-align:-2px;background:#3b69c8;animation:blink .8s infinite}.composer{padding:17px 25px 19px;border-top:1px solid #e8ebf0;background:white}.composer-box{display:flex;align-items:flex-end;gap:10px;padding:9px 10px 9px 15px;border:1px solid #dbe1ea;border-radius:14px;box-shadow:0 8px 24px rgba(17,45,84,.05)}.composer-box:focus-within{border-color:#8ba8e1}.composer-box :deep(.el-textarea__inner){padding:4px 0;border:0;box-shadow:none;background:transparent;line-height:1.6}.composer>span{display:block;margin-top:8px;color:#a1a9b5;font-size:7px;text-align:center}.stop-button{color:#d56161}.composer .el-button{flex:0 0 auto}@keyframes blink{50%{opacity:0}}@media(max-width:900px){.assistant-page{height:auto;min-height:700px}.assistant-shell{grid-template-columns:1fr}.assistant-context{display:none}.conversation-panel{min-height:650px}.chat-body{padding:24px 18px}.suggestions{grid-template-columns:1fr}.message-content{max-width:85%}}@media(max-width:600px){.assistant-heading span{max-width:270px}.assistant-heading .el-button{display:none}.welcome-state h3{font-size:21px}.composer{padding:12px}.message-content{max-width:88%}}
.assistant-page{overflow:hidden}.assistant-shell{height:0}.conversation-panel{height:100%;min-height:0;overflow:hidden}.chat-body{height:0;overscroll-behavior:contain}.composer{position:relative;z-index:2;flex:0 0 auto}
.message-sources{margin-top:10px;padding:12px;border:1px solid #dfe7f2;border-radius:11px;background:#f7faff}.message-sources>strong{display:block;margin-bottom:8px;color:#426a9e;font-size:8px;letter-spacing:.08em}.message-sources article{padding:7px 0;display:flex;align-items:flex-start;gap:8px;border-top:1px solid #e8edf4}.message-sources article:first-of-type{border-top:0}.message-sources article>b{color:#285bd4;font-size:8px}.message-sources article>span{min-width:0}.message-sources em,.message-sources small{display:block}.message-sources em{overflow:hidden;color:#465873;font-size:8px;font-style:normal;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.message-sources small{margin-top:3px;color:#8e9aac;font-size:7px}
.message-tools{margin-bottom:10px;padding:11px 13px;border:1px solid #dce8e6;border-radius:11px;background:#f4fbf9}.message-tools>strong{display:block;margin-bottom:7px;color:#39786f;font-size:8px;letter-spacing:.07em}.message-tools article{padding:6px 0;display:flex;align-items:flex-start;gap:9px;border-top:1px solid #e2efec}.message-tools article:first-of-type{border-top:0}.message-tools article>i{width:7px;height:7px;margin-top:4px;flex:0 0 auto;border-radius:50%;background:#55b9a8}.message-tools article.running>i{animation:toolPulse 1s infinite;box-shadow:0 0 0 4px rgba(85,185,168,.12)}.message-tools article.error>i{background:#dc6868}.message-tools article span,.message-tools article b,.message-tools article small{display:block}.message-tools article b{color:#355c58;font-size:8px}.message-tools article small{margin-top:3px;color:#7a9792;font-size:7px}@keyframes toolPulse{50%{opacity:.35;transform:scale(.8)}}
.assistant-shell{grid-template-columns:290px minmax(0,1fr)}.assistant-context{padding:22px 17px 17px;min-height:0}.history-brand{display:flex;align-items:center;gap:12px}.history-brand .ai-orb{width:42px;height:42px;border-radius:13px;font-size:20px}.history-brand p{margin:0 0 4px;color:#65d4c2;font-size:7px;font-weight:900;letter-spacing:.15em}.history-brand strong{font-size:13px}.new-chat{width:100%;height:39px;margin-top:20px!important;border-color:rgba(107,216,199,.28)!important;color:#dff9f5!important;background:rgba(101,212,194,.1)!important}.new-chat:hover{border-color:#6bd8c7!important;background:rgba(101,212,194,.16)!important}.history-label{margin:22px 5px 9px;display:flex;align-items:center;justify-content:space-between;color:#7189ad;font-size:7px;font-weight:800;letter-spacing:.12em}.history-label small{letter-spacing:0}.history-list{min-height:100px;flex:1;overflow-y:auto}.history-list article{margin-bottom:7px;padding:11px 9px 11px 12px;display:flex;align-items:center;justify-content:space-between;gap:7px;border:1px solid transparent;border-radius:11px;cursor:pointer;transition:.16s}.history-list article:hover{background:rgba(255,255,255,.055)}.history-list article.active{border-color:rgba(103,213,195,.18);background:rgba(81,133,196,.2)}.history-list article>div{min-width:0;flex:1}.history-list strong,.history-list article>div>span,.history-list article>div>small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.history-list strong{color:#dce8f8;font-size:9px}.history-list article>div>span{margin-top:5px;color:#7187a9;font-size:7px}.history-list article>div>small{margin-top:5px;color:#526b91;font-size:6px}.history-actions{display:none;flex:0 0 auto}.history-list article:hover .history-actions,.history-list article.active .history-actions{display:flex}.history-actions .el-button{width:23px;height:23px;margin:0;padding:0;color:#8aa0bf}.history-actions .el-button:hover{color:#6bd8c7;background:rgba(255,255,255,.08)}.history-empty{padding:45px 0;color:#60789d;text-align:center;font-size:8px}.history-status{padding:14px 5px 0;border-top:1px solid rgba(255,255,255,.08)}.history-status .online{margin:0}.history-status>small{display:block;margin-top:7px;color:#536b90;font-size:7px}
@media(max-width:900px){.assistant-page{overflow:visible}.assistant-shell{height:auto}.assistant-context{display:flex;max-height:285px}.history-list{max-height:150px}.conversation-panel{height:auto;overflow:visible}.chat-body{height:auto}}
</style>

<style scoped>
.agent-action{margin:0 0 12px;padding:17px;border:1px solid #cfe0f5;border-radius:16px;background:#f6faff;box-shadow:0 8px 24px rgba(0,113,227,.05)}
.agent-action>header{display:flex;align-items:flex-start;justify-content:space-between;gap:18px}.agent-action>header div span,.agent-action>header div strong,.agent-action>header div small{display:block}.agent-action>header div span{margin-bottom:6px;color:#0071e3;font-size:10px;font-weight:700}.agent-action>header div strong{color:#1d1d1f;font-size:14px}.agent-action>header div small{margin-top:4px;color:#6e6e73;font-size:11px}.agent-action>header>b{padding:5px 8px;border-radius:999px;color:#805b0b;background:#fff0c7;font-size:9px;white-space:nowrap}
.agent-action dl{margin:15px 0;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.agent-action dl div{padding:9px 11px;border-radius:10px;background:rgba(255,255,255,.8)}.agent-action dt{color:#86868b;font-size:9px}.agent-action dd{margin:3px 0 0;overflow:hidden;color:#303033;font-size:11px;text-overflow:ellipsis;white-space:nowrap}
.agent-action>footer{display:flex;align-items:center;justify-content:flex-end;gap:8px}.agent-action>footer .el-button{min-height:34px;padding:7px 14px;font-size:11px}.agent-action>footer>span{color:#6e6e73;font-size:10px}
.agent-action.success{border-color:#bfe4da;background:#f2fbf8}.agent-action.success>header>b{color:#087966;background:#dff5ef}.agent-action.cancelled{opacity:.62}.agent-action.cancelled>header>b{color:#6e6e73;background:#e8e8ed}.agent-action.error{border-color:#f1c7c7;background:#fff7f7}.agent-action.error>header>b{color:#ad3f3f;background:#fde5e5}
@media(max-width:600px){.agent-action dl{grid-template-columns:1fr}.agent-action>header{gap:10px}}
</style>

<style scoped>
.assistant-page { height: calc(100vh - 176px); min-height: 650px; }
.assistant-heading { margin-bottom: 28px; }
.assistant-heading p { margin-bottom: 8px; color:var(--rf-blue); font-size:11px; font-weight:700; letter-spacing:.12em; }
.assistant-heading h2 { font-size:clamp(30px,3vw,44px); font-weight:700; letter-spacing:-.045em; }
.assistant-heading span { margin-top:10px; color:var(--rf-muted); font-size:14px; }
.assistant-shell { grid-template-columns: 300px minmax(0,1fr); border-color:rgba(29,29,31,.08); border-radius:28px; background:rgba(255,255,255,.88); box-shadow:var(--rf-shadow-md); }
.assistant-context { padding:24px 18px 18px; color:var(--rf-ink); background:linear-gradient(180deg,rgba(245,245,247,.96),rgba(232,239,248,.96)); border-right:1px solid rgba(29,29,31,.07); }
.history-brand .ai-orb { color:white; background:linear-gradient(145deg,#2997ff,#0071e3); border:0; box-shadow:0 10px 24px rgba(0,113,227,.2); }
.history-brand p { color:var(--rf-blue); font-size:9px; }.history-brand strong { color:var(--rf-ink); font-size:15px; }
.new-chat { height:42px; border-color:rgba(0,113,227,.18)!important; color:#0066cc!important; background:rgba(0,113,227,.07)!important; }
.new-chat:hover { border-color:rgba(0,113,227,.35)!important; background:rgba(0,113,227,.11)!important; }
.history-label { color:#86868b; font-size:10px; }
.history-list article { padding:12px; border-radius:14px; }
.history-list article:hover { background:rgba(0,0,0,.035); }
.history-list article.active { border-color:rgba(0,113,227,.12); background:rgba(0,113,227,.09); }
.history-list strong { color:var(--rf-ink); font-size:12px; }.history-list article>div>span { color:#6e6e73; font-size:10px; }.history-list article>div>small { color:#86868b; font-size:9px; }
.history-empty { color:#86868b; font-size:11px; }.history-status { border-top-color:rgba(29,29,31,.08); }.history-status>small,.online { color:#6e6e73; font-size:10px; }
.chat-body { padding:38px clamp(24px,4vw,54px); background:linear-gradient(180deg,#fff,#fbfbfd); }
.welcome-icon { width:70px; height:70px; border-radius:22px; color:white; background:linear-gradient(145deg,#2997ff,#0071e3); box-shadow:0 15px 35px rgba(0,113,227,.18); }
.welcome-state>p { margin-top:22px; color:var(--rf-blue); font-size:10px; }.welcome-state h3 { font:700 32px/1.2 -apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif; letter-spacing:-.04em; }.welcome-state>span { margin-top:12px; color:#6e6e73; font-size:13px; }
.suggestions { width:min(680px,100%); gap:12px; }.suggestions button { min-height:86px; padding:17px; border-color:rgba(29,29,31,.09); border-radius:18px; color:#3d3d42; font-size:12px; box-shadow:var(--rf-shadow-sm); }.suggestions button:hover { border-color:rgba(0,113,227,.3); color:#0066cc; box-shadow:0 12px 28px rgba(0,113,227,.09); }
.message { max-width:900px; margin-bottom:28px; gap:14px; }.message-avatar { width:38px; height:38px; border-radius:12px; color:white; background:#0071e3; font-size:10px; }.message.assistant .message-avatar { background:#1d1d1f; }
.message-content>small { font-size:10px; }.message-content>p { padding:16px 19px; border-color:rgba(29,29,31,.08); border-radius:6px 18px 18px; color:#2f3033; font-size:14px; line-height:1.8; box-shadow:var(--rf-shadow-sm); }.message.user .message-content>p { border-radius:18px 6px 18px 18px; background:#0071e3; }
.message-content details { padding:11px 14px; border-radius:12px; font-size:11px; }
.message-sources,.message-tools { border-radius:14px; }.message-sources>strong,.message-tools>strong,.message-sources article>b,.message-tools article b { font-size:11px; }.message-sources em,.message-sources small,.message-tools article small { font-size:10px; }
.composer { padding:18px 26px 21px; border-top-color:rgba(29,29,31,.07); background:rgba(255,255,255,.9); backdrop-filter:blur(18px); }
.composer-box { padding:10px 10px 10px 16px; border-color:rgba(29,29,31,.12); border-radius:19px; box-shadow:0 8px 25px rgba(0,0,0,.045); }.composer-box:focus-within { border-color:rgba(0,113,227,.5); box-shadow:0 0 0 4px rgba(0,113,227,.08); }.composer>span { font-size:10px; }
@media(max-width:900px){.assistant-page{height:auto}.assistant-shell{grid-template-columns:1fr}.assistant-context{background:#f5f5f7}.chat-body{padding:28px 18px}}
</style>
