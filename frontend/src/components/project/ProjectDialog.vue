<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { projectApi } from "@/api/project";
import { useWorkspaceStore } from "@/stores/workspace";
import type { Project, ProjectForm, ProjectStatus } from "@/types/model";
import { PROJECT_STATUS } from "@/utils/constants";

const props = defineProps<{ modelValue: boolean; project?: Project | null }>();
const emit = defineEmits<{ "update:modelValue": [value: boolean]; saved: [] }>();
const workspace = useWorkspaceStore(); const formRef = ref<FormInstance>(); const saving = ref(false);
const form = reactive<ProjectForm>({ name: "", description: "", status: "PLANNING", startDate: "", endDate: "" });
const isEdit = computed(() => Boolean(props.project));
const rules: FormRules = { name: [{ required: true, message: "请输入项目名称", trigger: "blur" }] };
watch(() => props.modelValue, (visible) => { if (!visible) return; Object.assign(form, props.project ? { name: props.project.name, description: props.project.description || "", status: props.project.status, startDate: props.project.startDate || "", endDate: props.project.endDate || "" } : { name: "", description: "", status: "PLANNING", startDate: "", endDate: "" }); });
async function submit() { await formRef.value?.validate(); saving.value = true; try { const data = { ...form, startDate: form.startDate || undefined, endDate: form.endDate || undefined }; if (props.project) await projectApi.update(props.project.id, data); else await projectApi.create(data); await workspace.initialize(); ElMessage.success(isEdit.value ? "项目已更新" : "项目创建成功"); emit("saved"); emit("update:modelValue", false); } catch (error) { ElMessage.error((error as Error).message); } finally { saving.value = false; } }
</script>

<template>
  <el-dialog :model-value="modelValue" :title="isEdit ? '编辑研究项目' : '创建研究项目'" width="620px" destroy-on-close @update:model-value="emit('update:modelValue', $event)">
    <p class="dialog-intro">定义研究目标、周期与当前推进状态。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="two-column-form">
      <el-form-item label="项目名称" prop="name" class="wide"><el-input v-model="form.name" maxlength="100" placeholder="例如：多模态文献理解研究" /></el-form-item>
      <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" /></el-form-item>
      <el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" /></el-form-item>
      <template v-if="isEdit"><el-form-item label="项目状态"><el-select v-model="form.status"><el-option v-for="(label, value) in PROJECT_STATUS" :key="value" :label="label" :value="value as ProjectStatus" /></el-select></el-form-item></template>
      <el-form-item label="项目说明" class="wide"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="简要描述研究背景、目标与预期成果" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="saving" @click="submit">{{ isEdit ? "保存修改" : "创建项目" }}</el-button></template>
  </el-dialog>
</template>

<style scoped>
.dialog-intro { margin: -8px 0 24px; color: var(--rf-muted); font-size: 12px; }.two-column-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }.wide { grid-column: 1 / -1; } :deep(.el-date-editor), :deep(.el-select) { width: 100%; }
@media (max-width: 640px) { .two-column-form { display: block; } }
</style>
