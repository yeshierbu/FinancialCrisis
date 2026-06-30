<template>
  <div class="animate-fadeIn space-y-8">
    <section class="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
      <div class="rounded-lg border border-gray-100 bg-white p-8 shadow-sm">
        <div class="mb-6 inline-flex items-center gap-2 rounded-full bg-primary-50 px-3 py-1 text-sm font-medium text-primary-700">
          <Sparkles class="h-4 w-4" />
          智能审批主流程已接入后端
        </div>
        <h1 class="max-w-3xl text-3xl font-bold leading-tight text-gray-950 md:text-5xl">
          智能信贷审批系统
        </h1>
        <p class="mt-5 max-w-2xl text-base leading-7 text-gray-600">
          在线提交贷款申请、上传材料元数据，并通过 Spring Boot 后端的 Agent 编排流程完成材料校验、风险分析和审批决策。
        </p>
        <div class="mt-8 flex flex-wrap gap-3">
          <button class="btn-primary" @click="$emit('start-application')">
            <ArrowRight class="h-5 w-5" />
            发起申请
          </button>
          <button class="btn-secondary" @click="$emit('view-list')">
            <ClipboardList class="h-5 w-5" />
            查看记录
          </button>
          <button class="btn-secondary" @click="$emit('view-tutorial')">
            <BookOpen class="h-5 w-5" />
            使用指南
          </button>
        </div>
      </div>

      <div class="rounded-lg border border-gray-100 bg-gray-900 p-6 text-white shadow-sm">
        <div class="flex items-center justify-between border-b border-white/10 pb-4">
          <div>
            <p class="text-sm text-white/60">审批工作台</p>
            <h2 class="text-xl font-semibold">今日流程状态</h2>
          </div>
          <Activity class="h-6 w-6 text-success-500" />
        </div>
        <div class="mt-6 grid grid-cols-2 gap-4">
          <div v-for="item in metrics" :key="item.label" class="rounded-lg bg-white/10 p-4">
            <p class="text-2xl font-bold">{{ item.value }}</p>
            <p class="mt-1 text-sm text-white/60">{{ item.label }}</p>
          </div>
        </div>
        <div class="mt-6 space-y-3">
          <div v-for="step in flow" :key="step" class="flex items-center gap-3 text-sm text-white/80">
            <CheckCircle2 class="h-4 w-4 text-success-500" />
            <span>{{ step }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-4 md:grid-cols-3">
      <div v-for="feature in features" :key="feature.title" class="card">
        <div :class="['mb-4 flex h-11 w-11 items-center justify-center rounded-lg', feature.bg]">
          <component :is="feature.icon" :class="['h-6 w-6', feature.color]" />
        </div>
        <h3 class="font-semibold text-gray-900">{{ feature.title }}</h3>
        <p class="mt-2 text-sm leading-6 text-gray-600">{{ feature.description }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { markRaw } from 'vue'
import {
  Activity,
  ArrowRight,
  BookOpen,
  BrainCircuit,
  CheckCircle2,
  ClipboardList,
  FileSearch,
  ShieldCheck,
  Sparkles,
} from 'lucide-vue-next'

defineEmits(['start-application', 'view-tutorial', 'view-list'])

const metrics = [
  { label: '平均审批时长', value: '3 分钟' },
  { label: '流程节点', value: '4 个' },
  { label: '材料类型', value: '3 类' },
  { label: '接口状态', value: '已连接' },
]

const flow = ['创建申请', '材料接收与模拟 OCR', '反欺诈与偿债能力分析', '合规决策与报告生成']

const features = [
  {
    icon: markRaw(FileSearch),
    title: '材料闭环',
    description: '前端选择文件后，向后端提交材料类型、文件名、大小和模拟地址，驱动审批流继续前进。',
    bg: 'bg-blue-100',
    color: 'text-blue-600',
  },
  {
    icon: markRaw(BrainCircuit),
    title: 'Agent 编排',
    description: '后端按材料解析、风险评估、偿债能力和合规决策顺序同步编排。',
    bg: 'bg-green-100',
    color: 'text-green-600',
  },
  {
    icon: markRaw(ShieldCheck),
    title: '状态可追踪',
    description: '申请状态和时间线来自真实接口，方便演示和继续扩展数据库持久化。',
    bg: 'bg-orange-100',
    color: 'text-orange-600',
  },
]
</script>
