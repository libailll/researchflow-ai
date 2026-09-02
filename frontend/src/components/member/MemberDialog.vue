<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { projectApi } from "@/api/project";
import { useWorkspaceStore } from "@/stores/workspace";
import type { MemberRole } from "@/types/model";

defineProps<{ modelValue: boolean }>(); const emit = defineEmits<{ "update:modelValue": [value: boolean] }>();
const workspace = useWorkspaceStore(); const formRef = ref<FormInstance>(); const saving = ref(false); const form = reactive<{ userId?: number; role: MemberRole }>({ userId: undefined, role: "MEMBER" });
const rules: FormRules = { userId: [{ required: true, message: "请输入用户 ID", trigger: "blur" }] };
async function submit() { if (!workspace.activeProjectId || !form.userId) return; await formRef.value?.validate(); saving.value = true; try { await projectApi.addMember(workspace.activeProjectId, form.userId, form.role); await workspace.loadActiveProject(); ElMessage.success("成员添加成功"); emit("update:modelValue", false); } catch (error) { ElMessage.error((error as Error).message); } finally { saving.value = false; } }
</script>

<template><el-dialog :model-value="modelValue" title="添加项目成员" width="480px" @update:model-value="emit('update:modelValue', $event)"><p class="dialog-intro">通过用户编号邀请已注册的 ResearchFlow 用户。</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="用户编号" prop="userId"><el-input-number v-model="form.userId" :min="1" controls-position="right" /></el-form-item><el-form-item label="项目角色"><el-select v-model="form.role"><el-option label="成员 · 可查看和执行任务" value="MEMBER" /><el-option label="管理员 · 可管理项目与任务" value="ADMIN" /></el-select></el-form-item></el-form><template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="saving" @click="submit">添加成员</el-button></template></el-dialog></template>
<style scoped>.dialog-intro { margin: -8px 0 24px; color: var(--rf-muted); font-size: 12px; } :deep(.el-input-number), :deep(.el-select) { width: 100%; }</style>
