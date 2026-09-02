<script setup lang="ts">
import { onMounted } from "vue";
import { useAuthStore } from "@/stores/auth";

const authStore = useAuthStore();
onMounted(() => authStore.initialize());
</script>

<template>
  <router-view />
</template>

