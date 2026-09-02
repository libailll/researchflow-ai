<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Bell, Check } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { notificationApi, type NotificationType, type UserNotification } from "@/api/notification";
import { useWorkspaceStore } from "@/stores/workspace";
import { formatDate } from "@/utils/format";

const router = useRouter(); const workspace = useWorkspaceStore();
const notifications = ref<UserNotification[]>([]); const unreadCount = ref(0);
const loading = ref(false); const markingAll = ref(false);
let timer: number | undefined;

const typeLabels: Record<NotificationType, string> = {
  TASK_ASSIGNED: "任务指派", TASK_UPDATED: "任务更新", TASK_STATUS_CHANGED: "状态变更",
  TASK_DUE_SOON: "即将到期", TASK_OVERDUE: "任务逾期", WEEKLY_REPORT_READY: "项目周报",
  DOCUMENT_SUMMARY_READY: "文档总结", RISK_REPORT_READY: "风险分析",
};

async function loadUnreadCount() {
  try { unreadCount.value = (await notificationApi.unreadCount()).count; }
  catch { /* Background refresh should not interrupt the current page. */ }
}
async function loadNotifications() {
  loading.value = true;
  try {
    notifications.value = await notificationApi.list();
    unreadCount.value = notifications.value.filter(item => !item.read).length;
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { loading.value = false; }
}
async function openNotification(item: UserNotification) {
  try {
    if (!item.read) {
      const updated = await notificationApi.markRead(item.id); Object.assign(item, updated);
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
    if (item.projectId && workspace.activeProjectId !== item.projectId) await workspace.selectProject(item.projectId);
    if (item.targetPath) await router.push(item.targetPath);
  } catch (error) { ElMessage.error((error as Error).message); }
}
async function markAllRead() {
  if (!unreadCount.value) return;
  markingAll.value = true;
  try {
    await notificationApi.markAllRead(); const now = new Date().toISOString();
    notifications.value.forEach(item => { item.read = true; item.readAt = item.readAt || now; });
    unreadCount.value = 0; ElMessage.success("全部通知已标记为已读");
  } catch (error) { ElMessage.error((error as Error).message); }
  finally { markingAll.value = false; }
}

onMounted(() => { loadUnreadCount(); timer = window.setInterval(loadUnreadCount, 30000); });
onUnmounted(() => { if (timer) window.clearInterval(timer); });
</script>

<template>
  <el-popover placement="bottom-end" :width="390" trigger="click" popper-class="notification-popover" @show="loadNotifications">
    <template #reference>
      <el-badge :value="unreadCount > 99 ? '99+' : unreadCount" :hidden="unreadCount === 0" class="notification-badge">
        <el-button circle :icon="Bell" class="bell-button" title="通知中心" />
      </el-badge>
    </template>
    <section class="notification-panel">
      <header><div><h3>通知</h3><span>{{ unreadCount ? `${unreadCount} 条未读` : "已全部读完" }}</span></div><el-button text type="primary" :icon="Check" :loading="markingAll" :disabled="!unreadCount" @click="markAllRead">全部已读</el-button></header>
      <div v-loading="loading" class="notification-list">
        <button v-for="item in notifications" :key="item.id" class="notification-item" :class="[{ unread: !item.read }, item.type.toLowerCase()]" @click="openNotification(item)">
          <i /><span><small>{{ typeLabels[item.type] }} · {{ formatDate(item.createdAt) }}</small><strong>{{ item.title }}</strong><p>{{ item.content }}</p></span>
        </button>
        <div v-if="!notifications.length && !loading" class="notification-empty"><el-icon><Bell /></el-icon><strong>还没有通知</strong><span>任务变化和 AI 结果会出现在这里。</span></div>
      </div>
    </section>
  </el-popover>
</template>

<style scoped>
.notification-badge{display:flex}.bell-button{border-color:rgba(29,29,31,.1);background:rgba(255,255,255,.72)}.notification-panel{margin:-12px;overflow:hidden;border-radius:12px}.notification-panel>header{height:70px;padding:0 18px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid rgba(29,29,31,.08)}.notification-panel>header h3{margin:0;font-size:17px;letter-spacing:-.02em}.notification-panel>header span{display:block;margin-top:3px;color:#86868b;font-size:10px}.notification-panel>header .el-button{min-height:32px;padding:5px 8px}.notification-list{max-height:470px;min-height:180px;overflow-y:auto}.notification-item{width:100%;padding:15px 18px;display:grid;grid-template-columns:9px 1fr;gap:11px;border:0;border-bottom:1px solid rgba(29,29,31,.06);color:#1d1d1f;background:#fff;text-align:left}.notification-item:hover{background:#f7f7f9}.notification-item.unread{background:#f1f7ff}.notification-item>i{width:8px;height:8px;margin-top:5px;border-radius:50%;background:#a1a1a6}.notification-item.unread>i{background:#0071e3}.notification-item.task_overdue>i{background:#d8374f}.notification-item.task_due_soon>i{background:#d5831f}.notification-item.risk_report_ready>i{background:#a74472}.notification-item span{min-width:0}.notification-item small,.notification-item strong{display:block}.notification-item small{color:#86868b;font-size:9px}.notification-item strong{margin-top:5px;font-size:12px}.notification-item p{margin:4px 0 0;display:-webkit-box;overflow:hidden;color:#6e6e73;font-size:10px;line-height:1.55;-webkit-box-orient:vertical;-webkit-line-clamp:2}.notification-empty{min-height:230px;display:grid;place-content:center;justify-items:center;color:#86868b;text-align:center}.notification-empty .el-icon{margin-bottom:12px;color:#a1a1a6;font-size:27px}.notification-empty strong{color:#515154;font-size:12px}.notification-empty span{margin-top:5px;font-size:10px}
</style>
