<template>
  <div class="login-shell min-h-screen">
    <div class="mx-auto grid min-h-screen max-w-7xl items-center gap-12 px-6 py-12 lg:grid-cols-[1.05fr_0.95fr] lg:px-10">
      <section class="hidden text-white lg:block">
        <div class="mb-8 inline-flex items-center gap-3 rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur">
          <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-400 text-slate-950">
            <Landmark class="h-5 w-5" />
          </span>
          <div>
            <p class="font-semibold">智能信贷审批系统</p>
            <p class="text-xs text-slate-300">AI Loan Approval Workspace</p>
          </div>
        </div>
        <p class="text-sm font-semibold uppercase tracking-[0.24em] text-cyan-300">Role-based workspace</p>
        <h1 class="mt-5 max-w-2xl text-5xl font-bold leading-tight">
          一个入口，连接客户申请与审批管理
        </h1>
        <p class="mt-6 max-w-xl text-base leading-8 text-slate-300">
          客户账号进入申请与进度查询界面，管理员账号进入审批驾驶舱和人工复核工作台。
        </p>

        <div class="mt-12 grid max-w-xl grid-cols-2 gap-4">
          <div class="rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
            <FileText class="h-6 w-6 text-cyan-300" />
            <p class="mt-4 font-semibold">客户服务台</p>
            <p class="mt-2 text-sm leading-6 text-slate-400">提交材料、跟踪审批、查看申请记录。</p>
          </div>
          <div class="rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
            <ShieldCheck class="h-6 w-6 text-emerald-300" />
            <p class="mt-4 font-semibold">审批管理台</p>
            <p class="mt-2 text-sm leading-6 text-slate-400">总览业务、处理复核、查看案件状态。</p>
          </div>
        </div>
      </section>

      <section class="animate-fadeIn mx-auto w-full max-w-md">
        <div class="rounded-3xl border border-white/60 bg-white/95 p-7 shadow-2xl shadow-slate-950/20 backdrop-blur sm:p-9">
          <div class="mb-8">
            <div class="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-950 text-white lg:hidden">
              <Landmark class="h-6 w-6" />
            </div>
            <p class="text-sm font-semibold text-primary-600">欢迎登录</p>
            <h2 class="mt-2 text-3xl font-bold tracking-tight text-slate-950">选择你的工作空间</h2>
            <p class="mt-3 text-sm leading-6 text-slate-500">系统将根据登录账号自动进入对应界面。</p>
          </div>

          <form class="space-y-5" @submit.prevent="handleLogin">
            <label class="block">
              <span class="form-label">账号</span>
              <div class="relative">
                <User class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                <input
                  v-model.trim="form.username"
                  class="form-input form-input-icon-left"
                  autocomplete="username"
                  required
                  placeholder="请输入登录账号"
                  @input="errorMessage = ''"
                />
              </div>
            </label>

            <label class="block">
              <span class="form-label">密码</span>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="form-input form-input-icon-left pr-11"
                  autocomplete="current-password"
                  required
                  placeholder="请输入登录密码"
                  @input="errorMessage = ''"
                />
                <button
                  type="button"
                  class="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  @click="showPassword = !showPassword"
                >
                  <component :is="showPassword ? EyeOff : Eye" class="h-5 w-5" />
                </button>
              </div>
            </label>

            <div v-if="errorMessage" class="flex items-start gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
              <CircleAlert class="mt-0.5 h-4 w-4 shrink-0" />
              <span>{{ errorMessage }}</span>
            </div>

            <button class="btn-primary w-full" :disabled="loading" type="submit">
              <Loader2 v-if="loading" class="h-5 w-5 animate-spin" />
              <LogIn v-else class="h-5 w-5" />
              {{ loading ? "正在登录..." : "登录系统" }}
            </button>
          </form>

          <div class="mt-7 border-t border-slate-100 pt-6">
            <p class="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">演示账号</p>
            <div class="grid grid-cols-2 gap-3">
              <button
                v-for="account in demoAccounts"
                :key="account.username"
                type="button"
                class="rounded-xl border border-slate-200 p-3 text-left transition hover:border-primary-300 hover:bg-primary-50"
                @click="fillAccount(account)"
              >
                <span class="block text-sm font-semibold text-slate-800">{{ account.label }}</span>
                <span class="mt-1 block text-xs text-slate-500">{{ account.username }} / {{ account.password }}</span>
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import {
  CircleAlert,
  Eye,
  EyeOff,
  FileText,
  Landmark,
  Loader2,
  Lock,
  LogIn,
  ShieldCheck,
  User,
} from "lucide-vue-next";
import { authenticate } from "../services/auth";

const emit = defineEmits(["logged-in"]);

const demoAccounts = [
  { label: "管理端", username: "admin", password: "admin123" },
  { label: "客户端", username: "user", password: "user123" },
];
const form = reactive({ username: "", password: "" });
const showPassword = ref(false);
const loading = ref(false);
const errorMessage = ref("");

function fillAccount(account) {
  form.username = account.username;
  form.password = account.password;
  errorMessage.value = "";
}

async function handleLogin() {
  errorMessage.value = "";
  loading.value = true;
  await new Promise((resolve) => setTimeout(resolve, 350));

  const account = authenticate(form.username, form.password);
  loading.value = false;

  if (!account) {
    errorMessage.value = "账号或密码错误，请使用下方演示账号登录。";
    return;
  }

  emit("logged-in", account);
}
</script>
