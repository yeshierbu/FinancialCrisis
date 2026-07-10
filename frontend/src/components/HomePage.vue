<template>
  <div class="animate-fadeIn space-y-10">
    <section class="hero-surface overflow-hidden rounded-[2rem] px-6 py-8 text-white shadow-2xl shadow-slate-900/10 sm:px-10 sm:py-12 lg:px-14 lg:py-14">
      <div class="relative z-10 grid gap-12 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
        <div>
          <div class="mb-6 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1.5 text-sm text-blue-100 backdrop-blur">
            <Sparkles class="h-4 w-4 text-cyan-300" />
            协作式 Agent 审批工作台
          </div>
          <h1 class="max-w-2xl text-4xl font-semibold leading-tight tracking-tight sm:text-5xl lg:text-6xl">
            让每一笔申请，<span class="text-cyan-300">都有依据</span>可追溯
          </h1>
          <p class="mt-6 max-w-xl text-base leading-8 text-slate-300 sm:text-lg">
            从材料采集、反欺诈、偿债能力到协作审查，多个专业 Agent 共同完成信贷审批，并把每一步决策留在时间线上。
          </p>
          <div class="mt-9 flex flex-wrap gap-3">
            <button class="btn-hero" @click="$emit('start-application')">
              发起贷款申请
              <ArrowUpRight class="h-5 w-5" />
            </button>
            <button class="btn-hero-ghost" @click="$emit('view-list')">
              查看申请记录
              <ClipboardList class="h-5 w-5" />
            </button>
          </div>
          <div class="mt-9 flex flex-wrap gap-x-6 gap-y-3 text-sm text-slate-400">
            <span class="flex items-center gap-2"><ShieldCheck class="h-4 w-4 text-emerald-400" />规则兜底</span>
            <span class="flex items-center gap-2"><Fingerprint class="h-4 w-4 text-cyan-300" />风险可解释</span>
            <span class="flex items-center gap-2"><Activity class="h-4 w-4 text-violet-300" />全程可回放</span>
          </div>
        </div>

        <div class="glass-panel rounded-3xl p-5 sm:p-6">
          <div class="flex items-start justify-between border-b border-white/10 pb-5">
            <div>
              <p class="text-xs uppercase tracking-[0.2em] text-slate-400">Approval cockpit</p>
              <h2 class="mt-2 text-xl font-semibold">审批协作链路</h2>
            </div>
            <span class="inline-flex items-center gap-2 rounded-full bg-emerald-400/10 px-3 py-1.5 text-xs font-medium text-emerald-300">
              <span class="h-1.5 w-1.5 rounded-full bg-emerald-400 shadow-[0_0_10px_#34d399]"></span>
              系统在线
            </span>
          </div>
          <div class="mt-6 space-y-3">
            <div v-for="(stage, index) in agentStages" :key="stage.title" class="flex items-center gap-4 rounded-2xl border border-white/10 bg-white/[0.06] p-4">
              <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl" :class="stage.iconBg">
                <component :is="stage.icon" class="h-5 w-5" :class="stage.iconColor" />
              </span>
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-3">
                  <p class="font-medium text-white">{{ stage.title }}</p>
                  <span class="text-xs text-slate-500">0{{ index + 1 }}</span>
                </div>
                <p class="mt-1 truncate text-xs text-slate-400">{{ stage.description }}</p>
              </div>
              <CheckCircle2 class="h-4 w-4 shrink-0 text-emerald-400" />
            </div>
          </div>
          <div class="mt-5 flex items-center gap-3 rounded-2xl bg-cyan-400/10 px-4 py-3 text-sm text-cyan-100">
            <BrainCircuit class="h-5 w-5 text-cyan-300" />
            <span>Supervisor Agent 将不同结论汇总后交给规则决策。</span>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="flex items-center justify-between">
          <span class="text-sm text-slate-500">{{ metric.label }}</span>
          <component :is="metric.icon" class="h-5 w-5 text-slate-300" />
        </div>
        <p class="mt-4 text-2xl font-semibold tracking-tight text-slate-950">{{ metric.value }}</p>
        <p class="mt-1 text-xs text-emerald-600">{{ metric.note }}</p>
      </div>
    </section>

    <section class="grid gap-6 lg:grid-cols-[1fr_0.72fr]">
      <div>
        <div class="mb-5 flex items-end justify-between gap-4">
          <div>
            <p class="eyebrow">Why it works</p>
            <h2 class="section-title">一套可解释的审批体验</h2>
          </div>
          <button class="hidden text-sm font-medium text-primary-700 hover:text-primary-800 sm:inline-flex sm:items-center sm:gap-1" @click="$emit('view-tutorial')">
            查看指南 <ArrowRight class="h-4 w-4" />
          </button>
        </div>
        <div class="grid gap-4 md:grid-cols-3">
          <div v-for="feature in features" :key="feature.title" class="feature-card">
            <div :class="['mb-5 flex h-12 w-12 items-center justify-center rounded-2xl', feature.bg]">
              <component :is="feature.icon" :class="['h-6 w-6', feature.color]" />
            </div>
            <h3 class="font-semibold text-slate-950">{{ feature.title }}</h3>
            <p class="mt-2 text-sm leading-6 text-slate-500">{{ feature.description }}</p>
          </div>
        </div>
      </div>

      <div class="rounded-3xl border border-blue-100 bg-gradient-to-br from-blue-50 to-cyan-50 p-6 sm:p-7">
        <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-primary-700 shadow-sm">
          <Route class="h-5 w-5" />
        </div>
        <p class="eyebrow mt-6 text-primary-700">From application to answer</p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">三步完成一笔申请</h2>
        <div class="mt-6 space-y-5">
          <div v-for="(step, index) in simpleSteps" :key="step.title" class="flex gap-4">
            <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-950 text-sm font-semibold text-white">{{ index + 1 }}</span>
            <div>
              <p class="font-medium text-slate-900">{{ step.title }}</p>
              <p class="mt-1 text-sm leading-6 text-slate-500">{{ step.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { markRaw } from 'vue'
import {
  Activity,
  ArrowRight,
  ArrowUpRight,
  BrainCircuit,
  CheckCircle2,
  ClipboardList,
  FileSearch,
  Fingerprint,
  Route,
  ShieldCheck,
  Sparkles,
  WalletCards,
} from 'lucide-vue-next'

defineEmits(['start-application', 'view-tutorial', 'view-list'])

const metrics = [
  { label: '平均审批时长', value: '3 分钟', note: '同步流程演示', icon: markRaw(Activity) },
  { label: '协作节点', value: '5 个', note: '含交叉审查 Agent', icon: markRaw(BrainCircuit) },
  { label: '核心材料', value: '3 类', note: '身份证与银行流水', icon: markRaw(FileSearch) },
  { label: '审计覆盖', value: '100%', note: '状态与 Agent 日志', icon: markRaw(ShieldCheck) },
]

const agentStages = [
  { title: '材料采集 Agent', description: '完整性校验 · 模拟 OCR', icon: markRaw(FileSearch), iconBg: 'bg-blue-400/10', iconColor: 'text-blue-300' },
  { title: '反欺诈 Agent', description: '风险标签 · 规则命中', icon: markRaw(Fingerprint), iconBg: 'bg-rose-400/10', iconColor: 'text-rose-300' },
  { title: '偿债能力 Agent', description: 'DTI · 建议授信额度', icon: markRaw(WalletCards), iconBg: 'bg-amber-400/10', iconColor: 'text-amber-300' },
  { title: 'Approval Critic', description: '共享上下文 · 交叉复核', icon: markRaw(BrainCircuit), iconBg: 'bg-violet-400/10', iconColor: 'text-violet-300' },
]

const features = [
  {
    icon: markRaw(FileSearch),
    title: '材料闭环',
    description: '从材料选择到 OCR 状态，补件原因清楚展示，审批流程不会黑盒停滞。',
    bg: 'bg-blue-100',
    color: 'text-blue-600',
  },
  {
    icon: markRaw(BrainCircuit),
    title: 'Agent 协作',
    description: '多个专业 Agent 共享案件上下文，由审查 Agent 识别边界和结论冲突。',
    bg: 'bg-violet-100',
    color: 'text-violet-600',
  },
  {
    icon: markRaw(ShieldCheck),
    title: '规则兜底',
    description: '最终审批仍由可解释的规则决策，异常和边界结果自动进入人工复核。',
    bg: 'bg-emerald-100',
    color: 'text-emerald-600',
  },
]

const simpleSteps = [
  { title: '填写申请信息', description: '输入基本信息、贷款产品和工作情况。' },
  { title: '提交三类材料', description: '选择材料元数据，启动协作式审批链路。' },
  { title: '查看审批结果', description: '在状态页查看时间线、报告和当前步骤。' },
]
</script>
