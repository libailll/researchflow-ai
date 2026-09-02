<script setup lang="ts">
import { reactive, ref, watchEffect } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { Edit } from "@element-plus/icons-vue";
import { userApi } from "@/api/user";
import { useAuthStore } from "@/stores/auth";
import { initials } from "@/utils/format";

const auth=useAuthStore();const dialog=ref(false);const saving=ref(false);const formRef=ref<FormInstance>();const form=reactive({nickname:"",email:"",avatar:""});
const rules:FormRules={nickname:[{required:true,message:"请输入昵称",trigger:"blur"}],email:[{type:"email",message:"邮箱格式不正确",trigger:"blur"}]};
watchEffect(()=>{if(auth.user)Object.assign(form,{nickname:auth.user.nickname,email:auth.user.email||"",avatar:auth.user.avatar||""});});
async function submit(){await formRef.value?.validate();saving.value=true;try{const user=await userApi.updateMe(form);auth.setUser(user);dialog.value=false;ElMessage.success("个人资料已更新");}catch(error){ElMessage.error((error as Error).message);}finally{saving.value=false;}}
</script>

<template>
  <section><header class="page-heading"><div><p>PERSONAL PROFILE</p><h2>个人资料</h2><span>管理你的公开身份与联系方式。</span></div><el-button type="primary" :icon="Edit" @click="dialog=true">编辑资料</el-button></header><article class="profile-card"><div class="profile-banner"><div class="profile-decoration"><i/><i/><i/></div><span class="profile-avatar">{{ initials(auth.user?.nickname) }}</span></div><div class="profile-body"><h2>{{ auth.user?.nickname }}</h2><p>@{{ auth.user?.username }}</p><dl><div><dt>用户编号</dt><dd>RF-{{ String(auth.user?.id||0).padStart(5,"0") }}</dd></div><div><dt>电子邮箱</dt><dd>{{ auth.user?.email||"尚未设置" }}</dd></div><div><dt>头像地址</dt><dd>{{ auth.user?.avatar||"使用默认头像" }}</dd></div></dl></div></article></section>
  <el-dialog v-model="dialog" title="编辑个人资料" width="520px"><p class="dialog-intro">这些信息会展示给与你协作的项目成员。</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" maxlength="50" /></el-form-item><el-form-item label="电子邮箱" prop="email"><el-input v-model="form.email" placeholder="name@example.com" /></el-form-item><el-form-item label="头像地址"><el-input v-model="form.avatar" placeholder="https://…" /></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存资料</el-button></template></el-dialog>
</template>

<style scoped>
.page-heading{margin-bottom:25px;display:flex;align-items:flex-end;justify-content:space-between}.page-heading p{margin:0 0 5px;color:#8996a9;font-size:8px;font-weight:800;letter-spacing:.18em}.page-heading h2{margin:0;font-size:27px;letter-spacing:-.04em}.page-heading span{display:block;margin-top:8px;color:#7b8799;font-size:11px}.profile-card{max-width:780px;overflow:hidden;border:1px solid var(--rf-line);border-radius:18px;background:white;box-shadow:0 12px 36px rgba(29,47,77,.04)}.profile-banner{position:relative;height:170px;padding:0 42px;display:flex;align-items:flex-end;overflow:hidden;background:linear-gradient(120deg,#10264d,#235795)}.profile-decoration{position:absolute;inset:0}.profile-decoration i{position:absolute;border:1px solid rgba(109,217,201,.16);border-radius:50%}.profile-decoration i:first-child{width:280px;height:280px;right:-60px;top:-110px}.profile-decoration i:nth-child(2){width:190px;height:190px;right:-15px;top:-65px}.profile-decoration i:last-child{width:90px;height:90px;right:36px;top:-15px}.profile-avatar{position:relative;z-index:1;width:100px;height:100px;margin-bottom:-50px;display:grid;place-items:center;border:6px solid white;border-radius:50%;color:#164258;background:#75d5c4;font-size:23px;font-weight:800}.profile-body{padding:69px 42px 38px}.profile-body h2{margin:0;font:700 25px/1.2 Georgia,"Noto Serif SC",serif}.profile-body>p{margin:6px 0 28px;color:#8792a3;font-size:10px}.profile-body dl{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}.profile-body dl div{padding:16px;border-radius:11px;background:#f6f8fb}.profile-body dt{color:#939dab;font-size:8px}.profile-body dd{margin:7px 0 0;overflow:hidden;color:#354159;font-size:9px;text-overflow:ellipsis;white-space:nowrap}.dialog-intro{margin:-8px 0 22px;color:var(--rf-muted);font-size:11px}
@media(max-width:700px){.page-heading{align-items:flex-start;gap:14px}.page-heading h2{font-size:22px}.profile-banner{padding:0 24px}.profile-body{padding-left:24px;padding-right:24px}.profile-body dl{grid-template-columns:1fr}}
</style>

<style scoped>
.profile-card { max-width:900px; }
.profile-banner { height:210px; padding:0 48px; background:radial-gradient(circle at 78% 5%,rgba(41,151,255,.4),transparent 32%),linear-gradient(135deg,#111217,#154b83); }
.profile-avatar { width:112px; height:112px; margin-bottom:-56px; border-width:7px; color:white; background:linear-gradient(145deg,#2997ff,#0071e3); font-size:26px; }
.profile-body { padding:78px 48px 44px; }.profile-body dl { gap:20px; }.profile-body dl div { padding:19px; border-radius:16px; background:#f5f5f7; }
@media(max-width:700px){.profile-banner{padding:0 26px}.profile-body{padding-left:26px;padding-right:26px}}
</style>
