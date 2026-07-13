<template>
  <nav class="sticky top-0 z-50 border-b border-slate-200/70 bg-white/90 backdrop-blur-xl">
    <div class="mx-auto max-w-7xl px-4 sm:px-6">
      <div class="flex h-[4.5rem] items-center justify-between gap-4">
        <button class="group flex min-w-0 items-center gap-3 text-left" @click="$emit('navigate', defaultView)">
          <span
            :class="[
              'relative flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl text-white shadow-lg transition group-hover:-rotate-3',
              role === 'admin' ? 'bg-slate-950 shadow-slate-900/15' : 'bg-primary-600 shadow-primary-600/20',
            ]"
          >
            <ShieldCheck v-if="role === 'admin'" class="h-5 w-5" />
            <CreditCard v-else class="h-5 w-5" />
            <span class="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full bg-cyan-400 ring-2 ring-white"></span>
          </span>
          <span class="min-w-0">
            <span class="block truncate text-[15px] font-bold tracking-tight text-slate-950 sm:text-base">
              {{ role === "admin" ? "智能信贷审批管理端" : "智能信贷客户服务端" }}
            </span>
            <span class="hidden text-[11px] font-medium text-slate-400 sm:block">
              {{ role === "admin" ? "风险审批与人工复核工作台" : "在线申请与进度查询中心" }}
            </span>
          </span>
        </button>

        <div class="hidden items-center gap-1 md:flex">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="nav-link"
            :class="currentView === item.key ? 'bg-slate-950 text-white shadow-md shadow-slate-900/10' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-950'"
            @click="$emit('navigate', item.key)"
          >
            <component :is="item.icon" class="h-4 w-4" />
            <span>{{ item.label }}</span>
          </button>

          <div class="mx-2 h-6 w-px bg-slate-200"></div>
          <button class="flex items-center gap-2 rounded-xl px-3 py-2 text-left transition hover:bg-slate-100" @click="$emit('navigate', 'profile')">
            <span class="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-slate-600">
              <User class="h-4 w-4" />
            </span>
            <span class="hidden xl:block">
              <span class="block text-xs font-semibold text-slate-800">{{ currentUser.displayName }}</span>
              <span class="block text-[10px] text-slate-400">{{ currentUser.username }}</span>
            </span>
          </button>
          <button class="rounded-xl p-2.5 text-slate-400 transition hover:bg-red-50 hover:text-red-600" aria-label="退出登录" title="退出登录" @click="$emit('logout')">
            <LogOut class="h-4 w-4" />
          </button>
        </div>

        <button
          class="rounded-xl p-2.5 text-slate-500 transition hover:bg-slate-100 hover:text-slate-950 md:hidden"
          aria-label="打开菜单"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <X v-if="mobileMenuOpen" class="h-5 w-5" />
          <Menu v-else class="h-5 w-5" />
        </button>
      </div>

      <div v-if="mobileMenuOpen" class="space-y-2 border-t border-slate-100 py-4 md:hidden">
        <div class="mb-3 flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3">
          <div>
            <p class="text-sm font-semibold text-slate-800">{{ currentUser.displayName }}</p>
            <p class="text-xs text-slate-400">{{ currentUser.username }}</p>
          </div>
          <span class="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-primary-600 shadow-sm">
            {{ role === "admin" ? "管理员" : "客户" }}
          </span>
        </div>
        <button
          v-for="item in mobileNavItems"
          :key="item.key"
          class="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-medium transition"
          :class="currentView === item.key ? 'bg-slate-950 text-white' : 'text-slate-600 hover:bg-slate-100'"
          @click="handleMobileNav(item.key)"
        >
          <component :is="item.icon" class="h-5 w-5" />
          <span>{{ item.label }}</span>
        </button>
        <button class="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-medium text-red-600 transition hover:bg-red-50" @click="handleMobileLogout">
          <LogOut class="h-5 w-5" />
          <span>退出登录</span>
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, markRaw, ref } from "vue";
import {
  ClipboardList,
  CreditCard,
  FileText,
  Gauge,
  Home,
  LogOut,
  Menu,
  ShieldCheck,
  User,
  X,
} from "lucide-vue-next";

const props = defineProps({
  currentView: { type: String, required: true },
  role: { type: String, required: true },
  currentUser: { type: Object, required: true },
});

const emit = defineEmits(["navigate", "logout"]);
const mobileMenuOpen = ref(false);
const defaultView = computed(() => (props.role === "admin" ? "admin" : "home"));

const navItems = computed(() =>
  props.role === "admin"
    ? [
        { key: "admin", label: "管理总览", icon: markRaw(Gauge) },
        { key: "list", label: "申请管理", icon: markRaw(ClipboardList) },
      ]
    : [
        { key: "home", label: "首页", icon: markRaw(Home) },
        { key: "apply", label: "贷款申请", icon: markRaw(FileText) },
        { key: "list", label: "申请记录", icon: markRaw(ClipboardList) },
      ],
);

const mobileNavItems = computed(() => [
  ...navItems.value,
  { key: "profile", label: "账户中心", icon: markRaw(User) },
]);

function handleMobileNav(key) {
  emit("navigate", key);
  mobileMenuOpen.value = false;
}

function handleMobileLogout() {
  mobileMenuOpen.value = false;
  emit("logout");
}
</script>
