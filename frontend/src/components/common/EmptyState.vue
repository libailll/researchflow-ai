<script setup lang="ts">
defineProps<{ title: string; description: string; action?: string }>(); defineEmits<{ action: [] }>();
</script>
<template><div class="empty-state"><span class="empty-mark">R</span><h2>{{ title }}</h2><p>{{ description }}</p><el-button v-if="action" type="primary" @click="$emit('action')">{{ action }}</el-button></div></template>
<style scoped>.empty-state { min-height: calc(100vh - 210px); display: grid; place-content: center; justify-items: center; text-align: center; }.empty-mark { width: 64px; height: 64px; display: grid; place-items: center; border-radius: 20px; color: white; background: linear-gradient(145deg,#2997ff,#0071e3); box-shadow: 0 18px 38px rgba(0,113,227,.22); font: 700 28px/1 -apple-system,BlinkMacSystemFont,sans-serif; }.empty-state h2 { margin: 26px 0 9px; font-size: 30px; letter-spacing: -.04em; }.empty-state p { max-width: 440px; margin: 0 0 26px; color: var(--rf-muted); font-size: 14px; line-height: 1.75; }</style>
