<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { taskApi } from "@/api/task";
import { useWorkspaceStore } from "@/stores/workspace";
import type { TaskForm, TaskPriority } from "@/types/model";
import { TASK_PRIORITY } from "@/utils/constants";

const props = defineProps<{ modelValue: boolean }>(); const emit = defineEmits<{ "update:modelValue": [value: boolean] }>();
const workspace = useWorkspaceStore(); const formRef = ref<FormInstance>(); const saving = ref(false);
const form = reactive<TaskForm>({ title: "", description: "", assigneeId: undefined, priority: "MEDIUM", startDate: "", dueDate: "" });
const rules: FormRules = { title: [{ required: true, message: "请输入任务标题", trigger: "blur" }], priority: [{ required: true, message: "请选择优先级", trigger: "change" }] };
watch(() => props.modelValue, (visible) => { if (visible) Object.assign(form, { title: "", description: "", assigneeId: undefined, priority: "MEDIUM", startDate: "", dueDate: "" }); });
async function submit() { if (!workspace.activeProjectId) return; await formRef.value?.validate(); saving.value = true; try { await taskApi.create(workspace.activeProjectId, { ...form, startDate: form.startDate || undefined, dueDate: form.dueDate || undefined }); await workspace.loadActiveProject(); ElMessage.success("任务创建成功"); emit("update:modelValue", false); } catch (error) { ElMessage.error((error as Error).message); } finally { saving.value = false; } }
</script>

<template>
  <el-dialog :model-value="modelValue" title="新建研究任务" width="620px" destroy-on-close @update:model-value="emit('update:modelValue', $event)"><p class="dialog-intro">把下一步工作定义得具体、可执行、可追踪。</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="two-column-form"><el-form-item label="任务标题" prop="title" class="wide"><el-input v-model="form.title" maxlength="200" placeholder="例如：完成基线模型实验" /></el-form-item><el-form-item label="负责人"><el-select v-model="form.assigneeId" clearable placeholder="暂不指派"><el-option v-for="member in workspace.members" :key="member.id" :label="member.nickname" :value="member.userId" /></el-select></el-form-item><el-form-item label="优先级" prop="priority"><el-select v-model="form.priority"><el-option v-for="(label, value) in TASK_PRIORITY" :key="value" :label="`${label}优先级`" :value="value as TaskPriority" /></el-select></el-form-item><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" /></el-form-item><el-form-item label="截止日期"><el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" /></el-form-item><el-form-item label="任务说明" class="wide"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="5000" placeholder="补充验收标准、资料位置或注意事项" /></el-form-item></el-form><template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="saving" @click="submit">创建任务</el-button></template></el-dialog>
</template>

<style scoped>
.dialog-intro { margin: -8px 0 24px; color: var(--rf-muted); font-size: 12px; }.two-column-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }.wide { grid-column: 1 / -1; } :deep(.el-date-editor), :deep(.el-select) { width: 100%; }
@media (max-width: 640px) { .two-column-form { display: block; } }
</style>

