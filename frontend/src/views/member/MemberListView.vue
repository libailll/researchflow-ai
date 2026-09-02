<script setup lang="ts">
import { ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import EmptyState from "@/components/common/EmptyState.vue";
import MemberDialog from "@/components/member/MemberDialog.vue";
import { projectApi } from "@/api/project";
import { useWorkspaceStore } from "@/stores/workspace";
import type { Member } from "@/types/model";
import { MEMBER_ROLE } from "@/utils/constants";
import { formatDate, initials } from "@/utils/format";

const workspace=useWorkspaceStore(); const dialog=ref(false);
async function remove(member:Member){if(!workspace.activeProjectId)return;try{await ElMessageBox.confirm(`移除成员“${member.nickname}”？`,`移除成员`,{type:"warning"});await projectApi.removeMember(workspace.activeProjectId,member.userId);await workspace.loadActiveProject();ElMessage.success("成员已移除");}catch(error){if(error!=="cancel"&&error!=="close")ElMessage.error((error as Error).message);}}
</script>

<template>
  <EmptyState v-if="!workspace.activeProject" title="先创建或选择一个项目" description="成员管理以项目为单位。" />
  <section v-else><header class="page-heading"><div><p>PROJECT TEAM</p><h2>{{ workspace.activeProject.name }} · 成员</h2><span>管理项目内的协作角色与访问权限。</span></div><el-button type="primary" :icon="Plus" @click="dialog=true">添加成员</el-button></header><div class="member-surface"><div class="member-row table-head"><span>成员</span><span>账号</span><span>角色</span><span>加入时间</span><span /></div><div v-for="(member,index) in workspace.members" :key="member.id" class="member-row"><span class="member-name"><i :class="`tone-${index%5}`">{{ initials(member.nickname) }}</i><strong>{{ member.nickname }}</strong></span><span>@{{ member.username }}</span><span><em :class="member.role.toLowerCase()">{{ MEMBER_ROLE[member.role] }}</em></span><span>{{ formatDate(member.joinedAt) }}</span><span><el-button v-if="member.role!=='OWNER'" text type="danger" @click="remove(member)">移除</el-button></span></div></div></section>
  <MemberDialog v-model="dialog" />
</template>

<style scoped>
.page-heading{margin-bottom:25px;display:flex;align-items:flex-end;justify-content:space-between}.page-heading p{margin:0 0 5px;color:#8996a9;font-size:8px;font-weight:800;letter-spacing:.18em}.page-heading h2{margin:0;font-size:27px;letter-spacing:-.04em}.page-heading span{display:block;margin-top:8px;color:#7b8799;font-size:11px}.member-surface{overflow:hidden;border:1px solid var(--rf-line);border-radius:16px;background:white}.member-row{min-height:69px;padding:0 24px;display:grid;grid-template-columns:1.4fr 1fr .8fr 1fr 55px;align-items:center;gap:15px;border-bottom:1px solid #edf0f4;color:#6d7a8d;font-size:9px}.member-row:last-child{border:0}.table-head{min-height:45px;color:#98a2b1;background:#fafbfc;font-size:8px;font-weight:800}.member-name{display:flex;align-items:center;gap:12px}.member-name i{width:35px;height:35px;display:grid;place-items:center;border-radius:50%;color:#184458;background:#78d5c5;font-size:8px;font-style:normal;font-weight:800}.member-name i.tone-1{color:#583f83;background:#ded4ef}.member-name i.tone-2{color:#87502d;background:#f2d9ca}.member-name i.tone-3{color:#285787;background:#d3e4f3}.member-name i.tone-4{color:#70502d;background:#ece0c8}.member-name strong{color:#27364e}.member-row em{padding:5px 8px;border-radius:5px;color:#46566e;background:#edf1f6;font-size:8px;font-style:normal;font-weight:800}.member-row em.owner{color:#167368;background:#e0f4ef}.member-row em.admin{color:#325dab;background:#e7edfb}
@media(max-width:760px){.page-heading{align-items:flex-start;gap:14px}.page-heading h2{font-size:22px}.member-surface{overflow-x:auto}.member-row{min-width:680px}}
</style>

