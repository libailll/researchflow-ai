<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox, type UploadRequestOptions, type UploadRawFile } from "element-plus";
import { Delete, Download, EditPen, Files, MagicStick, RefreshRight, Search, UploadFilled, View } from "@element-plus/icons-vue";
import EmptyState from "@/components/common/EmptyState.vue";
import { documentApi } from "@/api/document";
import { documentSummaryApi, type DocumentSummary } from "@/api/documentSummary";
import { searchProjectDocuments } from "@/api/ai";
import { useWorkspaceStore } from "@/stores/workspace";
import type { DocumentChunk, DocumentFileType, DocumentStatus, ProjectDocument, SemanticSearchResult } from "@/types/model";
import { formatDate } from "@/utils/format";

const workspace = useWorkspaceStore();
const documents = ref<ProjectDocument[]>([]);
const loading = ref(false); const uploading = ref(false); const uploadProgress = ref(0);
const keyword = ref(""); const typeFilter = ref<DocumentFileType | "">(""); const statusFilter = ref<DocumentStatus | "">("");
const chunkDrawer = ref(false); const chunkLoading = ref(false); const selectedDocument = ref<ProjectDocument>();
const chunks = ref<DocumentChunk[]>([]); const chunkTotal = ref(0); const chunkPage = ref(1); const chunkSize = 20;
const semanticQuery = ref(""); const semanticLoading = ref(false); const semanticSearched = ref(false);
const semanticResults = ref<SemanticSearchResult[]>([]);
const summaryDrawer = ref(false); const summaryLoading = ref(false); const summaryGenerating = ref(false);
const summarySaving = ref(false); const summaryEditing = ref(false); const summaryExporting = ref(false); const summaries = ref<DocumentSummary[]>([]);
const activeSummary = ref<DocumentSummary>(); const summaryEdit = ref({ title: "", content: "" });
let refreshTimer: number | undefined;

const statusText: Record<DocumentStatus, string> = { WAITING: "等待解析", PROCESSING: "解析中", SUCCESS: "已完成", FAILED: "解析失败" };
const typeText: Record<DocumentFileType, string> = { PDF: "PDF", DOCX: "Word", TXT: "文本", MARKDOWN: "Markdown" };
const filteredDocuments = computed(() => documents.value.filter((item) => {
  const matchKeyword = !keyword.value || item.originalName.toLowerCase().includes(keyword.value.toLowerCase());
  return matchKeyword && (!typeFilter.value || item.fileType === typeFilter.value) && (!statusFilter.value || item.parseStatus === statusFilter.value);
}));
const totalSize = computed(() => documents.value.reduce((sum, item) => sum + item.fileSize, 0));
const successCount = computed(() => documents.value.filter((item) => item.parseStatus === "SUCCESS").length);
const processingCount = computed(() => documents.value.filter((item) =>
  ["WAITING", "PROCESSING"].includes(item.parseStatus) || ["WAITING", "PROCESSING"].includes(item.vectorStatus)
).length);
const summarySections = computed(() => {
  const content = activeSummary.value?.content || ""; const result: { title: string; content: string }[] = [];
  let current: { title: string; content: string } | undefined;
  for (const line of content.split(/\r?\n/)) {
    if (/^##\s+/.test(line)) { current = { title: line.replace(/^##\s+/, "").trim(), content: "" }; result.push(current); }
    else if (current) current.content += `${current.content ? "\n" : ""}${line}`;
  }
  return result.length ? result : [{ title: "文档总结", content }];
});

async function loadDocuments() {
  if (!workspace.activeProjectId) { documents.value = []; return; }
  loading.value = true;
  try { documents.value = await documentApi.list(workspace.activeProjectId); }
  catch (error) { ElMessage.error((error as Error).message); }
  finally { loading.value = false; }
}

function validateFile(file: UploadRawFile) {
  const extension = file.name.split(".").pop()?.toLowerCase();
  if (!extension || !["pdf", "docx", "txt", "md", "markdown"].includes(extension)) { ElMessage.error("仅支持 PDF、DOCX、TXT 和 Markdown 文件"); return false; }
  if (file.size > 50 * 1024 * 1024) { ElMessage.error("文件大小不能超过 50MB"); return false; }
  return true;
}

async function uploadFile(options: UploadRequestOptions) {
  if (!workspace.activeProjectId) return;
  uploading.value = true; uploadProgress.value = 0;
  try {
    await documentApi.upload(workspace.activeProjectId, options.file, (percentage) => { uploadProgress.value = percentage; });
    options.onSuccess({}); ElMessage.success("文档上传成功，已进入解析队列"); await loadDocuments();
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { uploading.value = false; window.setTimeout(() => { uploadProgress.value = 0; }, 500); }
}

async function download(document: ProjectDocument) {
  try { const response = await documentApi.download(document.id); const url = URL.createObjectURL(response.data); const link = window.document.createElement("a"); link.href = url; link.download = document.originalName; link.click(); URL.revokeObjectURL(url); }
  catch (error) { ElMessage.error((error as Error).message); }
}

async function remove(document: ProjectDocument) {
  try { await ElMessageBox.confirm(`确定删除文档“${document.originalName}”？`, "删除文档", { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" }); await documentApi.remove(document.id); ElMessage.success("文档已删除"); await loadDocuments(); }
  catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); }
}

async function openChunks(document: ProjectDocument) {
  selectedDocument.value = document; chunkPage.value = 1; chunkDrawer.value = true; await loadChunks();
}

async function loadChunks() {
  if (!selectedDocument.value) return;
  chunkLoading.value = true;
  try { const result = await documentApi.chunks(selectedDocument.value.id, chunkPage.value, chunkSize); chunks.value = result.records; chunkTotal.value = result.total; }
  catch (error) { ElMessage.error((error as Error).message); }
  finally { chunkLoading.value = false; }
}

async function refreshProcessingDocuments() {
  if (documents.value.some((item) => ["WAITING", "PROCESSING"].includes(item.parseStatus) || ["WAITING", "PROCESSING"].includes(item.vectorStatus))) await loadDocuments();
}

async function retryVectorize(document: ProjectDocument) {
  try { await documentApi.vectorize(document.id); ElMessage.success("已重新提交向量化任务"); await loadDocuments(); }
  catch (error) { ElMessage.error((error as Error).message); }
}

async function openSummary(document: ProjectDocument) {
  selectedDocument.value = document; summaryDrawer.value = true; summaryEditing.value = false;
  await loadSummaries();
}
async function loadSummaries() {
  if (!selectedDocument.value) return;
  summaryLoading.value = true;
  try {
    summaries.value = await documentSummaryApi.list(selectedDocument.value.id);
    activeSummary.value = summaries.value[0];
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { summaryLoading.value = false; }
}
async function selectSummary(summaryId: number) {
  summaryLoading.value = true;
  try { activeSummary.value = await documentSummaryApi.detail(summaryId); summaryEditing.value = false; }
  catch (error) { ElMessage.error((error as Error).message); }
  finally { summaryLoading.value = false; }
}
async function generateSummary() {
  if (!selectedDocument.value || summaryGenerating.value) return;
  summaryGenerating.value = true;
  try {
    const summary = await documentSummaryApi.generate(selectedDocument.value.id);
    summaries.value.unshift(summary); activeSummary.value = summary;
    ElMessage.success("文档总结已生成并保存");
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { summaryGenerating.value = false; }
}
function editSummary() {
  if (!activeSummary.value) return;
  summaryEdit.value = { title: activeSummary.value.title, content: activeSummary.value.content };
  summaryEditing.value = true;
}
async function saveSummary() {
  if (!activeSummary.value || !summaryEdit.value.title.trim() || !summaryEdit.value.content.trim()) return;
  summarySaving.value = true;
  try {
    const updated = await documentSummaryApi.update(activeSummary.value.id, {
      title: summaryEdit.value.title.trim(), content: summaryEdit.value.content.trim(),
    });
    activeSummary.value = updated;
    const index = summaries.value.findIndex(item => item.id === updated.id);
    if (index >= 0) summaries.value[index] = updated;
    summaryEditing.value = false; ElMessage.success("总结已保存");
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { summarySaving.value = false; }
}
async function deleteSummary() {
  if (!activeSummary.value) return;
  try {
    await ElMessageBox.confirm(`确认删除“${activeSummary.value.title}”吗？`, "删除总结", { type: "warning" });
    const id = activeSummary.value.id; await documentSummaryApi.remove(id);
    summaries.value = summaries.value.filter(item => item.id !== id); activeSummary.value = summaries.value[0];
    ElMessage.success("总结已删除");
  } catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); }
}
async function exportSummary(format: "pdf" | "docx") {
  if (!activeSummary.value) return;
  summaryExporting.value = true;
  try {
    await documentSummaryApi.export(activeSummary.value.id, activeSummary.value.title, format);
    ElMessage.success(`${format === "pdf" ? "PDF" : "Word"} 文件已开始下载`);
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { summaryExporting.value = false; }
}

async function semanticSearch() {
  const query = semanticQuery.value.trim();
  if (!query) { ElMessage.warning("请输入要检索的问题"); return; }
  if (!workspace.activeProjectId) return;
  semanticLoading.value = true; semanticSearched.value = true;
  try { semanticResults.value = await searchProjectDocuments(workspace.activeProjectId, query); }
  catch (error) { semanticResults.value = []; ElMessage.error((error as Error).message); }
  finally { semanticLoading.value = false; }
}

function formatSize(bytes: number) { if (bytes < 1024) return `${bytes} B`; if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`; return `${(bytes / 1024 / 1024).toFixed(1)} MB`; }

onMounted(() => { loadDocuments(); refreshTimer = window.setInterval(refreshProcessingDocuments, 5000); });
onUnmounted(() => { if (refreshTimer) window.clearInterval(refreshTimer); });
watch(() => workspace.activeProjectId, () => { semanticQuery.value = ""; semanticResults.value = []; semanticSearched.value = false; summaryDrawer.value = false; loadDocuments(); });
</script>

<template>
  <EmptyState v-if="!workspace.activeProject" title="先创建或选择一个项目" description="项目文档会集中保存在当前研究空间。" />
  <section v-else class="documents-page">
    <header class="page-heading"><div><p>KNOWLEDGE LIBRARY</p><h2>{{ workspace.activeProject.name }} · 文档</h2><span>上传研究资料，系统会自动进入解析与向量化队列。</span></div></header>

    <section class="document-overview">
      <div class="overview-copy"><span class="overview-icon"><el-icon><Files /></el-icon></span><div><p>PROJECT KNOWLEDGE</p><h3>把研究资料沉淀为可检索的知识</h3><span>支持 PDF、Word、TXT 和 Markdown，单文件最大 50MB。</span></div></div>
      <el-upload drag :show-file-list="false" :before-upload="validateFile" :http-request="uploadFile" :disabled="uploading" accept=".pdf,.docx,.txt,.md,.markdown" class="upload-box"><el-icon class="upload-icon"><UploadFilled /></el-icon><div><strong>{{ uploading ? `正在上传 ${uploadProgress}%` : "拖放文件到这里，或点击选择" }}</strong><small>{{ uploading ? "请保持页面开启" : "上传后立即发送 document.parse 任务" }}</small></div><el-progress v-if="uploading" :percentage="uploadProgress" :show-text="false" /></el-upload>
    </section>

    <section class="document-stats"><article><span>文档总数</span><strong>{{ documents.length }}</strong><small>当前项目资料</small></article><article><span>已解析</span><strong>{{ successCount }}</strong><small>可用于后续检索</small></article><article><span>处理中</span><strong>{{ processingCount }}</strong><small>等待 AI Worker</small></article><article><span>存储用量</span><strong>{{ formatSize(totalSize) }}</strong><small>本地文件存储</small></article></section>

    <section class="semantic-surface">
      <div class="semantic-copy"><p>SEMANTIC DISCOVERY</p><h3>用自然语言检索项目知识</h3><span>不仅匹配文件名，还会从已向量化的文档片段中寻找语义最相关的内容。</span></div>
      <div class="semantic-search"><el-input v-model="semanticQuery" size="large" clearable placeholder="例如：无人机视觉导航采用了哪些关键方法？" @keyup.enter="semanticSearch"><template #prefix><el-icon><Search /></el-icon></template></el-input><el-button type="primary" size="large" :loading="semanticLoading" @click="semanticSearch">语义检索</el-button></div>
      <div v-if="semanticResults.length" class="semantic-results"><article v-for="result in semanticResults" :key="`${result.documentId}-${result.chunkIndex}`"><header><strong>{{ result.documentName }}</strong><span>{{ result.pageNumber ? `第 ${result.pageNumber} 页` : `片段 ${result.chunkIndex + 1}` }} · 相关度 {{ Math.round(result.score * 100) }}%</span></header><p>{{ result.content }}</p></article></div>
      <div v-else-if="semanticSearched && !semanticLoading" class="semantic-empty">没有找到相关片段。请确认文档向量状态为“已完成”，或换一种问法。</div>
    </section>

    <section class="document-surface">
      <header class="toolbar"><div><h3>文档列表</h3><span>{{ filteredDocuments.length }} 个文件</span></div><div class="filters"><el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索文件名" /><el-select v-model="typeFilter" clearable placeholder="全部类型"><el-option v-for="(label,value) in typeText" :key="value" :label="label" :value="value" /></el-select><el-select v-model="statusFilter" clearable placeholder="全部状态"><el-option v-for="(label,value) in statusText" :key="value" :label="label" :value="value" /></el-select></div></header>
      <div v-loading="loading" class="document-table"><div class="document-row table-head"><span>文件</span><span>上传者</span><span>解析状态</span><span>向量状态</span><span>上传时间</span><span /></div><div v-for="document in filteredDocuments" :key="document.id" class="document-row"><span class="file-info"><i :class="document.fileType.toLowerCase()">{{ document.fileType === "MARKDOWN" ? "MD" : document.fileType }}</i><span><strong>{{ document.originalName }}</strong><small>{{ typeText[document.fileType] }} · {{ formatSize(document.fileSize) }}</small></span></span><span>{{ document.uploaderName || `用户 ${document.uploaderId}` }}</span><span><el-tooltip :disabled="!document.parseError" :content="document.parseError" placement="top"><em class="status-tag" :class="document.parseStatus.toLowerCase()"><b />{{ statusText[document.parseStatus] }}</em></el-tooltip></span><span><el-tooltip :disabled="!document.vectorError" :content="document.vectorError" placement="top"><em class="status-tag" :class="document.vectorStatus.toLowerCase()"><b />{{ statusText[document.vectorStatus] }}</em></el-tooltip></span><span>{{ formatDate(document.createdAt) }}</span><span class="row-actions"><el-button text type="primary" :icon="MagicStick" title="AI 总结" :disabled="document.parseStatus !== 'SUCCESS' || document.vectorStatus !== 'SUCCESS'" @click="openSummary(document)" /><el-button v-if="document.parseStatus === 'SUCCESS' && !['SUCCESS','PROCESSING'].includes(document.vectorStatus)" text type="primary" :icon="RefreshRight" title="重新向量化" @click="retryVectorize(document)" /><el-button text :icon="View" title="查看解析文本" :disabled="document.parseStatus !== 'SUCCESS'" @click="openChunks(document)" /><el-button text :icon="Download" title="下载" @click="download(document)" /><el-button text type="danger" :icon="Delete" title="删除" @click="remove(document)" /></span></div><div v-if="!filteredDocuments.length && !loading" class="empty-list"><el-icon><Files /></el-icon><strong>{{ documents.length ? "没有符合条件的文档" : "还没有上传文档" }}</strong><span>{{ documents.length ? "尝试清除筛选条件" : "从上方上传区添加第一份研究资料" }}</span></div></div>
    </section>

    <el-drawer v-model="chunkDrawer" size="min(720px, 92vw)" class="chunk-drawer">
      <template #header><div class="drawer-heading"><p>PARSED CONTENT</p><h3>{{ selectedDocument?.originalName }}</h3><span>共 {{ chunkTotal }} 个文本片段，可作为后续向量检索的原始语料。</span></div></template>
      <div v-loading="chunkLoading" class="chunk-list"><article v-for="chunk in chunks" :key="chunk.id"><header><strong>片段 {{ chunk.chunkIndex + 1 }}</strong><span>{{ chunk.pageNumber ? `第 ${chunk.pageNumber} 页` : "无页码" }} · {{ chunk.charCount }} 字符</span></header><p>{{ chunk.content }}</p></article><div v-if="!chunks.length && !chunkLoading" class="chunk-empty">暂无解析片段</div></div>
      <el-pagination v-if="chunkTotal > chunkSize" v-model:current-page="chunkPage" :page-size="chunkSize" :total="chunkTotal" layout="prev, pager, next" background @current-change="loadChunks" />
    </el-drawer>

    <el-drawer v-model="summaryDrawer" size="min(880px, 96vw)" class="summary-drawer">
      <template #header><div class="drawer-heading"><p>AI DOCUMENT INSIGHT</p><h3>{{ selectedDocument?.originalName }}</h3><span>结构化总结会保存为独立版本，原始文档内容不会被修改。</span></div></template>
      <div class="summary-toolbar"><el-select v-if="summaries.length" :model-value="activeSummary?.id" placeholder="选择历史版本" @change="selectSummary"><el-option v-for="item in summaries" :key="item.id" :label="`${formatDate(item.createdAt)} · ${item.title}`" :value="item.id" /></el-select><span /><el-dropdown v-if="activeSummary && !summaryEditing" trigger="click" @command="exportSummary"><el-button :icon="Download" :loading="summaryExporting">导出</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="pdf">导出 PDF</el-dropdown-item><el-dropdown-item command="docx">导出 Word</el-dropdown-item></el-dropdown-menu></template></el-dropdown><el-button v-if="activeSummary && !summaryEditing" :icon="EditPen" @click="editSummary">编辑</el-button><el-button v-if="activeSummary && !summaryEditing" text type="danger" :icon="Delete" @click="deleteSummary">删除</el-button><el-button type="primary" :icon="MagicStick" :loading="summaryGenerating" @click="generateSummary">{{ summaries.length ? "重新生成" : "生成总结" }}</el-button></div>
      <div v-loading="summaryLoading" class="summary-body">
        <template v-if="summaryEditing"><el-input v-model="summaryEdit.title" maxlength="180" /><el-input v-model="summaryEdit.content" class="summary-editor" type="textarea" :rows="24" /><div class="summary-save"><el-button @click="summaryEditing=false">取消</el-button><el-button type="primary" :loading="summarySaving" @click="saveSummary">保存修改</el-button></div></template>
        <template v-else-if="activeSummary"><header class="summary-title"><span>{{ activeSummary.creatorName || "项目成员" }} · {{ activeSummary.model || "AI Model" }}</span><h2>{{ activeSummary.title }}</h2></header><section class="summary-content"><article v-for="section in summarySections" :key="section.title"><h4>{{ section.title }}</h4><p>{{ section.content.trim() || "文档片段中未提供。" }}</p></article></section><section v-if="activeSummary.sources.length" class="summary-sources"><h4>分析覆盖片段</h4><div><details v-for="source in activeSummary.sources" :key="source.chunkIndex"><summary>{{ source.pageNumber ? `第 ${source.pageNumber} 页` : "无页码" }} · 片段 {{ source.chunkIndex + 1 }}</summary><p>{{ source.excerpt }}</p></details></div></section></template>
        <div v-else class="summary-empty"><el-icon><MagicStick /></el-icon><h3>还没有文档总结</h3><p>AI 将分析解析后的真实文档片段，并保留引用位置。</p><el-button type="primary" :loading="summaryGenerating" @click="generateSummary">生成第一版总结</el-button></div>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.page-heading{margin-bottom:24px}.page-heading p{margin:0 0 5px;color:#8996a9;font-size:8px;font-weight:800;letter-spacing:.18em}.page-heading h2{margin:0;font-size:27px;letter-spacing:-.04em}.page-heading span{display:block;margin-top:8px;color:#7b8799;font-size:11px}.document-overview{min-height:188px;padding:27px 30px;display:grid;grid-template-columns:minmax(360px,.9fr) minmax(420px,1.1fr);align-items:center;gap:30px;overflow:hidden;border-radius:18px;color:white;background:radial-gradient(circle at 12% 120%,rgba(101,212,194,.15),transparent 34%),linear-gradient(120deg,#10264d,#19457c);box-shadow:0 15px 38px rgba(22,49,91,.13)}.overview-copy{display:flex;align-items:center;gap:18px}.overview-icon{width:54px;height:54px;flex:0 0 auto;display:grid;place-items:center;border:1px solid rgba(115,221,203,.25);border-radius:16px;color:#71dac9;background:rgba(101,212,194,.08);font-size:25px}.overview-copy p{margin:0 0 7px;color:#69d1c1;font-size:8px;font-weight:800;letter-spacing:.17em}.overview-copy h3{margin:0 0 8px;font:600 20px/1.4 Georgia,"Noto Serif SC",serif}.overview-copy>div>span{color:#9fb0c8;font-size:9px}.upload-box :deep(.el-upload){width:100%}.upload-box :deep(.el-upload-dragger){height:128px;padding:20px;display:flex;align-items:center;justify-content:center;gap:17px;border:1px dashed rgba(116,221,203,.38);border-radius:13px;background:rgba(255,255,255,.055);text-align:left}.upload-box :deep(.el-upload-dragger:hover){border-color:#76dccb;background:rgba(255,255,255,.08)}.upload-icon{color:#73d9c8;font-size:30px}.upload-box strong,.upload-box small{display:block}.upload-box strong{color:#eef5ff;font-size:11px}.upload-box small{margin-top:7px;color:#91a5c2;font-size:8px}.upload-box .el-progress{position:absolute;left:20px;right:20px;bottom:13px}.document-stats{margin:18px 0;display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.document-stats article{min-height:91px;padding:18px 20px;border:1px solid var(--rf-line);border-radius:14px;background:white}.document-stats span,.document-stats strong,.document-stats small{display:block}.document-stats span{color:#7c889a;font-size:8px;font-weight:700}.document-stats strong{margin:5px 0 3px;font:700 21px/1 Georgia,serif}.document-stats small{color:#a0a9b7;font-size:7px}.document-surface{overflow:hidden;border:1px solid var(--rf-line);border-radius:16px;background:white}.toolbar{min-height:76px;padding:16px 22px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #edf0f4}.toolbar>div:first-child{display:flex;align-items:baseline;gap:9px}.toolbar h3{margin:0;font-size:14px}.toolbar>div:first-child span{color:#98a2b2;font-size:8px}.filters{display:flex;gap:9px}.filters .el-input{width:180px}.filters .el-select{width:125px}.document-row{min-height:70px;padding:0 22px;display:grid;grid-template-columns:minmax(250px,1.7fr) .8fr .8fr .8fr .8fr 108px;align-items:center;gap:14px;border-bottom:1px solid #edf0f4;color:#6d7a8d;font-size:9px}.document-row:last-child{border:0}.table-head{min-height:43px;color:#98a2b1;background:#fafbfc;font-size:7px;font-weight:800}.file-info{min-width:0;display:flex;align-items:center;gap:12px}.file-info>i{width:39px;height:45px;flex:0 0 auto;display:grid;place-items:center;border-radius:7px;color:#c34841;background:#fde9e7;font-size:8px;font-style:normal;font-weight:900}.file-info>i.docx{color:#315fad;background:#e5edfc}.file-info>i.txt{color:#347567;background:#e0f3ef}.file-info>i.markdown{color:#6b5499;background:#ece6f7}.file-info>span{min-width:0}.file-info strong,.file-info small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.file-info strong{color:#29374e;font-size:9px}.file-info small{margin-top:5px;color:#9ca6b4;font-size:7px}.status-tag{display:inline-flex;align-items:center;gap:6px;padding:5px 8px;border-radius:999px;color:#8b671e;background:#fff3da;font-size:7px;font-style:normal;font-weight:800}.status-tag b{width:5px;height:5px;border-radius:50%;background:#e3ad45}.status-tag.processing{color:#315eb2;background:#e8efff}.status-tag.processing b{background:#4e78d8;animation:pulse 1.2s infinite}.status-tag.success{color:#17776b;background:#e0f4ef}.status-tag.success b{background:#54beac}.status-tag.failed{color:#a74d55;background:#fde9eb}.status-tag.failed b{background:#dd6565}.row-actions{display:flex}.empty-list{min-height:210px;display:grid;place-content:center;justify-items:center;color:#9ba6b5}.empty-list .el-icon{margin-bottom:12px;font-size:30px}.empty-list strong{color:#667388;font-size:10px}.empty-list span{margin-top:6px;font-size:8px}@keyframes pulse{50%{opacity:.35}}
.semantic-surface{margin:0 0 18px;padding:23px;border:1px solid var(--rf-line);border-radius:16px;background:linear-gradient(135deg,#fff 55%,#f2f8ff)}.semantic-copy p{margin:0 0 6px;color:#3f75c9;font-size:8px;font-weight:900;letter-spacing:.17em}.semantic-copy h3{margin:0;color:#1f304a;font-size:16px}.semantic-copy span{display:block;margin-top:6px;color:#8793a5;font-size:9px}.semantic-search{margin-top:17px;display:flex;gap:10px}.semantic-search .el-button{min-width:104px}.semantic-results{margin-top:18px;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:11px}.semantic-results article{padding:16px 17px;border:1px solid #e1e8f1;border-radius:12px;background:#fff}.semantic-results header{display:flex;align-items:center;justify-content:space-between;gap:12px}.semantic-results strong{overflow:hidden;color:#2d4161;font-size:9px;text-overflow:ellipsis;white-space:nowrap}.semantic-results header span{flex:0 0 auto;color:#4e78bd;font-size:7px}.semantic-results p{margin:10px 0 0;display:-webkit-box;overflow:hidden;color:#607087;font:400 9px/1.75 Georgia,"Noto Serif SC",serif;-webkit-box-orient:vertical;-webkit-line-clamp:4}.semantic-empty{margin-top:16px;padding:20px;border-radius:11px;color:#8995a7;background:#f7f9fc;text-align:center;font-size:9px}
@media(max-width:1100px){.document-overview{grid-template-columns:1fr}.document-stats{grid-template-columns:repeat(2,1fr)}.document-table{overflow-x:auto}.document-row{min-width:930px}}@media(max-width:700px){.page-heading h2{font-size:22px}.document-overview{padding:24px 20px}.overview-copy{align-items:flex-start}.upload-box :deep(.el-upload-dragger){height:120px}.toolbar{align-items:flex-start;flex-direction:column;gap:13px}.filters{width:100%;display:grid;grid-template-columns:1fr 1fr}.filters .el-input{width:auto;grid-column:1/-1}.filters .el-select{width:auto}}
.row-actions{justify-content:flex-end;gap:2px}.row-actions :deep(.el-button){width:32px;min-height:32px;margin-left:0;padding:0;border-radius:9px}.drawer-heading p{margin:0 0 7px;color:#4eb8a8;font-size:8px;font-weight:900;letter-spacing:.17em}.drawer-heading h3{margin:0;color:#20304a;font-size:18px}.drawer-heading span{display:block;margin-top:7px;color:#8b96a7;font-size:9px}.chunk-list{min-height:180px}.chunk-list article{margin-bottom:13px;padding:18px;border:1px solid #e4e9f0;border-radius:13px;background:#f8fafc}.chunk-list header{display:flex;align-items:center;justify-content:space-between}.chunk-list header strong{color:#285b98;font-size:9px}.chunk-list header span{color:#96a1b1;font-size:8px}.chunk-list article p{margin:13px 0 0;color:#46546a;font:400 11px/1.9 Georgia,"Noto Serif SC",serif;white-space:pre-wrap}.chunk-empty{padding:70px 0;text-align:center;color:#99a4b3;font-size:10px}.chunk-drawer .el-pagination{margin-top:18px;justify-content:center}
.document-row{grid-template-columns:minmax(250px,1.7fr) .8fr .8fr .8fr .8fr 150px}
.summary-toolbar{padding:0 0 18px;display:flex;align-items:center;gap:9px;border-bottom:1px solid rgba(29,29,31,.08)}.summary-toolbar .el-select{width:310px}.summary-toolbar>span{flex:1}.summary-body{min-height:480px;padding:24px 3px}.summary-title span{color:#0071e3;font-size:11px}.summary-title h2{margin:8px 0 18px;font-size:25px;letter-spacing:-.04em}.summary-content article{padding:22px 0;border-top:1px solid rgba(29,29,31,.07)}.summary-content h4{margin:0 0 11px;font-size:17px}.summary-content p{margin:0;color:#444446;font-size:14px;line-height:1.9;white-space:pre-wrap}.summary-sources{margin-top:20px;padding:20px;border-radius:16px;background:#f5f5f7}.summary-sources h4{margin:0 0 12px;font-size:14px}.summary-sources details{padding:9px 0;border-bottom:1px solid rgba(29,29,31,.07)}.summary-sources details:last-child{border:0}.summary-sources summary{color:#0066cc;font-size:12px;cursor:pointer}.summary-sources p{margin:8px 0 0;color:#6e6e73;font-size:11px;line-height:1.7}.summary-empty{min-height:430px;display:grid;place-content:center;justify-items:center;color:#86868b;text-align:center}.summary-empty .el-icon{color:#2997ff;font-size:42px}.summary-empty h3{margin:18px 0 7px;color:#1d1d1f}.summary-empty p{margin:0 0 20px;font-size:12px}.summary-editor{margin-top:14px}.summary-save{margin-top:14px;text-align:right}@media(max-width:700px){.summary-toolbar{align-items:stretch;flex-direction:column}.summary-toolbar .el-select{width:100%}.summary-toolbar>span{display:none}}
</style>

<style scoped>
.document-overview { min-height:230px; padding:38px 42px; gap:38px; border-radius:30px; background:radial-gradient(circle at 15% 120%,rgba(41,151,255,.18),transparent 36%),linear-gradient(130deg,#111217,#173a64); box-shadow:0 22px 55px rgba(13,33,64,.14); }
.overview-icon { width:62px; height:62px; border-color:rgba(255,255,255,.18); border-radius:19px; color:#64d8ff; background:rgba(255,255,255,.08); }
.overview-copy p { color:#64d8ff; font-size:10px; }.overview-copy h3 { margin-bottom:11px; font:700 27px/1.25 -apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif; letter-spacing:-.035em; }.overview-copy>div>span { color:rgba(255,255,255,.6); font-size:12px; }
.upload-box :deep(.el-upload-dragger) { height:150px; padding:24px; border-color:rgba(255,255,255,.28); border-radius:21px; background:rgba(255,255,255,.08); backdrop-filter:blur(10px); }.upload-box :deep(.el-upload-dragger:hover){border-color:#64d8ff;background:rgba(255,255,255,.12)}
.upload-icon { color:#64d8ff; font-size:34px; }.upload-box strong { font-size:14px; }.upload-box small { color:rgba(255,255,255,.55); font-size:11px; }
.document-stats { margin:20px 0; gap:16px; }
.semantic-surface { margin-bottom:20px; padding:28px; background:linear-gradient(135deg,rgba(255,255,255,.94),rgba(236,246,255,.88))!important; }
.semantic-copy p { color:var(--rf-blue); font-size:10px; }.semantic-copy h3 { font-size:22px; letter-spacing:-.025em; }.semantic-copy span { margin-top:8px; font-size:12px; }
.semantic-search { margin-top:20px; }.semantic-results { gap:14px; }.semantic-results article { padding:19px; border-color:rgba(29,29,31,.08); border-radius:18px; }.semantic-results strong { font-size:12px; }.semantic-results header span { font-size:10px; }.semantic-results p { font:400 13px/1.75 -apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif; }
.toolbar { min-height:88px; padding:20px 26px; }.filters .el-input { width:210px; }.filters .el-select { width:140px; }
@media(max-width:700px){.document-overview{padding:28px 22px}.overview-copy h3{font-size:23px}.semantic-results{grid-template-columns:1fr}}
</style>
