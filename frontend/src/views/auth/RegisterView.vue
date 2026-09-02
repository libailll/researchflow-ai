<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { Lock, User, UserFilled } from "@element-plus/icons-vue";
import AuthLayout from "@/layouts/AuthLayout.vue";
import AppLogo from "@/components/common/AppLogo.vue";
import { authApi } from "@/api/auth";

const router = useRouter(); const formRef = ref<FormInstance>(); const loading = ref(false);
const form = reactive({ nickname: "", username: "", password: "" });
const rules: FormRules = {
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }, { min: 3, max: 20, message: "用户名长度为 3–20 个字符", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }, { min: 6, max: 50, message: "密码至少 6 位", trigger: "blur" }],
};
async function submit() { await formRef.value?.validate(); loading.value = true; try { await authApi.register(form.username.trim(), form.password, form.nickname.trim()); ElMessage.success("注册成功，请登录"); await router.replace("/login"); } catch (error) { ElMessage.error((error as Error).message); } finally { loading.value = false; } }
</script>

<template>
  <AuthLayout><section class="auth-card"><AppLogo class="mobile-logo" /><h2>创建 ResearchFlow 账号</h2><p class="form-note">开始组织你的研究工作。</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit"><el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" :prefix-icon="UserFilled" placeholder="团队中显示的名称" size="large" /></el-form-item><el-form-item label="用户名" prop="username"><el-input v-model="form.username" :prefix-icon="User" placeholder="3–20 个字符" size="large" autocomplete="username" /></el-form-item><el-form-item label="密码" prop="password"><el-input v-model="form.password" :prefix-icon="Lock" placeholder="至少 6 位" type="password" show-password size="large" autocomplete="new-password" /></el-form-item><el-button class="submit-button" type="primary" size="large" :loading="loading" @click="submit">创建账号</el-button></el-form><p class="switch-auth">已有账号？<router-link to="/login">返回登录</router-link></p></section></AuthLayout>
</template>

<style scoped>
.auth-card { width: min(100%, 430px); padding: 40px 46px; border: 1px solid #e0e6ef; border-radius: 24px; background: rgba(255,255,255,.97); box-shadow: 0 26px 75px rgba(34,53,82,.11); }.mobile-logo { display: none; }.form-kicker { margin: 0 0 11px; color: var(--rf-blue); font-size: 10px; font-weight: 800; letter-spacing: .2em; }.auth-card h2 { margin: 0; font-size: 29px; letter-spacing: -.04em; }.form-note { margin: 11px 0 27px; color: var(--rf-muted); font-size: 12px; }.submit-button { width: 100%; height: 50px; margin-top: 5px; padding: 0 19px; display: flex; justify-content: space-between; border-radius: 11px; box-shadow: 0 12px 26px rgba(37,89,214,.2); }.submit-button span { flex: 1; text-align: left; }.submit-button b { font-size: 19px; font-weight: 400; }.switch-auth { margin: 22px 0 0; color: #7d899b; text-align: center; font-size: 12px; }.switch-auth a { margin-left: 5px; color: var(--rf-blue); font-weight: 700; } :deep(.el-form-item) { margin-bottom: 18px; } :deep(.el-form-item__label) { color: #37445a; font-size: 12px; font-weight: 700; } :deep(.el-input__wrapper) { border-radius: 10px; background: #fbfcfe; box-shadow: 0 0 0 1px #dfe5ee inset; }
@media (max-width: 900px) { .auth-card { padding: 28px 24px; border: 0; box-shadow: none; }.mobile-logo { display: flex; margin-bottom: 30px; } }
</style>

<style scoped>
.auth-card{width:min(100%,400px);padding:0;border:0;border-radius:0;background:transparent;box-shadow:none;backdrop-filter:none}
.auth-card h2{margin:0;font-size:32px;font-weight:600;line-height:1.15;letter-spacing:-.035em}
.form-note{margin:12px 0 30px;color:#6e6e73;font-size:15px}
.submit-button{width:100%;height:50px;margin-top:4px;justify-content:center;border-radius:12px;box-shadow:none}
.switch-auth{margin-top:22px;font-size:13px}
:deep(.el-form-item){margin-bottom:18px}:deep(.el-form-item__label){font-size:13px;font-weight:600}:deep(.el-input__wrapper){min-height:50px;border-radius:12px;background:rgba(255,255,255,.72)}
@media(max-width:900px){.auth-card{padding:0}.mobile-logo{margin-bottom:48px}}
</style>

<style scoped>
.auth-card { width:min(100%,450px); padding:44px 48px; border-color:rgba(29,29,31,.08); border-radius:28px; background:rgba(255,255,255,.88); box-shadow:0 24px 70px rgba(0,0,0,.09); backdrop-filter:blur(20px); }
.form-kicker { color:var(--rf-blue); font-size:11px; letter-spacing:.14em; }.auth-card h2 { font-size:34px; font-weight:700; letter-spacing:-.045em; }.form-note { margin:12px 0 30px; font-size:14px; }
.submit-button { height:52px; border-radius:999px; }.switch-auth { font-size:13px; }
:deep(.el-form-item__label){font-size:13px}:deep(.el-input__wrapper){border-radius:13px;background:rgba(255,255,255,.8)}
</style>
