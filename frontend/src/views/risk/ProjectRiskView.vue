<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Download, EditPen, MagicStick, Plus, Warning } from "@element-plus/icons-vue";
import EmptyState from "@/components/common/EmptyState.vue";
import { riskReportApi, type ProjectRiskReport, type RiskLevel } from "@/api/riskReport";
import { useWorkspaceStore } from "@/stores/workspace";
import { formatDate } from "@/utils/format";

interface ReportSection { title: string; content: string }

const workspace = useWorkspaceStore();
const route = useRoute();
const reports = ref<ProjectRiskReport[]>([]); const activeReport = ref<ProjectRiskReport>();
const loading = ref(false); const generating = ref(false); const saving = ref(false); const exporting = ref(false);
const generateDialog = ref(false); const editing = ref(false);
const form = reactive({ title: "" }); const editForm = reactive({ title: "", content: "" });

const levelMeta: Record<RiskLevel, { label: string; note: string }> = {
  LOW: { label: "低风险", note: "当前风险总体可控" },
  MEDIUM: { label: "中风险", note: "存在需要跟进的风险项" },
  HIGH: { label: "高风险", note: "建议尽快调整计划与资源" },
  CRITICAL: { label: "严重风险", note: "需要立即处理关键阻塞" },
};
const sections = computed<ReportSection[]>(() => {
  const content = activeReport.value?.content || ""; const result: ReportSection[] = [];
  let current: ReportSection | undefined;
  for (const line of content.split(/\r?\n/)) {
    if (/^##\s+/.test(line)) { current = { title: line.replace(/^##\s+/, "").trim(), content: "" }; result.push(current); }
    else if (current) current.content += `${current.content ? "\n" : ""}${line}`;
  }
  return result.length ? result : [{ title: "项目风险分析", content }];
});
const metrics = computed(() => {
  const value = activeReport.value?.analysisSnapshot || {};
  return [
    { label: "逾期任务", value: value.overdueTasks || 0, tone: "red" },
    { label: "7日内到期", value: value.dueSoonTasks || 0, tone: "orange" },
    { label: "紧急任务", value: value.urgentTasks || 0, tone: "purple" },
    { label: "未指派", value: value.unassignedTasks || 0, tone: "blue" },
    { label: "高负载成员", value: value.overloadedMembers || 0, tone: "navy" },
    { label: "项目进度", value: `${value.projectProgress || 0}%`, tone: "green" },
  ];
});

async function loadReports() {
  if (!workspace.activeProjectId) { reports.value = []; activeReport.value = undefined; return; }
  loading.value = true;
  try {
    reports.value = await riskReportApi.list(workspace.activeProjectId);
    const requestedId = Number(route.query.reportId || 0);
    if (requestedId && reports.value.some(item => item.id === requestedId)) activeReport.value = reports.value.find(item => item.id === requestedId);
    else if (!activeReport.value || !reports.value.some(item => item.id === activeReport.value?.id)) activeReport.value = reports.value[0];
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { loading.value = false; }
}
async function selectReport(report: ProjectRiskReport) {
  try { activeReport.value = await riskReportApi.detail(report.id); editing.value = false; }
  catch (error) { ElMessage.error((error as Error).message); }
}
function openGenerate() { form.title = ""; generateDialog.value = true; }
async function generate() {
  if (!workspace.activeProjectId) return;
  generating.value = true;
  try {
    const report = await riskReportApi.generate(workspace.activeProjectId, { title: form.title.trim() || undefined });
    reports.value.unshift(report); activeReport.value = report; generateDialog.value = false;
    ElMessage.success("风险分析已生成并保存");
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { generating.value = false; }
}
function startEdit() {
  if (!activeReport.value) return;
  Object.assign(editForm, { title: activeReport.value.title, content: activeReport.value.content }); editing.value = true;
}
async function save() {
  if (!activeReport.value || !editForm.title.trim() || !editForm.content.trim()) return;
  saving.value = true;
  try {
    const updated = await riskReportApi.update(activeReport.value.id, { title: editForm.title.trim(), content: editForm.content.trim() });
    activeReport.value = updated; const index = reports.value.findIndex(item => item.id === updated.id);
    if (index >= 0) reports.value[index] = updated;
    editing.value = false; ElMessage.success("风险报告已保存");
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { saving.value = false; }
}
async function remove() {
  if (!activeReport.value) return;
  try {
    await ElMessageBox.confirm(`确认删除“${activeReport.value.title}”吗？`, "删除风险报告", { type: "warning" });
    const id = activeReport.value.id; await riskReportApi.remove(id);
    reports.value = reports.value.filter(item => item.id !== id); activeReport.value = reports.value[0];
    ElMessage.success("风险报告已删除");
  } catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); }
}
async function exportReport(format: "pdf" | "docx") {
  if (!activeReport.value) return;
  exporting.value = true;
  try {
    await riskReportApi.export(activeReport.value.id, activeReport.value.title, format);
    ElMessage.success(`${format === "pdf" ? "PDF" : "Word"} 文件已开始下载`);
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { exporting.value = false; }
}

watch(() => workspace.activeProjectId, loadReports, { immediate: true });
</script>

<template>
  <EmptyState v-if="!workspace.activeProject" title="先选择一个研究项目" description="风险分析会读取当前项目的任务、成员负载、周期和知识库。" />
  <section v-else class="risk-page" v-loading="loading">
    <header class="page-heading">
      <div><p>AI RISK INTELLIGENCE</p><h2>项目风险分析</h2><span>用可解释评分识别延期、负载、周期与技术风险，并保存每次分析快照。</span></div>
      <el-button type="primary" :icon="MagicStick" @click="openGenerate">生成风险分析</el-button>
    </header>

    <section class="risk-hero" :class="activeReport?.riskLevel.toLowerCase() || 'empty'">
      <div class="hero-copy"><span>CURRENT RISK PROFILE</span><h3>{{ workspace.activeProject.name }}</h3><p>{{ activeReport ? levelMeta[activeReport.riskLevel].note : "生成第一份风险分析，建立项目风险基线。" }}</p></div>
      <div v-if="activeReport" class="risk-score"><strong>{{ activeReport.riskScore }}</strong><span>/ 100</span><b>{{ levelMeta[activeReport.riskLevel].label }}</b></div>
      <div v-else class="risk-score muted"><el-icon><Warning /></el-icon><b>尚未分析</b></div>
    </section>

    <section v-if="activeReport" class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" :class="metric.tone"><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong></article>
    </section>

    <div class="risk-layout">
      <aside class="report-list">
        <header><div><span>风险档案</span><strong>{{ reports.length }} 份</strong></div><button title="生成风险分析" @click="openGenerate"><el-icon><Plus /></el-icon></button></header>
        <button v-for="report in reports" :key="report.id" class="report-item" :class="{ active: activeReport?.id === report.id }" @click="selectReport(report)">
          <span><i :class="report.riskLevel.toLowerCase()" />{{ levelMeta[report.riskLevel].label }} · {{ report.riskScore }} 分</span>
          <strong>{{ report.title }}</strong><small>{{ report.creatorName || "项目成员" }} · {{ formatDate(report.createdAt) }}</small>
        </button>
        <div v-if="!reports.length" class="list-empty">还没有风险报告<br />生成第一份项目风险基线</div>
      </aside>

      <article v-if="activeReport" class="report-document">
        <header class="document-head"><div><span>{{ levelMeta[activeReport.riskLevel].label }} · {{ activeReport.riskScore }} 分</span><h3>{{ activeReport.title }}</h3><small>由 {{ activeReport.creatorName || "项目成员" }} 生成 · {{ activeReport.model || "AI Model" }} · 指标快照不会随任务变化</small></div><div class="document-actions"><el-dropdown v-if="!editing" trigger="click" @command="exportReport"><el-button :icon="Download" :loading="exporting">导出</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="pdf">导出 PDF</el-dropdown-item><el-dropdown-item command="docx">导出 Word</el-dropdown-item></el-dropdown-menu></template></el-dropdown><el-button v-if="!editing" :icon="EditPen" @click="startEdit">编辑</el-button><el-button v-if="!editing" text type="danger" :icon="Delete" @click="remove">删除</el-button></div></header>
        <template v-if="editing"><div class="editor"><el-input v-model="editForm.title" maxlength="180" /><el-input v-model="editForm.content" type="textarea" :rows="24" resize="vertical" /></div><footer class="editor-actions"><el-button @click="editing=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存修改</el-button></footer></template>
        <template v-else><div class="report-content"><section v-for="section in sections" :key="section.title"><h4>{{ section.title }}</h4><p>{{ section.content.trim() || "暂无足够数据。" }}</p></section></div><section v-if="activeReport.analysisSnapshot.workload?.length" class="workload"><h4>成员任务负载快照</h4><div><article v-for="member in activeReport.analysisSnapshot.workload" :key="member.userId"><span>{{ member.name || `成员 ${member.userId}` }}</span><b>{{ member.activeTasks }} 项进行中</b></article></div></section><section v-if="activeReport.sources.length" class="sources"><h4>知识库证据</h4><span v-for="(source,index) in activeReport.sources" :key="`${source.documentId}-${source.chunkIndex}`">[{{ index+1 }}] {{ source.documentName }}<template v-if="source.pageNumber"> · 第 {{ source.pageNumber }} 页</template></span></section></template>
      </article>
      <div v-else class="document-empty"><el-icon><Warning /></el-icon><h3>建立项目风险基线</h3><p>系统会先计算真实指标，再调用 AI 结合知识库形成处置建议。</p><el-button type="primary" :icon="MagicStick" @click="openGenerate">开始分析</el-button></div>
    </div>
  </section>

  <el-dialog v-model="generateDialog" title="生成项目风险分析" width="520px" destroy-on-close>
    <p class="dialog-copy">分析会读取当前任务、成员负载、项目周期和已向量化文档。生成结果不会自动修改任何业务数据。</p>
    <el-form label-position="top"><el-form-item label="报告标题（可选）"><el-input v-model="form.title" maxlength="180" placeholder="留空将使用项目名与当前日期" /></el-form-item></el-form>
    <template #footer><el-button @click="generateDialog=false">取消</el-button><el-button type="primary" :loading="generating" @click="generate">{{ generating ? "正在计算指标并分析" : "生成并保存" }}</el-button></template>
  </el-dialog>
</template>

<style scoped>
.page-heading{margin-bottom:24px;display:flex;align-items:flex-end;justify-content:space-between}.page-heading p{margin:0 0 7px;color:#0071e3;font-size:10px;font-weight:700;letter-spacing:.14em}.page-heading h2{margin:0;font-size:34px;letter-spacing:-.045em}.page-heading span{display:block;margin-top:9px;color:#6e6e73;font-size:14px}.risk-hero{min-height:190px;padding:38px 44px;display:flex;align-items:center;justify-content:space-between;border-radius:28px;color:#fff;background:radial-gradient(circle at 82% 0,rgba(255,255,255,.18),transparent 34%),linear-gradient(130deg,#111217,#174a78);box-shadow:0 24px 60px rgba(14,42,80,.14)}.risk-hero.medium{background:radial-gradient(circle at 82% 0,rgba(255,255,255,.2),transparent 34%),linear-gradient(130deg,#242016,#9a6300)}.risk-hero.high{background:radial-gradient(circle at 82% 0,rgba(255,255,255,.18),transparent 34%),linear-gradient(130deg,#2a1712,#bd4c25)}.risk-hero.critical{background:radial-gradient(circle at 82% 0,rgba(255,255,255,.16),transparent 34%),linear-gradient(130deg,#281214,#a4142d)}.risk-hero.low{background:radial-gradient(circle at 82% 0,rgba(255,255,255,.17),transparent 34%),linear-gradient(130deg,#101d1b,#087d69)}.hero-copy{max-width:720px}.hero-copy>span{color:rgba(255,255,255,.6);font-size:10px;font-weight:700;letter-spacing:.14em}.hero-copy h3{margin:12px 0 9px;font-size:31px;letter-spacing:-.04em}.hero-copy p{margin:0;color:rgba(255,255,255,.72);font-size:14px}.risk-score{min-width:170px;display:grid;grid-template-columns:auto auto;justify-content:center;align-items:end}.risk-score strong{font-size:60px;line-height:.9;letter-spacing:-.07em}.risk-score>span{padding:0 0 5px 7px;color:rgba(255,255,255,.6);font-size:12px}.risk-score b{margin-top:12px;grid-column:1/-1;text-align:center;font-size:13px}.risk-score.muted{display:flex;align-items:center;flex-direction:column;gap:12px;color:rgba(255,255,255,.68)}.risk-score.muted .el-icon{font-size:38px}.metric-grid{margin:18px 0;display:grid;grid-template-columns:repeat(6,1fr);gap:12px}.metric-grid article{min-height:92px;padding:19px;border:1px solid rgba(29,29,31,.08);border-radius:18px;background:rgba(255,255,255,.9)}.metric-grid span,.metric-grid strong{display:block}.metric-grid span{color:#86868b;font-size:11px}.metric-grid strong{margin-top:8px;font-size:25px}.metric-grid .red strong{color:#c92e43}.metric-grid .orange strong{color:#b35b13}.metric-grid .purple strong{color:#7652ad}.metric-grid .blue strong{color:#0071e3}.metric-grid .navy strong{color:#294d77}.metric-grid .green strong{color:#138a75}.risk-layout{margin-top:20px;display:grid;grid-template-columns:310px minmax(0,1fr);gap:20px;align-items:start}.report-list,.report-document,.document-empty{border:1px solid rgba(29,29,31,.08);border-radius:22px;background:rgba(255,255,255,.9);box-shadow:0 12px 35px rgba(0,0,0,.035)}.report-list{padding:12px}.report-list>header{height:58px;padding:0 10px;display:flex;align-items:center;justify-content:space-between}.report-list>header div{display:flex;align-items:center;gap:9px}.report-list>header span{font-size:14px;font-weight:700}.report-list>header strong{color:#86868b;font-size:11px}.report-list>header button{width:31px;height:31px;border:0;border-radius:50%;color:#0071e3;background:#edf6ff}.report-item{width:100%;padding:17px 15px;display:block;border:0;border-radius:14px;color:#1d1d1f;background:transparent;text-align:left}.report-item:hover{background:#f5f5f7}.report-item.active{background:#eaf4ff}.report-item span,.report-item strong,.report-item small{display:block}.report-item span{color:#6e6e73;font-size:10px}.report-item span i{width:7px;height:7px;margin-right:6px;display:inline-block;border-radius:50%;background:#39a88f}.report-item span i.medium{background:#d28b1f}.report-item span i.high{background:#dc653d}.report-item span i.critical{background:#ce2943}.report-item strong{margin:7px 0 6px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px}.report-item small{color:#9a9a9f;font-size:10px}.list-empty{padding:60px 15px;color:#9a9a9f;text-align:center;font-size:12px;line-height:1.8}.report-document{min-height:660px;overflow:hidden}.document-head{padding:28px 32px;display:flex;justify-content:space-between;gap:20px;border-bottom:1px solid rgba(29,29,31,.07)}.document-head span{color:#0071e3;font-size:11px;font-weight:650}.document-head h3{margin:8px 0;font-size:24px;letter-spacing:-.035em}.document-head small{color:#86868b;font-size:11px}.document-actions{display:flex;align-items:flex-start}.report-content{padding:12px 48px 30px}.report-content section{padding:25px 0;border-bottom:1px solid rgba(29,29,31,.07)}.report-content h4,.workload h4,.sources h4{margin:0 0 13px;font-size:17px}.report-content p{margin:0;color:#444446;font-size:14px;line-height:1.9;white-space:pre-wrap}.workload,.sources{margin:0 32px 24px;padding:22px;border-radius:16px;background:#f5f5f7}.workload>div{display:grid;grid-template-columns:repeat(2,1fr);gap:9px}.workload article{padding:11px 13px;display:flex;justify-content:space-between;border-radius:11px;background:#fff;font-size:11px}.workload article b{color:#6e6e73}.sources span{display:block;margin-top:8px;color:#6e6e73;font-size:11px}.editor{padding:28px 32px}.editor .el-textarea{margin-top:15px}.editor-actions{padding:0 32px 28px;text-align:right}.document-empty{min-height:660px;display:grid;place-content:center;justify-items:center;color:#86868b;text-align:center}.document-empty .el-icon{font-size:42px;color:#e06a42}.document-empty h3{margin:18px 0 5px;color:#1d1d1f}.document-empty p{max-width:430px;margin:0 0 20px;font-size:12px;line-height:1.7}.dialog-copy{margin:-5px 0 22px;color:#6e6e73;font-size:12px;line-height:1.7}@media(max-width:1100px){.metric-grid{grid-template-columns:repeat(3,1fr)}}@media(max-width:900px){.risk-layout{grid-template-columns:1fr}.report-list{display:flex;overflow-x:auto}.report-list>header{min-width:120px}.report-item{min-width:230px}.risk-hero{padding:30px}}@media(max-width:640px){.page-heading{align-items:flex-start;gap:18px;flex-direction:column}.risk-hero{min-height:240px;align-items:flex-start;flex-direction:column}.risk-score{margin-top:25px}.metric-grid{grid-template-columns:repeat(2,1fr)}.report-content{padding:8px 25px 28px}.document-head{padding:24px;flex-direction:column}.workload>div{grid-template-columns:1fr}}
</style>
