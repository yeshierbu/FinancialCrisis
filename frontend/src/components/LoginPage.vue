<template>
  <div class="animate-fadeIn mx-auto max-w-md">
    <div class="card">
      <div class="mb-8 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
          <User class="h-7 w-7" />
        </div>
        <h1 class="text-2xl font-bold text-gray-950">演示登录</h1>
        <p class="mt-2 text-sm text-gray-500">当前为前端演示登录，不调用后端鉴权。</p>
      </div>

      <form class="space-y-5" @submit.prevent="handleLogin">
        <label class="block">
          <span class="form-label">手机号</span>
          <div class="relative">
            <Phone class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
            <input v-model.trim="form.mobile" class="form-input pl-10" required placeholder="13800138000" />
          </div>
        </label>
        <label class="block">
          <span class="form-label">密码</span>
          <div class="relative">
            <Lock class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
            <input v-model="form.password" :type="showPassword ? 'text' : 'password'" class="form-input pl-10 pr-10" required placeholder="请输入任意密码" />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" @click="showPassword = !showPassword">
              <component :is="showPassword ? EyeOff : Eye" class="h-5 w-5" />
            </button>
          </div>
        </label>
        <button class="btn-primary w-full" :disabled="loading" type="submit">
          <Loader2 v-if="loading" class="h-5 w-5 animate-spin" />
          登录
        </button>
      </form>

      <button class="btn-secondary mt-4 w-full" @click="$emit('back')">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { Eye, EyeOff, Loader2, Lock, Phone, User } from 'lucide-vue-next'

const emit = defineEmits(['back', 'logged-in'])

const form = reactive({
  mobile: '',
  password: '',
})
const showPassword = ref(false)
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  await new Promise((resolve) => setTimeout(resolve, 500))
  loading.value = false
  emit('logged-in')
}
</script>
