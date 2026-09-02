<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Download, EditPen, MagicStick, Plus } from "@element-plus/icons-vue";
import EmptyState from "@/components/common/EmptyState.vue";
import { weeklyReportApi, type WeeklyReport } from "@/api/weeklyReport";
import { useWorkspaceStore } from "@/stores/workspace";
import { formatDate } from "@/utils/format";

interface ReportSection { title: string; content: string }

const workspace = useWorkspaceStore();
const route = useRoute();
const reports = ref<WeeklyReport[]>([]); const activeReport = ref<WeeklyReport>();
const loading = ref(false); const generating = ref(false); const saving = ref(false); const exporting = ref(false);
const generateDialog = ref(false); const editing = ref(false);
const form = reactive({ title: "", periodStart: "", periodEnd: "" });
const editForm = reactive({ title: "", content: "" });

const sections = computed<ReportSection[]>(() => {
  const content = activeReport.value?.content || "";
  const result: ReportSection[] = [];
  let current: ReportSection | undefined;
  for (const line of content.split(/\r?\n/)) {
    if (/^##\s+/.test(line)) {
      current = { title: line.replace(/^##\s+/, "").trim(), content: "" };
      result.push(current);
    } else if (current) current.content += `${current.content ? "\n" : ""}${line}`;
  }
  return result.length ? result : [{ title: "项目周报", content }];
});

function iso(date: Date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}
function resetGenerateForm() {
  const end = new Date(); const start = new Date(end); start.setDate(end.getDate() - 6);
  Object.assign(form, { title: "", periodStart: iso(start), periodEnd: iso(end) });
}
async function loadReports() {
  if (!workspace.activeProjectId) { reports.value = []; activeReport.value = undefined; return; }
  loading.value = true;
  try {
    reports.value = await weeklyReportApi.list(workspace.activeProjectId);
    const requestedId = Number(route.query.reportId || 0);
    if (requestedId && reports.value.some(item => item.id === requestedId)) activeReport.value = reports.value.find(item => item.id === requestedId);
    else if (!activeReport.value || !reports.value.some(item => item.id === activeReport.value?.id)) activeReport.value = reports.value[0];
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { loading.value = false; }
}
async function selectReport(report: WeeklyReport) {
  try { activeReport.value = await weeklyReportApi.detail(report.id); editing.value = false; }
  catch (error) { ElMessage.error((error as Error).message); }
}
function openGenerate() { resetGenerateForm(); generateDialog.value = true; }
async function generate() {
  if (!workspace.activeProjectId || !form.periodStart || !form.periodEnd) return;
  if (form.periodEnd < form.periodStart) { ElMessage.warning("结束日期不能早于开始日期"); return; }
  generating.value = true;
  try {
    const report = await weeklyReportApi.generate(workspace.activeProjectId, {
      periodStart: form.periodStart, periodEnd: form.periodEnd, title: form.title.trim() || undefined,
    });
    reports.value.unshift(report); activeReport.value = report; generateDialog.value = false;
    ElMessage.success("周报已生成并保存");
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
    const updated = await weeklyReportApi.update(activeReport.value.id, {
      title: editForm.title.trim(), content: editForm.content.trim(),
    });
    activeReport.value = updated;
    const index = reports.value.findIndex(item => item.id === updated.id);
    if (index >= 0) reports.value[index] = updated;
    editing.value = false; ElMessage.success("周报已保存");
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { saving.value = false; }
}
async function remove() {
  if (!activeReport.value) return;
  try {
    await ElMessageBox.confirm(`确认删除“${activeReport.value.title}”吗？`, "删除周报", { type: "warning" });
    const id = activeReport.value.id; await weeklyReportApi.remove(id);
    reports.value = reports.value.filter(item => item.id !== id); activeReport.value = reports.value[0];
    ElMessage.success("周报已删除");
  } catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); }
}
async function exportReport(format: "pdf" | "docx") {
  if (!activeReport.value) return;
  exporting.value = true;
  try {
    await weeklyReportApi.export(activeReport.value.id, activeReport.value.title, format);
    ElMessage.success(`${format === "pdf" ? "PDF" : "Word"} 文件已开始下载`);
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { exporting.value = false; }
}

watch(() => workspace.activeProjectId, loadReports, { immediate: true });
</script>

<template>
  <EmptyState v-if="!workspace.activeProject" title="先选择一个研究项目" description="项目周报会基于任务、活动和知识库自动生成。" />
  <section v-else class="report-page" v-loading="loading">
    <header class="page-heading">
      <div><p>AI WEEKLY REPORT</p><h2>项目周报</h2><span>将真实任务进展与项目知识沉淀为可编辑、可追溯的周报。</span></div>
      <el-button type="primary" :icon="MagicStick" @click="openGenerate">生成新周报</el-button>
    </header>

    <section class="report-hero">
      <div><span>RESEARCH INTELLIGENCE</span><h3>{{ workspace.activeProject.name }}</h3><p>AI 将读取所选周期内的任务、延期项、Agent 活动和项目知识库，生成结构化进展总结。</p></div>
      <div class="hero-stat"><strong>{{ reports.length }}</strong><span>已保存周报</span></div>
    </section>

    <div class="report-layout">
      <aside class="report-list">
        <header><div><span>报告归档</span><strong>{{ reports.length }} 份</strong></div><button @click="openGenerate"><el-icon><Plus /></el-icon></button></header>
        <button v-for="report in reports" :key="report.id" class="report-item" :class="{ active: activeReport?.id === report.id }" @click="selectReport(report)">
          <span>{{ report.periodStart }} — {{ report.periodEnd }}</span><strong>{{ report.title }}</strong><small>{{ report.creatorName || "项目成员" }} · {{ formatDate(report.createdAt) }}</small>
        </button>
        <div v-if="!reports.length" class="list-empty">还没有周报<br />生成第一份项目进展记录</div>
      </aside>

      <article v-if="activeReport" class="report-document">
        <header class="document-head">
          <div><span>{{ activeReport.periodStart }} — {{ activeReport.periodEnd }}</span><h3>{{ activeReport.title }}</h3><small>由 {{ activeReport.creatorName || "项目成员" }} 生成 · {{ activeReport.model || "AI Model" }}</small></div>
          <div class="document-actions"><el-dropdown v-if="!editing" trigger="click" @command="exportReport"><el-button :icon="Download" :loading="exporting">导出</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="pdf">导出 PDF</el-dropdown-item><el-dropdown-item command="docx">导出 Word</el-dropdown-item></el-dropdown-menu></template></el-dropdown><el-button v-if="!editing" :icon="EditPen" @click="startEdit">编辑</el-button><el-button v-if="!editing" text type="danger" :icon="Delete" @click="remove">删除</el-button></div>
        </header>
        <template v-if="editing">
          <div class="editor"><el-input v-model="editForm.title" maxlength="160" /><el-input v-model="editForm.content" type="textarea" :rows="22" resize="vertical" /></div>
          <footer class="editor-actions"><el-button @click="editing=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存修改</el-button></footer>
        </template>
        <template v-else>
          <div class="report-content"><section v-for="section in sections" :key="section.title"><h4>{{ section.title }}</h4><p>{{ section.content.trim() || "暂无足够数据。" }}</p></section></div>
          <section v-if="activeReport.sources.length" class="report-sources"><h4>知识库来源</h4><div><span v-for="(source,index) in activeReport.sources" :key="`${source.documentId}-${source.chunkIndex}`">[{{ index+1 }}] {{ source.documentName }}<template v-if="source.pageNumber"> · 第 {{ source.pageNumber }} 页</template></span></div></section>
        </template>
      </article>
      <div v-else class="document-empty"><el-icon><MagicStick /></el-icon><h3>生成第一份项目周报</h3><p>选择统计周期，AI 会整理真实项目数据并保存结果。</p><el-button type="primary" @click="openGenerate">开始生成</el-button></div>
    </div>
  </section>

  <el-dialog v-model="generateDialog" title="生成项目周报" width="520px" destroy-on-close>
    <p class="dialog-copy">选择统计周期。生成过程可能需要几十秒，成功后会自动保存。</p>
    <el-form label-position="top">
      <el-form-item label="周报标题（可选）"><el-input v-model="form.title" maxlength="160" placeholder="留空将自动使用项目名与日期" /></el-form-item>
      <div class="date-grid"><el-form-item label="开始日期"><el-date-picker v-model="form.periodStart" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="form.periodEnd" type="date" value-format="YYYY-MM-DD" /></el-form-item></div>
    </el-form>
    <template #footer><el-button @click="generateDialog=false">取消</el-button><el-button type="primary" :loading="generating" @click="generate">{{ generating ? "正在读取项目并生成" : "生成并保存" }}</el-button></template>
  </el-dialog>
</template>

<style scoped>
.page-heading{margin-bottom:24px;display:flex;align-items:flex-end;justify-content:space-between}.page-heading p{margin:0 0 7px;color:#0071e3;font-size:10px;font-weight:700;letter-spacing:.14em}.page-heading h2{margin:0;font-size:34px;letter-spacing:-.045em}.page-heading span{display:block;margin-top:9px;color:#6e6e73;font-size:14px}.report-hero{min-height:190px;padding:38px 42px;display:flex;align-items:center;justify-content:space-between;overflow:hidden;border-radius:28px;color:white;background:radial-gradient(circle at 80% 5%,rgba(72,180,255,.4),transparent 33%),linear-gradient(130deg,#111217,#153e70 68%,#0877d1);box-shadow:0 24px 60px rgba(14,42,80,.14)}.report-hero>div:first-child{max-width:720px}.report-hero span{color:rgba(255,255,255,.58);font-size:10px;font-weight:700;letter-spacing:.13em}.report-hero h3{margin:12px 0 10px;font-size:31px;letter-spacing:-.04em}.report-hero p{margin:0;color:rgba(255,255,255,.7);font-size:14px;line-height:1.7}.hero-stat{min-width:150px;text-align:center}.hero-stat strong,.hero-stat span{display:block}.hero-stat strong{font-size:48px;letter-spacing:-.06em}.hero-stat span{margin-top:6px;letter-spacing:0}.report-layout{margin-top:22px;display:grid;grid-template-columns:310px minmax(0,1fr);gap:20px;align-items:start}.report-list,.report-document,.document-empty{border:1px solid rgba(29,29,31,.08);border-radius:22px;background:rgba(255,255,255,.86);box-shadow:0 12px 35px rgba(0,0,0,.035)}.report-list{padding:12px;overflow:hidden}.report-list>header{height:58px;padding:0 10px;display:flex;align-items:center;justify-content:space-between}.report-list>header div{display:flex;align-items:center;gap:9px}.report-list>header span{font-size:14px;font-weight:700}.report-list>header strong{color:#86868b;font-size:11px}.report-list>header button{width:31px;height:31px;border:0;border-radius:50%;color:#0071e3;background:#edf6ff}.report-item{width:100%;padding:17px 15px;display:block;border:0;border-radius:14px;color:#1d1d1f;background:transparent;text-align:left;transition:.18s}.report-item:hover{background:#f5f5f7}.report-item.active{background:#eaf4ff}.report-item span,.report-item strong,.report-item small{display:block}.report-item span{color:#86868b;font-size:10px}.report-item strong{margin:7px 0 6px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px}.report-item small{color:#9a9a9f;font-size:10px}.list-empty{padding:60px 15px;color:#9a9a9f;text-align:center;font-size:12px;line-height:1.8}.report-document{min-height:620px;overflow:hidden}.document-head{padding:28px 32px;display:flex;justify-content:space-between;gap:20px;border-bottom:1px solid rgba(29,29,31,.07)}.document-head span{color:#0071e3;font-size:11px;font-weight:650}.document-head h3{margin:8px 0;font-size:24px;letter-spacing:-.035em}.document-head small{color:#86868b;font-size:11px}.document-actions{display:flex;align-items:flex-start}.report-content{padding:12px 48px 36px}.report-content section{padding:25px 0;border-bottom:1px solid rgba(29,29,31,.07)}.report-content section:last-child{border:0}.report-content h4{margin:0 0 13px;font-size:17px}.report-content p{margin:0;color:#444446;font-size:14px;line-height:1.9;white-space:pre-wrap}.report-sources{margin:0 32px 32px;padding:22px;border-radius:16px;background:#f5f5f7}.report-sources h4{margin:0 0 12px;font-size:13px}.report-sources span{display:block;margin-top:7px;color:#6e6e73;font-size:11px}.editor{padding:28px 32px}.editor .el-textarea{margin-top:15px}.editor-actions{padding:0 32px 28px;text-align:right}.document-empty{min-height:620px;display:grid;place-content:center;justify-items:center;color:#86868b;text-align:center}.document-empty .el-icon{font-size:40px;color:#2997ff}.document-empty h3{margin:18px 0 5px;color:#1d1d1f}.document-empty p{margin:0 0 20px;font-size:12px}.dialog-copy{margin:-5px 0 22px;color:#6e6e73;font-size:12px}.date-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.date-grid .el-date-editor{width:100%}@media(max-width:900px){.report-layout{grid-template-columns:1fr}.report-list{display:flex;overflow-x:auto}.report-list>header{min-width:120px}.report-item{min-width:230px}.report-hero{padding:30px}.hero-stat{display:none}}@media(max-width:640px){.page-heading{align-items:flex-start;gap:18px;flex-direction:column}.report-hero{min-height:220px}.report-content{padding:8px 25px 28px}.document-head{padding:24px;flex-direction:column}.date-grid{grid-template-columns:1fr}}
</style>
