import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { projectApi } from "@/api/project";
import { taskApi } from "@/api/task";
import type { Dashboard, Member, Project, ResearchTask } from "@/types/model";

export const useWorkspaceStore = defineStore("workspace", () => {
  const projects = ref<Project[]>([]);
  const activeProjectId = ref<number | null>(null);
  const tasks = ref<ResearchTask[]>([]);
  const members = ref<Member[]>([]);
  const dashboard = ref<Dashboard | null>(null);
  const loading = ref(false);
  const activeProject = computed(() => projects.value.find((item) => item.id === activeProjectId.value) || null);

  async function loadProjects() {
    projects.value = await projectApi.list();
    if (!activeProjectId.value || !projects.value.some((item) => item.id === activeProjectId.value)) activeProjectId.value = projects.value[0]?.id ?? null;
  }

  async function loadActiveProject() {
    if (!activeProjectId.value) { tasks.value = []; members.value = []; dashboard.value = null; return; }
    loading.value = true;
    try {
      [tasks.value, members.value, dashboard.value] = await Promise.all([
        taskApi.list(activeProjectId.value), projectApi.members(activeProjectId.value), taskApi.dashboard(activeProjectId.value),
      ]);
    } finally { loading.value = false; }
  }

  async function initialize() { await loadProjects(); await loadActiveProject(); }
  async function selectProject(id: number) { activeProjectId.value = id; await loadActiveProject(); }
  function reset() { projects.value = []; activeProjectId.value = null; tasks.value = []; members.value = []; dashboard.value = null; }

  return { projects, activeProjectId, activeProject, tasks, members, dashboard, loading, loadProjects, loadActiveProject, initialize, selectProject, reset };
});

