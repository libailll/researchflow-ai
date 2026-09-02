import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { authApi } from "@/api/auth";
import { TOKEN_KEY } from "@/api/http";
import { userApi } from "@/api/user";
import type { User } from "@/types/model";

export const useAuthStore = defineStore("auth", () => {
  const user = ref<User | null>(null);
  const initialized = ref(false);
  const isAuthenticated = computed(() => Boolean(localStorage.getItem(TOKEN_KEY)));

  async function login(username: string, password: string) {
    const result = await authApi.login(username, password);
    localStorage.setItem(TOKEN_KEY, result.token);
    user.value = result.user;
  }

  async function initialize() {
    if (!localStorage.getItem(TOKEN_KEY)) { initialized.value = true; return; }
    try { user.value = await userApi.me(); }
    catch { localStorage.removeItem(TOKEN_KEY); user.value = null; }
    finally { initialized.value = true; }
  }

  async function logout() {
    try { await authApi.logout(); } catch { /* clear the local session regardless */ }
    localStorage.removeItem(TOKEN_KEY); user.value = null;
  }

  function setUser(next: User) { user.value = next; }
  return { user, initialized, isAuthenticated, login, initialize, logout, setUser };
});

