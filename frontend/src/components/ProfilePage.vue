<template>
  <div class="animate-fadeIn mx-auto max-w-5xl space-y-6">
    <div class="card flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
      <div class="flex items-center gap-4">
        <div :class="['flex h-16 w-16 items-center justify-center rounded-2xl', isAdmin ? 'bg-slate-950 text-white' : 'bg-primary-50 text-primary-600']">
          <ShieldCheck v-if="isAdmin" class="h-8 w-8" />
          <User v-else class="h-8 w-8" />
        </div>
        <div>
          <div class="flex flex-wrap items-center gap-2">
            <h1 class="text-2xl font-bold text-gray-950">{{ account.displayName }}</h1>
            <span :class="['rounded-full px-2.5 py-1 text-xs font-semibold', isAdmin ? 'bg-slate-100 text-slate-700' : 'bg-primary-50 text-primary-700']">
              {{ isAdmin ? "管理员" : "客户" }}
            </span>
          </div>
          <p class="mt-1 text-sm text-gray-500">登录账号：{{ account.username }}</p>
        </div>
      </div>
      <button class="btn-secondary" @click="$emit('logout')">
        <LogOut class="h-5 w-5" />
        退出登录
      </button>
    </div>

    <div class="grid gap-4 md:grid-cols-3">
      <button v-for="action in actions" :key="action.key" class="card text-left transition hover:-translate-y-0.5 hover:border-primary-100 hover:shadow-md" @click="$emit('navigate', action.key)">
        <component :is="action.icon" class="mb-4 h-6 w-6 text-primary-600" />
        <h2 class="font-semibold text-gray-900">{{ action.title }}</h2>
        <p class="mt-2 text-sm leading-6 text-gray-500">{{ action.description }}</p>
      </button>
    </div>

    <div class="card">
      <h2 class="mb-3 text-lg font-semibold text-gray-900">账户权限</h2>
      <p class="text-sm leading-7 text-gray-600">
        {{ isAdmin
          ? "当前账号可访问管理总览、全量申请记录和人工复核工作台，可对待复核案件执行通过或拒绝操作。"
          : "当前账号可提交贷款申请、上传材料、查询审批进度并查看申请记录，无法进入管理端审批功能。" }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw } from "vue";
import {
  ClipboardList,
  FileText,
  Gauge,
  HelpCircle,
  LogOut,
  ShieldCheck,
  User,
} from "lucide-vue-next";

const props = defineProps({
  account: { type: Object, required: true },
});

defineEmits(["back", "logout", "navigate"]);

const isAdmin = computed(() => props.account.role === "admin");
const actions = computed(() =>
  isAdmin.value
    ? [
        { key: "admin", title: "管理总览", description: "查看审批指标和待人工复核队列。", icon: markRaw(Gauge) },
        { key: "list", title: "申请管理", description: "检索并查看全部贷款申请。", icon: markRaw(ClipboardList) },
        { key: "admin", title: "复核工作台", description: "返回管理总览处理待复核案件。", icon: markRaw(ShieldCheck) },
      ]
    : [
        { key: "apply", title: "发起申请", description: "进入贷款申请表单并提交材料。", icon: markRaw(FileText) },
        { key: "list", title: "申请记录", description: "查看已提交的贷款申请。", icon: markRaw(ClipboardList) },
        { key: "tutorial", title: "使用指南", description: "了解申请材料和审批流程。", icon: markRaw(HelpCircle) },
      ],
);
</script>
