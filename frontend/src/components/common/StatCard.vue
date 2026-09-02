<script setup lang="ts">
import type { Component } from "vue";
defineProps<{ label: string; value: number; note: string; tone: string; icon: Component }>();
</script>
<template><article class="stat-card"><span class="stat-icon" :class="tone"><el-icon><component :is="icon" /></el-icon></span><div><p>{{ label }}</p><strong>{{ value }}</strong><small>{{ note }}</small></div></article></template>
<style scoped>
.stat-card { min-height: 142px; padding: 24px; display: flex; align-items: center; gap: 18px; border: 1px solid rgba(29,29,31,.07); border-radius: 24px; background: rgba(255,255,255,.88); box-shadow: var(--rf-shadow-sm); transition: transform .22s ease, box-shadow .22s ease; }
.stat-card:hover { transform: translateY(-3px); box-shadow: var(--rf-shadow-md); }
.stat-icon { width: 48px; height: 48px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 15px; font-size: 21px; }
.stat-icon.blue { color: #0071e3; background: #e8f3ff; }.stat-icon.navy { color: #4c5f78; background: #edf1f6; }.stat-icon.mint { color: #087f6d; background: #e3f7f3; }.stat-icon.orange { color: #c84f2f; background: #fff0eb; }
.stat-card p,.stat-card strong,.stat-card small { display: block; margin: 0; }
.stat-card p { color: var(--rf-muted); font-size: 12px; font-weight: 600; }
.stat-card strong { margin: 4px 0 3px; color: var(--rf-ink); font: 700 32px/1.05 -apple-system, BlinkMacSystemFont, sans-serif; letter-spacing: -.045em; }
.stat-card small { color: #86868b; font-size: 11px; }
</style>
