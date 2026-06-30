<template>
  <nav class="sticky top-0 z-50 border-b border-gray-100 bg-white/95 backdrop-blur">
    <div class="mx-auto max-w-7xl px-4">
      <div class="flex h-16 items-center justify-between">
        <button class="flex items-center gap-3 text-left" @click="$emit('navigate', 'home')">
          <span class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-600 text-white">
            <CreditCard class="h-6 w-6" />
          </span>
          <span>
            <span class="block text-lg font-bold text-gray-900">智能信贷审批</span>
            <span class="block text-xs text-gray-500">AI 驱动的贷款审批工作台</span>
          </span>
        </button>

        <div class="hidden items-center gap-2 md:flex">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition"
            :class="currentView === item.key ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'"
            @click="handleNavClick(item.key)"
          >
            <component :is="item.icon" class="h-4 w-4" />
            <span>{{ item.label }}</span>
          </button>
        </div>

        <button class="rounded-lg p-2 text-gray-600 hover:bg-gray-100 md:hidden" @click="mobileMenuOpen = !mobileMenuOpen">
          <Menu class="h-6 w-6" />
        </button>
      </div>

      <div v-if="mobileMenuOpen" class="space-y-2 pb-4 md:hidden">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-left text-sm transition"
          :class="currentView === item.key ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-50'"
          @click="handleMobileNav(item.key)"
        >
          <component :is="item.icon" class="h-5 w-5" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, markRaw, ref } from 'vue'
import { ClipboardList, CreditCard, FileText, Home, LogIn, Menu, User } from 'lucide-vue-next'

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
  if (key === 'login') {
    emit('login')
  } else {
    emit('navigate', key)
  }
}

function handleMobileNav(key) {
  handleNavClick(key)
  mobileMenuOpen.value = false
}
</script>
