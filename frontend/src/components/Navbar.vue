<template>
  <nav class="sticky top-0 z-50 border-b border-slate-200/70 bg-white/85 backdrop-blur-xl">
    <div class="mx-auto max-w-7xl px-4 sm:px-6">
      <div class="flex h-[4.5rem] items-center justify-between">
        <button class="group flex items-center gap-3 text-left" @click="$emit('navigate', 'home')">
          <span class="relative flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-950 text-white shadow-lg shadow-slate-900/15 transition group-hover:-rotate-3">
            <CreditCard class="h-5 w-5" />
            <span class="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full bg-cyan-400 ring-2 ring-white"></span>
          </span>
          <span>
            <span class="block text-[15px] font-bold tracking-tight text-slate-950 sm:text-base">智能信贷审批</span>
            <span class="hidden text-[11px] font-medium text-slate-400 sm:block">AI 驱动的贷款审批工作台</span>
          </span>
        </button>

        <div class="hidden items-center gap-1 md:flex">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="nav-link"
            :class="currentView === item.key ? 'bg-slate-950 text-white shadow-md shadow-slate-900/10' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-950'"
            @click="handleNavClick(item.key)"
          >
            <component :is="item.icon" class="h-4 w-4" />
            <span>{{ item.label }}</span>
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
        <button
          v-for="item in navItems"
          :key="item.key"
          class="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-medium transition"
          :class="currentView === item.key ? 'bg-slate-950 text-white' : 'text-slate-600 hover:bg-slate-100'"
          @click="handleMobileNav(item.key)"
        >
          <component :is="item.icon" class="h-5 w-5" />
          <span>{{ item.label }}</span>
        </button>
        <div class="flex items-center gap-2 px-4 pt-2 text-xs font-semibold text-emerald-700">
          <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
          演示环境 · 接口已连接
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, markRaw, ref } from 'vue'
import { ClipboardList, CreditCard, FileText, Home, LogIn, Menu, User, X } from 'lucide-vue-next'

const props = defineProps({
  currentView: {
    type: String,
    required: true,
  },
  isLoggedIn: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['navigate', 'login'])
const mobileMenuOpen = ref(false)

const navItems = computed(() => [
  { key: 'home', label: '首页', icon: markRaw(Home) },
  { key: 'apply', label: '贷款申请', icon: markRaw(FileText) },
  { key: 'list', label: '申请记录', icon: markRaw(ClipboardList) },
  {
    key: props.isLoggedIn ? 'profile' : 'login',
    label: props.isLoggedIn ? '我的' : '登录',
    icon: markRaw(props.isLoggedIn ? User : LogIn),
  },
])

function handleNavClick(key) {
  if (key === 'login') emit('login')
  else emit('navigate', key)
}

function handleMobileNav(key) {
  handleNavClick(key)
  mobileMenuOpen.value = false
}
</script>
