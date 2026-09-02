<script setup lang="ts">
withDefaults(defineProps<{ compact?: boolean; light?: boolean }>(), { compact: false, light: false });
</script>

<template>
  <div class="app-logo" :class="{ compact, light }">
    <span class="logo-mark">R</span>
    <span v-if="!compact" class="logo-copy"><strong>ResearchFlow</strong><small>AI RESEARCH OS</small></span>
  </div>
</template>

<style scoped>
.app-logo { display: flex; align-items: center; gap: 11px; }
.logo-mark { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 12px; color: white; background: linear-gradient(145deg, #2997ff, #0071e3 58%, #0057b5); box-shadow: 0 8px 22px rgba(0,113,227,.22); font: 700 19px/1 -apple-system, BlinkMacSystemFont, sans-serif; letter-spacing: -.05em; }
.logo-copy strong, .logo-copy small { display: block; }
.logo-copy strong { color: var(--rf-ink); font-size: 15px; font-weight: 700; letter-spacing: -.025em; }
.logo-copy small { margin-top: 2px; color: #86868b; font-size: 8px; font-weight: 600; letter-spacing: .16em; }
.light .logo-copy strong { color: var(--rf-ink); }
.light .logo-copy small { color: #86868b; }
.compact .logo-mark { width: 36px; height: 36px; font-size: 18px; }
</style>
