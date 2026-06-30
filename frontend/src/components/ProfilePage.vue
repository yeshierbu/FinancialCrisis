<template>
  <div class="animate-fadeIn mx-auto max-w-5xl space-y-6">
    <div class="card flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
      <div class="flex items-center gap-4">
        <div class="flex h-16 w-16 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
          <User class="h-8 w-8" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-950">演示用户</h1>
          <p class="mt-1 text-sm text-gray-500">138****8000</p>
        </div>
      </div>
      <button class="btn-secondary" @click="$emit('logout')">
        <LogOut class="h-5 w-5" />
        退出登录
      </button>
    </div>

    <div class="grid gap-4 md:grid-cols-3">
      <button v-for="action in actions" :key="action.key" class="card text-left hover:bg-gray-50" @click="handleAction(action.key)">
        <component :is="action.icon" class="mb-4 h-6 w-6 text-primary-600" />
        <h2 class="font-semibold text-gray-900">{{ action.title }}</h2>
        <p class="mt-2 text-sm text-gray-500">{{ action.description }}</p>
      </button>
    </div>

    <div class="card">
      <h2 class="mb-4 text-lg font-semibold text-gray-900">账户说明</h2>
      <p class="text-sm leading-6 text-gray-600">
        当前项目尚未实现用户鉴权模块，登录页和个人中心保留为前端演示能力。贷款申请、申请列表、状态查询已经接入后端接口。
      </p>
    </div>
  </div>
</template>

<script setup>
import { markRaw } from 'vue'
import { ClipboardList, FileText, Headphones, LogOut, User } from 'lucide-vue-next'

const emit = defineEmits(['back', 'logout', 'navigate'])

const actions = [
  { key: 'apply', title: '发起申请', description: '进入贷款申请表单。', icon: markRaw(FileText) },
  { key: 'list', title: '申请记录', description: '查看后端内存中的申请列表。', icon: markRaw(ClipboardList) },
  { key: 'home', title: '联系支持', description: '演示项目可在 README 中补充支持信息。', icon: markRaw(Headphones) },
]

function handleAction(key) {
  emit('navigate', key)
}
</script>
