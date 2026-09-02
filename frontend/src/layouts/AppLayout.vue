<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Calendar, ChatDotRound, Collection, DataBoard, Document, Fold, Menu, Plus, Tickets, User, UserFilled, Warning } from "@element-plus/icons-vue";
import AppLogo from "@/components/common/AppLogo.vue";
import ProjectDialog from "@/components/project/ProjectDialog.vue";
import NotificationCenter from "@/components/notification/NotificationCenter.vue";
import { useAuthStore } from "@/stores/auth";
import { useWorkspaceStore } from "@/stores/workspace";
import { initials } from "@/utils/format";

const route = useRoute(); const router = useRouter(); const auth = useAuthStore(); const workspace = useWorkspaceStore();
const collapsed = ref(false); const projectDialog = ref(false); const initialized = ref(false);
const title = computed(() => String(route.meta.title || "工作台"));
const navItems = [
  { path: "/", name: "工作台", icon: DataBoard }, { path: "/projects", name: "项目", icon: Collection },
  { path: "/tasks", name: "任务看板", icon: Calendar }, { path: "/members", name: "项目成员", icon: UserFilled },
  { path: "/documents", name: "项目文档", icon: Document },
  { path: "/assistant", name: "AI 助手", icon: ChatDotRound },
  { path: "/weekly-reports", name: "项目周报", icon: Tickets },
  { path: "/risk-analysis", name: "风险分析", icon: Warning },
];

onMounted(async () => { if (!auth.user) await auth.initialize(); try { await workspace.initialize(); } finally { initialized.value = true; } });
async function signOut() { await auth.logout(); workspace.reset(); await router.replace("/login"); }
</script>

<template>
  <div class="app-shell" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="sidebar-logo"><AppLogo :compact="collapsed" light /></div>
      <nav>
        <p v-if="!collapsed" class="nav-section">工作空间</p>
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item" :class="{ exact: item.path === '/' }"><el-icon><component :is="item.icon" /></el-icon><span v-if="!collapsed">{{ item.name }}</span></router-link>
        <p v-if="!collapsed" class="nav-section account">账户</p>
        <router-link to="/profile" class="nav-item"><el-icon><User /></el-icon><span v-if="!collapsed">个人资料</span></router-link>
      </nav>
      <div class="sidebar-user">
        <span class="avatar">{{ initials(auth.user?.nickname) }}</span>
        <span v-if="!collapsed" class="user-copy"><strong>{{ auth.user?.nickname || "研究者" }}</strong><small>@{{ auth.user?.username }}</small></span>
        <el-button v-if="!collapsed" text class="logout-button" @click="signOut">↗</el-button>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div class="title-area"><el-button text class="collapse-button" :icon="collapsed ? Menu : Fold" @click="collapsed = !collapsed" /><div><p>RESEARCHFLOW / {{ title }}</p><h1>{{ route.name === "dashboard" ? `你好，${auth.user?.nickname || "研究者"}` : title }}</h1></div></div>
        <div class="top-actions">
          <el-select v-if="workspace.projects.length && !['projects', 'profile'].includes(String(route.name))" :model-value="workspace.activeProjectId" class="project-select" @change="workspace.selectProject"><el-option v-for="project in workspace.projects" :key="project.id" :label="project.name" :value="project.id" /></el-select>
          <NotificationCenter />
          <el-button type="primary" class="new-project-button" :icon="Plus" @click="projectDialog = true">新建项目</el-button>
        </div>
      </header>
      <div v-loading="workspace.loading || !initialized" class="page-container"><router-view /></div>
    </main>
    <ProjectDialog v-model="projectDialog" />
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; display: grid; grid-template-columns: 248px minmax(0,1fr); background: var(--rf-bg); transition: grid-template-columns .25s ease; }.app-shell.collapsed { grid-template-columns: 80px minmax(0,1fr); }.sidebar { position: sticky; top: 0; height: 100vh; padding: 27px 17px 19px; display: flex; flex-direction: column; overflow: hidden; color: white; background: linear-gradient(180deg, #0b1d3c, #0d2348); }.sidebar-logo { height: 56px; padding: 0 9px 20px; border-bottom: 1px solid rgba(255,255,255,.08); }.sidebar nav { margin-top: 18px; }.nav-section { margin: 20px 13px 8px; color: #617493; font-size: 8px; font-weight: 800; letter-spacing: .18em; text-transform: uppercase; }.nav-section.account { margin-top: 28px; }.nav-item { height: 44px; margin-bottom: 3px; padding: 0 14px; display: flex; align-items: center; gap: 13px; border-radius: 10px; color: #8fa1bc; font-size: 12px; text-decoration: none; transition: .18s; }.nav-item:hover { color: white; background: rgba(255,255,255,.055); }.nav-item.router-link-active:not(.exact), .nav-item.exact.router-link-exact-active { color: white; background: linear-gradient(90deg, #1d427e, #19386d); box-shadow: inset 3px 0 #64d1bf; }.nav-item .el-icon { color: #6f84a5; font-size: 17px; }.nav-item.router-link-active .el-icon { color: #69d4c2; }.collapsed .nav-item { justify-content: center; padding: 0; }.sidebar-user { margin-top: auto; padding: 17px 6px 0; display: flex; align-items: center; gap: 10px; border-top: 1px solid rgba(255,255,255,.08); }.avatar { width: 35px; height: 35px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 50%; color: #123452; background: #70d3c2; font-size: 9px; font-weight: 800; }.user-copy { min-width: 0; flex: 1; }.user-copy strong,.user-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.user-copy strong { font-size: 11px; }.user-copy small { margin-top: 3px; color: #7185a3; font-size: 9px; }.logout-button { color: #6d82a2; font-size: 17px; }.main-area { min-width: 0; }.topbar { height: 104px; padding: 0 38px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e4e8ef; background: rgba(255,255,255,.88); backdrop-filter: blur(16px); }.title-area { display: flex; align-items: center; gap: 11px; }.title-area p { margin: 0 0 6px; color: #9aa4b3; font-size: 8px; font-weight: 800; letter-spacing: .15em; }.title-area h1 { margin: 0; font-size: 23px; letter-spacing: -.04em; }.collapse-button { color: #7c899b; }.top-actions { display: flex; align-items: center; gap: 10px; }.project-select { width: 190px; }.bell-button { position: relative; }.bell-button::after { content: ""; position: absolute; top: 7px; right: 8px; width: 5px; height: 5px; border: 2px solid white; border-radius: 50%; background: #f07963; }.page-container { min-height: calc(100vh - 104px); padding: 30px 38px 48px; }
@media (max-width: 760px) { .app-shell,.app-shell.collapsed { display: block; }.sidebar { position: fixed; z-index: 10; inset: auto 0 0; width: 100%; height: 66px; padding: 7px 9px; }.sidebar-logo,.sidebar-user,.nav-section { display: none; }.sidebar nav { margin: 0; display: flex; justify-content: space-around; }.nav-item,.collapsed .nav-item { width: 20%; height: 52px; padding: 3px; flex-direction: column; justify-content: center; gap: 2px; font-size: 8px; }.nav-item.router-link-active:not(.exact),.nav-item.exact.router-link-exact-active { box-shadow: inset 0 -2px #64d1bf; }.topbar { height: 88px; padding: 0 17px; }.collapse-button,.bell-button,.project-select { display: none; }.title-area h1 { font-size: 18px; }.top-actions .el-button { padding: 8px 11px; font-size: 10px; }.page-container { min-height: calc(100vh - 88px); padding: 19px 15px 88px; } }
@media (max-width: 760px) { .nav-item,.collapsed .nav-item { width: 11.11%; } }
</style>

<style scoped>
.new-project-button {
  min-width: 118px;
  height: 42px;
  border-color: #1d1d1f;
  color: #fff !important;
  background: #1d1d1f;
  box-shadow: none;
}
.new-project-button:hover,
.new-project-button:focus {
  border-color: #333336;
  color: #fff !important;
  background: #333336;
  box-shadow: 0 8px 20px rgba(0,0,0,.14);
}
.new-project-button :deep(.el-icon) { color: #fff; }
@media(max-width:760px){.new-project-button{min-width:auto;height:38px;padding:8px 14px}}
</style>

<style scoped>
.app-shell { grid-template-columns: 244px minmax(0,1fr); background: transparent; }
.app-shell.collapsed { grid-template-columns: 84px minmax(0,1fr); }
.sidebar { z-index: 8; padding: 22px 16px 18px; color: var(--rf-ink); background: rgba(255,255,255,.78); border-right: 1px solid rgba(29,29,31,.08); backdrop-filter: blur(28px) saturate(160%); }
.sidebar-logo { height: 58px; padding: 0 8px 19px; border-bottom-color: rgba(29,29,31,.07); }
.sidebar nav { margin-top: 12px; }
.nav-section { margin: 22px 13px 8px; color: #86868b; font-size: 10px; font-weight: 600; letter-spacing: .08em; text-transform: none; }
.nav-item { height: 45px; margin-bottom: 4px; padding: 0 13px; gap: 12px; border-radius: 13px; color: #515154; font-size: 13px; font-weight: 500; }
.nav-item:hover { color: var(--rf-ink); background: rgba(0,0,0,.04); }
.nav-item.router-link-active:not(.exact), .nav-item.exact.router-link-exact-active { color: #0066cc; background: rgba(0,113,227,.09); box-shadow: none; }
.nav-item .el-icon { color: #86868b; font-size: 18px; }
.nav-item.router-link-active .el-icon { color: #0071e3; }
.sidebar-user { padding: 17px 7px 0; border-top-color: rgba(29,29,31,.08); }
.avatar { width: 38px; height: 38px; color: white; background: linear-gradient(145deg,#2997ff,#0071e3); font-size: 11px; }
.user-copy strong { color: var(--rf-ink); font-size: 12px; }
.user-copy small { color: #86868b; font-size: 10px; }
.logout-button { color: #86868b; }
.topbar { position: sticky; top: 0; z-index: 7; height: 88px; padding: 0 clamp(24px,3vw,48px); border-bottom: 1px solid rgba(29,29,31,.07); background: rgba(250,250,252,.74); backdrop-filter: blur(28px) saturate(170%); }
.title-area { gap: 13px; }
.title-area p { margin-bottom: 4px; color: #86868b; font-size: 10px; font-weight: 600; letter-spacing: .08em; }
.title-area h1 { font-size: 24px; font-weight: 700; letter-spacing: -.035em; }
.collapse-button { color: #6e6e73; }
.top-actions { gap: 12px; }
.project-select { width: 210px; }
.bell-button { border-color: rgba(29,29,31,.1); background: rgba(255,255,255,.72); }
.page-container { min-height: calc(100vh - 88px); padding: 44px clamp(24px,3vw,48px) 64px; }
@media (min-width: 1600px) { .page-container { padding-left: max(48px, calc((100vw - 1540px) / 2)); padding-right: max(48px, calc((100vw - 1540px) / 2)); } }
@media (max-width: 760px) {
  .sidebar { background: rgba(250,250,252,.92); border-top: 1px solid rgba(29,29,31,.08); border-right: 0; backdrop-filter: blur(24px); }
  .nav-item,.collapsed .nav-item { font-size: 9px; color: #6e6e73; }
  .nav-item.router-link-active:not(.exact),.nav-item.exact.router-link-exact-active { box-shadow: none; }
  .topbar { height: 76px; padding: 0 18px; }
  .title-area p { display: none; }
  .title-area h1 { font-size: 21px; }
  .page-container { min-height: calc(100vh - 76px); padding: 28px 16px 92px; }
}
</style>
