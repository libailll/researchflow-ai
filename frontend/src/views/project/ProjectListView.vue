<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import EmptyState from "@/components/common/EmptyState.vue";
import ProjectDialog from "@/components/project/ProjectDialog.vue";
import { projectApi } from "@/api/project";
import { useWorkspaceStore } from "@/stores/workspace";
import type { Project } from "@/types/model";
import { PROJECT_STATUS } from "@/utils/constants";
import { formatDate } from "@/utils/format";

const workspace = useWorkspaceStore(); const router = useRouter(); const dialog = ref(false); const editing = ref<Project | null>(null);
function create() { editing.value = null; dialog.value = true; } function edit(project: Project) { editing.value = project; dialog.value = true; }
async function open(id: number) { await workspace.selectProject(id); await router.push("/"); }
async function remove(project: Project) { try { await ElMessageBox.confirm(`删除项目“${project.name}”后无法恢复，是否继续？`, "删除项目", { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" }); await projectApi.remove(project.id); await workspace.initialize(); ElMessage.success("项目已删除"); } catch (error) { if (error !== "cancel" && error !== "close") ElMessage.error((error as Error).message); } }
</script>

<template>
  <EmptyState v-if="!workspace.projects.length" title="还没有研究项目" description="创建一个项目，把研究目标、周期和协作成员集中起来。" action="创建项目" @action="create" />
  <section v-else><header class="page-heading"><div><p>PROJECT PORTFOLIO</p><h2>全部研究项目</h2><span>一处查看所有项目的状态与推进程度。</span></div><el-button type="primary" :icon="Plus" @click="create">新建项目</el-button></header><div class="project-grid"><article v-for="(project,index) in workspace.projects" :key="project.id" class="project-card"><i class="card-accent" :class="`tone-${index%4}`" /><header><span class="status-badge" :class="project.status.toLowerCase()">{{ PROJECT_STATUS[project.status] }}</span><el-dropdown trigger="click"><button class="more-button">•••</button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="edit(project)">编辑项目</el-dropdown-item><el-dropdown-item divided class="danger" @click="remove(project)">删除项目</el-dropdown-item></el-dropdown-menu></template></el-dropdown></header><button class="project-title" @click="open(project.id)">{{ project.name }}</button><p>{{ project.description || "暂无项目描述" }}</p><div class="project-progress"><div><span>项目进度</span><strong>{{ project.progress }}%</strong></div><el-progress :percentage="project.progress" :show-text="false" /></div><footer><span>{{ formatDate(project.startDate) }} — {{ formatDate(project.endDate) }}</span><button @click="open(project.id)">打开项目 →</button></footer></article></div></section>
  <ProjectDialog v-model="dialog" :project="editing" />
</template>

<style scoped>
.page-heading { margin-bottom:25px;display:flex;align-items:flex-end;justify-content:space-between; }.page-heading p { margin:0 0 5px;color:#8996a9;font-size:8px;font-weight:800;letter-spacing:.18em; }.page-heading h2 { margin:0;font-size:27px;letter-spacing:-.04em; }.page-heading span { display:block;margin-top:8px;color:#7b8799;font-size:11px; }.project-grid { display:grid;grid-template-columns:repeat(3,minmax(250px,1fr));gap:18px; }.project-card { position:relative;min-height:276px;padding:24px;overflow:hidden;border:1px solid var(--rf-line);border-radius:16px;background:white;box-shadow:0 9px 28px rgba(28,47,78,.035);transition:.22s; }.project-card:hover { transform:translateY(-3px);box-shadow:0 18px 40px rgba(28,47,78,.09); }.card-accent { position:absolute;inset:0 0 auto;height:4px;background:#5f84e1; }.card-accent.tone-1{background:#62cbbb}.card-accent.tone-2{background:#dfa052}.card-accent.tone-3{background:#8d79d4}.project-card>header{display:flex;justify-content:space-between}.status-badge{padding:5px 9px;border-radius:999px;color:#536177;background:#edf1f5;font-size:8px;font-weight:800}.status-badge.running,.status-badge.completed{color:#147a6c;background:#e0f5f1}.status-badge.planning{color:#315caf;background:#e7edfc}.status-badge.paused{color:#936617;background:#fff2d9}.status-badge.cancelled{color:#a34c56;background:#fce9eb}.more-button{padding:0 4px;border:0;color:#8c97a7;background:transparent;letter-spacing:2px}.project-title{margin:24px 0 10px;padding:0;display:block;border:0;color:var(--rf-ink);background:transparent;text-align:left;font:700 18px/1.4 Georgia,"Noto Serif SC",serif}.project-card>p{height:39px;overflow:hidden;margin:0;color:#7d899b;font-size:9px;line-height:1.8}.project-progress{margin-top:25px}.project-progress>div{margin-bottom:9px;display:flex;justify-content:space-between;color:#768296;font-size:8px}.project-card footer{margin-top:25px;padding-top:16px;display:flex;justify-content:space-between;border-top:1px solid #edf0f4}.project-card footer span{color:#9aa4b3;font-size:8px}.project-card footer button{padding:0;border:0;color:var(--rf-blue);background:transparent;font-size:8px;font-weight:700}.danger{color:#d65f58!important}
@media(max-width:1100px){.project-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:700px){.project-grid{grid-template-columns:1fr}.page-heading{align-items:flex-start;gap:14px}.page-heading h2{font-size:22px}}
</style>

<style scoped>
.project-grid { gap:20px; }
.project-card { min-height:310px; padding:28px; }
.card-accent { height:0; }.project-card>header { align-items:center; }
.project-title { margin:28px 0 12px; }.project-progress { margin-top:28px; }.project-card footer { margin-top:27px; padding-top:19px; border-top-color:rgba(29,29,31,.07); }
.more-button { width:34px; height:34px; border-radius:50%; }.more-button:hover { background:#f5f5f7; }
</style>
