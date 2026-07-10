<template>
  <div class="animate-fadeIn mx-auto max-w-md">
    <div class="card">
      <div class="mb-8 text-center">
        <div
          class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-lg bg-primary-50 text-primary-600"
        >
          <User class="h-7 w-7" />
        </div>
        <h1 class="text-2xl font-bold text-gray-950">演示登录</h1>
        <p class="mt-2 text-sm text-gray-500">
          当前为前端演示登录，不调用后端鉴权。
        </p>
      </div>

      <form class="space-y-5" @submit.prevent="handleLogin">
        <label class="block">
          <span class="form-label">账号</span>
          <div class="relative">
            <User
              class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
            />
            <input
              v-model.trim="form.username"
              class="form-input form-input-icon-left"
              required
              placeholder="请输入账号"
            />
          </div>
          <p v-if="validationErrors.username" class="mt-1 text-xs text-red-600">
            {{ validationErrors.username }}
          </p>
        </label>
        <label class="block">
          <span class="form-label">密码</span>
          <div class="relative">
            <Lock
              class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
            />
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input form-input-icon-left pr-10"
              required
              placeholder="请输入密码"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
              @click="showPassword = !showPassword"
            >
              <component :is="showPassword ? EyeOff : Eye" class="h-5 w-5" />
            </button>
          </div>
          <p v-if="validationErrors.password" class="mt-1 text-xs text-red-600">
            {{ validationErrors.password }}
          </p>
        </label>
        <button class="btn-primary w-full" :disabled="loading" type="submit">
          <Loader2 v-if="loading" class="h-5 w-5 animate-spin" />
          登录
        </button>
      </form>

      <button class="btn-secondary mt-4 w-full" @click="$emit('back')">
        返回首页
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { Eye, EyeOff, Loader2, Lock, User } from "lucide-vue-next";

const emit = defineEmits(["back", "logged-in"]);

const form = reactive({
  username: "",
  password: "",
});
const showPassword = ref(false);
const loading = ref(false);
const validationErrors = reactive({
  username: "",
  password: "",
});

function validateUsername(username) {
  if (!username) return "请输入账号";
  if (username.length < 6 || username.length > 20)
    return "账号长度需在6-20位之间";
  const regex = /^[a-zA-Z0-9_]+$/;
  if (!regex.test(username)) return "账号只能包含字母、数字和下划线";
  return "";
}

function validatePassword(password) {
  if (!password) return "请输入密码";
  if (password.length < 8 || password.length > 20)
    return "密码长度需在8-20位之间";
  if (!/[a-zA-Z]/.test(password)) return "密码必须包含字母";
  if (!/[0-9]/.test(password)) return "密码必须包含数字";
  return "";
}

async function handleLogin() {
  validationErrors.username = validateUsername(form.username);
  validationErrors.password = validatePassword(form.password);

  if (validationErrors.username || validationErrors.password) {
    return;
  }

  loading.value = true;
  await new Promise((resolve) => setTimeout(resolve, 500));
  loading.value = false;
  emit("logged-in");
}
</script>
