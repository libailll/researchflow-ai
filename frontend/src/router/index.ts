import { createRouter, createWebHistory } from "vue-router";
import { TOKEN_KEY } from "@/api/http";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: () => import("@/views/auth/LoginView.vue"), meta: { public: true, title: "登录" } },
    { path: "/register", name: "register", component: () => import("@/views/auth/RegisterView.vue"), meta: { public: true, title: "注册" } },
    {
      path: "/", component: () => import("@/layouts/AppLayout.vue"),
      children: [
        { path: "", name: "dashboard", component: () => import("@/views/dashboard/DashboardView.vue"), meta: { title: "工作台" } },
        { path: "projects", name: "projects", component: () => import("@/views/project/ProjectListView.vue"), meta: { title: "项目" } },
        { path: "tasks", name: "tasks", component: () => import("@/views/task/TaskBoardView.vue"), meta: { title: "任务看板" } },
        { path: "documents", name: "documents", component: () => import("@/views/document/DocumentListView.vue"), meta: { title: "项目文档" } },
        { path: "assistant", name: "assistant", component: () => import("@/views/ai/AiAssistantView.vue"), meta: { title: "AI 助手" } },
        { path: "weekly-reports", name: "weekly-reports", component: () => import("@/views/report/WeeklyReportView.vue"), meta: { title: "项目周报" } },
        { path: "risk-analysis", name: "risk-analysis", component: () => import("@/views/risk/ProjectRiskView.vue"), meta: { title: "风险分析" } },
        { path: "members", name: "members", component: () => import("@/views/member/MemberListView.vue"), meta: { title: "项目成员" } },
        { path: "profile", name: "profile", component: () => import("@/views/profile/ProfileView.vue"), meta: { title: "个人资料" } },
      ],
    },
    { path: "/:pathMatch(.*)*", redirect: "/" },
  ],
});

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!to.meta.public && !token) return { name: "login", query: { redirect: to.fullPath } };
  if (to.meta.public && token) return { name: "dashboard" };
  document.title = `${String(to.meta.title || "ResearchFlow AI")} · ResearchFlow AI`;
});

export default router;
