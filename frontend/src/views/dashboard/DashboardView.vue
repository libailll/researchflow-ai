<script setup lang="ts">
import { computed, ref } from "vue";
import { Check, Collection, List, Warning } from "@element-plus/icons-vue";
import EmptyState from "@/components/common/EmptyState.vue";
import StatCard from "@/components/common/StatCard.vue";
import ProjectDialog from "@/components/project/ProjectDialog.vue";
import TaskDialog from "@/components/task/TaskDialog.vue";
import { useWorkspaceStore } from "@/stores/workspace";
import { PROJECT_STATUS, TASK_STATUS } from "@/utils/constants";
import { formatDate, initials, isOverdue } from "@/utils/format";

const workspace = useWorkspaceStore(); const projectDialog = ref(false); const taskDialog = ref(false);
const recentTasks = computed(() => [...workspace.tasks].sort((a,b) => b.updatedAt.localeCompare(a.updatedAt)).slice(0,5));
const projectProgress = computed(() => workspace.dashboard?.progress ?? workspace.activeProject?.progress ?? 0);
</script>

<template>
  <EmptyState v-if="!workspace.activeProject" title="建立你的第一个研究项目" description="从项目开始组织目标、成员和每一项研究任务。" action="创建项目" @action="projectDialog = true" />
  <template v-else>
    <section class="project-hero">
      <div class="hero-copy"><span class="project-status">{{ PROJECT_STATUS[workspace.activeProject.status] }}</span><p class="section-kicker mint">CURRENT RESEARCH</p><h2>{{ workspace.activeProject.name }}</h2><p class="hero-description">{{ workspace.activeProject.description || "这个项目还没有添加说明。" }}</p><div class="hero-meta"><span>研究周期 {{ formatDate(workspace.activeProject.startDate) }} — {{ formatDate(workspace.activeProject.endDate) }}</span><span>{{ workspace.members.length }} 位协作成员</span></div></div>
      <div class="progress-ring" :style="{ '--progress': `${projectProgress * 3.6}deg` }"><div><strong>{{ projectProgress }}%</strong><span>项目进度</span></div></div>
    </section>
    <section class="stat-grid"><StatCard label="参与项目" :value="workspace.projects.length" note="当前工作空间" tone="blue" :icon="Collection" /><StatCard label="任务总数" :value="workspace.dashboard?.totalTasks || 0" :note="`${workspace.dashboard?.inProgressTasks || 0} 项正在推进`" tone="navy" :icon="List" /><StatCard label="已完成" :value="workspace.dashboard?.completedTasks || 0" :note="`${workspace.dashboard?.progress || 0}% 完成率`" tone="mint" :icon="Check" /><StatCard label="已逾期" :value="workspace.dashboard?.overdueTasks || 0" note="需要优先关注" tone="orange" :icon="Warning" /></section>
    <section class="dashboard-grid">
      <article class="surface tasks-panel"><header class="panel-head"><div><p class="section-kicker">RECENT TASKS</p><h3>近期任务</h3></div><router-link to="/tasks">查看看板 →</router-link></header><div v-if="recentTasks.length" class="task-list"><div v-for="task in recentTasks" :key="task.id" class="task-row"><span class="priority-dot" :class="task.priority.toLowerCase()" /><span class="task-title"><strong>{{ task.title }}</strong><small>{{ workspace.members.find(m => m.userId === task.assigneeId)?.nickname || "未指派" }}</small></span><span class="task-status" :class="task.status.toLowerCase()">{{ TASK_STATUS[task.status] }}</span><time :class="{ overdue: isOverdue(task) }">{{ formatDate(task.dueDate) }}</time></div></div><div v-else class="panel-empty"><p>还没有任务，先创建一个明确的下一步。</p><el-button text type="primary" @click="taskDialog = true">新建任务 →</el-button></div></article>
      <article class="surface team-panel"><header class="panel-head"><div><p class="section-kicker">TEAM</p><h3>协作成员</h3></div><span>{{ workspace.members.length }} 人</span></header><div class="avatar-cloud"><span v-for="(member,index) in workspace.members.slice(0,7)" :key="member.id" class="member-avatar" :class="`tone-${index%5}`">{{ initials(member.nickname) }}</span></div><div class="team-progress"><div><span>总体任务完成度</span><strong>{{ workspace.dashboard?.progress || 0 }}%</strong></div><el-progress :percentage="workspace.dashboard?.progress || 0" :show-text="false" /></div><el-button class="add-task" @click="taskDialog = true">＋ 添加下一项任务</el-button></article>
    </section>
  </template>
  <ProjectDialog v-model="projectDialog" /><TaskDialog v-model="taskDialog" />
</template>

<style scoped>
.project-hero { position: relative; min-height: 246px; padding: 36px 42px; display: flex; align-items: center; justify-content: space-between; overflow: hidden; border-radius: 20px; color: white; background: radial-gradient(circle at 75% 10%, rgba(71,120,204,.25),transparent 26%),linear-gradient(115deg,#10264d,#173e77); box-shadow: 0 18px 44px rgba(22,49,91,.16); }.project-hero::after { content:""; position:absolute; width:410px;height:410px;right:-140px;bottom:-220px;border:1px solid rgba(105,218,201,.2);border-radius:50%;box-shadow:0 0 0 58px rgba(105,218,201,.025),0 0 0 116px rgba(105,218,201,.017); }.hero-copy { position:relative;z-index:1;max-width:660px; }.project-status { padding:5px 10px;border-radius:999px;color:#87e1d2;background:rgba(101,212,194,.12);font-size:8px;font-weight:800; }.section-kicker { margin:0 0 5px;color:#8d98a9;font-size:8px;font-weight:800;letter-spacing:.18em; }.section-kicker.mint { margin-top:20px;color:#6bd5c4; }.hero-copy h2 { margin:0 0 11px;font:600 30px/1.2 Georgia,"Noto Serif SC",serif;letter-spacing:-.03em; }.hero-description { max-width:620px;margin:0;color:#b8c5d8;font-size:12px;line-height:1.8; }.hero-meta { margin-top:23px;display:flex;gap:28px;color:#91a5c2;font-size:9px; }.progress-ring { --progress:0deg;position:relative;z-index:1;width:138px;height:138px;flex:0 0 auto;display:grid;place-items:center;border-radius:50%;background:conic-gradient(var(--rf-mint) var(--progress),rgba(255,255,255,.11) 0); }.progress-ring::after { content:"";position:absolute;inset:8px;border-radius:50%;background:#183a6b; }.progress-ring>div { position:relative;z-index:1;text-align:center; }.progress-ring strong,.progress-ring span { display:block; }.progress-ring strong { font:700 28px/1 Georgia,serif; }.progress-ring span { margin-top:7px;color:#91a7c5;font-size:8px; }.stat-grid { margin:21px 0;display:grid;grid-template-columns:repeat(4,1fr);gap:15px; }.dashboard-grid { display:grid;grid-template-columns:minmax(0,1.55fr) minmax(260px,.65fr);gap:19px; }.surface { border:1px solid var(--rf-line);border-radius:16px;background:white; }.panel-head { min-height:78px;padding:20px 23px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #edf0f4; }.panel-head h3 { margin:0;font-size:15px; }.panel-head a { color:var(--rf-blue);font-size:9px;font-weight:700;text-decoration:none; }.panel-head>span { color:#8c97a8;font-size:10px; }.task-list { padding:4px 21px 10px; }.task-row { min-height:64px;display:grid;grid-template-columns:8px minmax(0,1fr) 70px 72px;align-items:center;gap:12px;border-bottom:1px solid #eff2f6; }.task-row:last-child { border:0; }.priority-dot { width:7px;height:7px;border-radius:50%;background:#5dc7b5; }.priority-dot.medium { background:#e4b04b; }.priority-dot.high,.priority-dot.urgent { background:#ef7863; }.task-title strong,.task-title small { display:block; }.task-title strong { font-size:10px; }.task-title small { margin-top:4px;color:#9ba5b4;font-size:8px; }.task-status { justify-self:start;padding:5px 7px;border-radius:5px;color:#56647a;background:#eef1f5;font-size:7px;font-weight:800; }.task-status.in_progress,.task-status.review { color:#315eb8;background:#e8eeff; }.task-status.done { color:#168273;background:#dff5f0; }.task-row time { color:#929dad;font-size:8px;text-align:right; }.overdue { color:#d95f4c!important; }.panel-empty { min-height:190px;display:grid;place-content:center;justify-items:center;color:#8e99aa;font-size:11px; }.avatar-cloud { min-height:100px;padding:29px 24px;display:flex;align-items:center; }.member-avatar { width:40px;height:40px;margin-right:-8px;display:grid;place-items:center;border:3px solid white;border-radius:50%;color:#174052;background:#7ad7c7;font-size:8px;font-weight:800; }.tone-1 { color:#563e84;background:#ded4f0; }.tone-2 { color:#8c502c;background:#f2d8c8; }.tone-3 { color:#285987;background:#d2e4f4; }.tone-4 { color:#70502d;background:#ece0c9; }.team-progress { padding:0 24px 22px; }.team-progress>div { margin-bottom:9px;display:flex;justify-content:space-between;color:#758195;font-size:8px; }.add-task { width:calc(100% - 48px);margin:0 24px 22px; }
@media(max-width:1100px){.stat-grid{grid-template-columns:repeat(2,1fr)}} @media(max-width:760px){.project-hero{min-height:310px;padding:27px 23px;align-items:flex-start;flex-direction:column}.hero-copy h2{font-size:24px}.hero-meta{flex-direction:column;gap:7px}.progress-ring{width:92px;height:92px;align-self:flex-end;margin-top:-14px}.progress-ring strong{font-size:20px}.stat-grid{gap:9px}.dashboard-grid{grid-template-columns:1fr}.task-row{grid-template-columns:8px 1fr 65px}.task-row time{display:none}}
</style>

<style scoped>
.project-hero { min-height: 330px; padding: clamp(34px,4vw,58px); border-radius: 30px; background: radial-gradient(circle at 76% 5%,rgba(80,170,255,.34),transparent 34%),linear-gradient(135deg,#111217,#172b50 65%,#0b64b7); box-shadow: 0 24px 60px rgba(13,33,64,.16); }
.project-hero::before { content:""; position:absolute; inset:0; pointer-events:none; background:linear-gradient(110deg,rgba(255,255,255,.05),transparent 45%); }
.project-hero::after { width:520px; height:520px; right:-170px; bottom:-310px; border-color:rgba(255,255,255,.16); box-shadow:0 0 0 74px rgba(255,255,255,.025),0 0 0 148px rgba(255,255,255,.018); }
.hero-copy { max-width: 760px; }
.project-status { padding:7px 12px; color:#d5f7ff; background:rgba(255,255,255,.12); font-size:11px; backdrop-filter:blur(10px); }
.section-kicker.mint { margin-top:28px !important; color:#64d8ff !important; }
.hero-copy h2 { margin-bottom:15px; font:700 clamp(34px,4vw,54px)/1.05 -apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif; letter-spacing:-.05em; }
.hero-description { max-width:680px; color:rgba(255,255,255,.72); font-size:15px; line-height:1.7; }
.hero-meta { margin-top:32px; gap:34px; color:rgba(255,255,255,.58); font-size:12px; }
.progress-ring { width:156px; height:156px; background:conic-gradient(#62d8ff var(--progress),rgba(255,255,255,.13) 0); }
.progress-ring::after { inset:9px; background:rgba(15,42,80,.9); backdrop-filter:blur(10px); }
.progress-ring strong { font:700 34px/1 -apple-system,BlinkMacSystemFont,sans-serif; letter-spacing:-.045em; }
.progress-ring span { margin-top:8px; color:rgba(255,255,255,.58); font-size:11px; }
.stat-grid { margin:22px 0; gap:16px; }
.dashboard-grid { gap:20px; }
.panel-head { min-height:92px; padding:24px 28px; border-bottom-color:rgba(29,29,31,.07); }
.panel-head h3 { font-size:20px; letter-spacing:-.025em; }
.panel-head a,.panel-head>span { font-size:12px; }
.task-list { padding:5px 27px 13px; }
.task-row { min-height:76px; grid-template-columns:8px minmax(0,1fr) 88px 84px; gap:15px; border-bottom-color:rgba(29,29,31,.07); }
.task-title strong { font-size:13px; }.task-title small { margin-top:4px; font-size:11px; }
.task-row time { font-size:11px; }
.avatar-cloud { min-height:118px; padding:32px 28px; }
.member-avatar { width:46px; height:46px; font-size:11px; }
.team-progress { padding:0 28px 26px; }.team-progress>div { font-size:11px; }
.add-task { width:calc(100% - 56px); margin:0 28px 25px; }
@media(max-width:760px){.project-hero{min-height:360px;padding:32px 25px}.hero-copy h2{font-size:34px}.hero-meta{gap:9px}.progress-ring{width:105px;height:105px}.progress-ring strong{font-size:24px}.task-row{grid-template-columns:8px 1fr 76px}}
</style>
