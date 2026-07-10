<template>
  <div class="animate-fadeIn mx-auto max-w-6xl space-y-7">
    <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="eyebrow text-primary-600">Application tracking</p>
        <h1 class="mt-2 text-3xl font-semibold tracking-tight text-slate-950">审批状态查询</h1>
        <p class="mt-2 text-sm text-slate-500">实时查看申请状态、Agent 协作节点和审计时间线。</p>
      </div>
      <div class="flex flex-wrap gap-3">
        <button class="btn-secondary" :disabled="loading" @click="loadStatus">
          <RefreshCw class="h-4 w-4" :class="loading ? 'animate-spin' : ''" />
          刷新状态
        </button>
        <button class="btn-secondary" @click="$emit('view-list')">申请记录</button>
      </div>
    </div>

    <div v-if="loading" class="card flex items-center gap-3 text-slate-600">
      <Loader2 class="h-5 w-5 animate-spin text-primary-600" />
      正在读取后端审批状态...
    </div>

    <div v-else-if="errorMessage" class="rounded-3xl border border-red-100 bg-red-50 p-6 text-red-700">
      <p class="font-semibold">读取申请失败</p>
      <p class="mt-2 text-sm">{{ errorMessage }}</p>
    </div>

    <div v-else-if="!application" class="card py-16 text-center">
      <FileQuestion class="mx-auto mb-4 h-12 w-12 text-slate-300" />
      <h2 class="text-lg font-semibold text-slate-900">暂无可查询的申请</h2>
      <p class="mt-2 text-sm text-slate-500">请先提交一笔贷款申请，或在申请记录中选择一条记录查看。</p>
      <button class="btn-primary mt-6" @click="$emit('back')">返回首页</button>
    </div>

    <template v-else>
      <section class="overflow-hidden rounded-[2rem] bg-slate-950 p-6 text-white shadow-xl shadow-slate-900/10 sm:p-8">
        <div class="grid gap-8 lg:grid-cols-[1fr_0.9fr] lg:items-center">
          <div>
            <div class="flex flex-wrap items-center gap-3">
              <span class="status-badge" :class="getStatusClass(statusInfo?.status || application.status)">
                {{ statusInfo?.statusDesc || getStatusText(application.status) }}
              </span>
              <span class="text-xs text-slate-400">最后更新 {{ formatDate(statusInfo?.lastUpdatedAt || application.updatedAt) }}</span>
            </div>
            <p class="mt-6 text-xs uppercase tracking-[0.2em] text-slate-500">Application number</p>
            <h2 class="mt-2 break-all text-2xl font-semibold tracking-tight sm:text-3xl">{{ application.applicationNo }}</h2>
            <p class="mt-4 max-w-xl text-sm leading-6 text-slate-400">{{ application.currentStep || '系统正在准备下一步审批动作。' }}</p>
          </div>
          <div class="rounded-3xl border border-white/10 bg-white/[0.06] p-5">
            <div class="flex items-center justify-between">
              <p class="text-sm font-medium text-slate-300">流程完成度</p>
              <p class="text-2xl font-semibold text-cyan-300">{{ progressPercent }}%</p>
            </div>
            <div class="mt-4 h-2 overflow-hidden rounded-full bg-white/10">
              <div class="h-full rounded-full bg-gradient-to-r from-cyan-400 to-blue-500 transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
            </div>
            <div class="mt-5 grid grid-cols-4 gap-2 text-center text-[11px] text-slate-500">
              <span v-for="stage in progressStages" :key="stage.key" :class="currentStageIndex >= stage.index ? 'text-white' : ''">{{ stage.label }}</span>
            </div>
          </div>
        </div>
        <div class="mt-8 grid gap-3 border-t border-white/10 pt-6 sm:grid-cols-4">
          <div v-for="item in summaryItems" :key="item.label" class="rounded-2xl bg-white/[0.06] p-4">
            <p class="text-xs text-slate-500">{{ item.label }}</p>
            <p class="mt-2 truncate font-semibold text-white">{{ item.value }}</p>
          </div>
        </div>
      </section>

      <section class="grid gap-6 lg:grid-cols-[1fr_0.82fr]">
        <div class="card">
          <div class="flex items-center justify-between gap-4">
            <div>
              <p class="eyebrow">Audit trail</p>
              <h3 class="mt-2 text-xl font-semibold text-slate-950">状态时间线</h3>
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">{{ normalizedTimeline.length }} 个节点</span>
          </div>
          <div v-if="normalizedTimeline.length" class="mt-7 space-y-1">
            <div v-for="(event, index) in normalizedTimeline" :key="`${event.status}-${event.time}-${index}`" class="flex gap-4">
              <div class="flex flex-col items-center">
                <span :class="['flex h-9 w-9 items-center justify-center rounded-full', index === normalizedTimeline.length - 1 ? 'bg-slate-950 text-cyan-300' : 'bg-emerald-100 text-emerald-600']">
                  <CheckCircle2 class="h-4 w-4" />
                </span>
                <span v-if="index < normalizedTimeline.length - 1" class="my-1 min-h-8 w-px flex-1 bg-slate-200"></span>
              </div>
              <div class="pb-5 pt-1">
                <p class="font-semibold text-slate-900">{{ getStatusText(event.status) }}</p>
                <p class="mt-1 text-sm text-slate-500">{{ formatDate(event.time) }}</p>
              </div>
            </div>
          </div>
          <p v-else class="mt-7 text-sm text-slate-500">暂时没有状态流转记录。</p>
        </div>

        <div class="space-y-6">
          <div class="card">
            <div class="flex items-center gap-3">
              <span class="flex h-10 w-10 items-center justify-center rounded-2xl bg-violet-100 text-violet-600"><BrainCircuit class="h-5 w-5" /></span>
              <div>
                <p class="eyebrow">Agent collaboration</p>
                <h3 class="mt-1 text-lg font-semibold text-slate-950">协作节点</h3>
              </div>
            </div>
            <div class="mt-5 space-y-3">
              <div v-for="node in agentNodes" :key="node.title" class="flex items-center gap-3 rounded-2xl bg-slate-50 p-3">
                <span class="flex h-8 w-8 items-center justify-center rounded-xl" :class="node.bg"><component :is="node.icon" class="h-4 w-4" :class="node.color" /></span>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-semibold text-slate-800">{{ node.title }}</p>
                  <p class="mt-0.5 text-xs text-slate-500">{{ node.description }}</p>
                </div>
                <CheckCircle2 class="h-4 w-4 text-emerald-500" />
              </div>
            </div>
          </div>

          <div class="card">
            <div class="flex items-center justify-between gap-4">
              <div>
                <p class="eyebrow">Decision report</p>
                <h3 class="mt-2 text-lg font-semibold text-slate-950">审批报告</h3>
              </div>
              <FileText class="h-5 w-5 text-slate-300" />
            </div>
            <template v-if="report">
              <p class="mt-5 text-sm text-slate-500">报告类型：{{ report.reportType }}</p>
              <p class="mt-2 break-all rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-600">{{ report.reportUrl }}</p>
            </template>
            <p v-else class="mt-5 text-sm leading-6 text-slate-500">申请审批通过或拒绝后，后端会生成审批报告地址。</p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, ref } from 'vue'
import { BrainCircuit, CheckCircle2, FileQuestion, FileSearch, FileText, Fingerprint, Loader2, RefreshCw, WalletCards } from 'lucide-vue-next'
import { loanApi } from '../services/api'

const props = defineProps({
  applicationId: {
    type: [Number, String],
    default: null,
  },
})

defineEmits(['back', 'view-list'])

const loading = ref(false)
const errorMessage = ref('')
const application = ref(null)
const statusInfo = ref(null)
const report = ref(null)

const progressStages = [
  { key: 'SUBMITTED', label: '已提交', index: 0 },
  { key: 'OCR_PARSING', label: '材料', index: 1 },
  { key: 'RISK_ANALYZING', label: '分析', index: 2 },
  { key: 'APPROVED', label: '结果', index: 3 },
]

const agentNodes = [
  { title: 'DocumentIntakeAgent', description: '材料完整性与模拟 OCR', icon: markRaw(FileSearch), bg: 'bg-blue-100', color: 'text-blue-600' },
  { title: 'FraudRiskAgent', description: '反欺诈风险评估', icon: markRaw(Fingerprint), bg: 'bg-rose-100', color: 'text-rose-600' },
  { title: 'RepaymentCapacityAgent', description: 'DTI 与建议额度测算', icon: markRaw(WalletCards), bg: 'bg-amber-100', color: 'text-amber-600' },
  { title: 'ApprovalCriticAgent', description: '共享上下文交叉复核', icon: markRaw(BrainCircuit), bg: 'bg-violet-100', color: 'text-violet-600' },
]

const summaryItems = computed(() => {
  if (!application.value) return []
  return [
    { label: '申请人', value: application.value.applicantName || '-' },
    { label: '产品', value: getProductName(application.value.productCode) },
    { label: '金额', value: formatMoney(application.value.loanAmount) },
    { label: '期限', value: `${application.value.loanTerm || '-'} 个月` },
  ]
})

const normalizedTimeline = computed(() => statusInfo.value?.timeline || [])

const currentStageIndex = computed(() => {
  const status = statusInfo.value?.status || application.value?.status
  if (['APPROVED', 'REJECTED', 'ARCHIVED'].includes(status)) return 3
  if (['RISK_ANALYZING', 'DECISION_PENDING', 'DECISIONING', 'MANUAL_REVIEW'].includes(status)) return 2
  if (['OCR_PARSING', 'DOCUMENT_PENDING', 'MATERIAL_PENDING'].includes(status)) return 1
  return 0
})

const progressPercent = computed(() => Math.round((currentStageIndex.value / (progressStages.length - 1)) * 100))

onMounted(loadStatus)

async function loadStatus() {
  loading.value = true
  errorMessage.value = ''
  report.value = null

  try {
    const id = await resolveApplicationId()
    if (!id) {
      application.value = null
      statusInfo.value = null
      return
    }

    const [app, status] = await Promise.all([loanApi.getApplication(id), loanApi.getApplicationStatus(id)])
    application.value = app
    statusInfo.value = status

    if (['APPROVED', 'REJECTED'].includes(status.status)) {
      try {
        report.value = await loanApi.getReport(id)
      } catch {
        report.value = null
      }
    }
  } catch (error) {
    errorMessage.value = error.message || '读取申请状态失败。'
  } finally {
    loading.value = false
  }
}

async function resolveApplicationId() {
  if (props.applicationId) return Number(props.applicationId)
  const latest = localStorage.getItem('latestApplicationId')
  if (latest) return Number(latest)
  const list = await loanApi.listApplications()
  const sorted = [...list].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
  return sorted[0]?.applicationId || null
}

function getProductName(code) {
  const products = {
    CONSUMER_LOAN_STD: '个人消费贷标准版',
    CONSUMER_LOAN_EXP: '个人消费贷尊享版',
    HOUSEHOLD_LOAN: '家装贷款',
  }
  return products[code] || code || '-'
}

function getStatusText(status) {
  const map = {
    SUBMITTED: '已提交',
    DOCUMENT_PENDING: '等待补充材料',
    MATERIAL_PENDING: '等待补充材料',
    OCR_PARSING: '材料解析中',
    EXTERNAL_VERIFYING: '外部核验中',
    RISK_ANALYZING: '风险分析中',
    DECISION_PENDING: '等待决策',
    DECISIONING: '审批决策中',
    MANUAL_REVIEW: '人工复核中',
    APPROVED: '审批通过',
    REJECTED: '审批拒绝',
    ARCHIVED: '已归档',
  }
  return map[status] || status || '-'
}

function getStatusClass(status) {
  if (['APPROVED', 'ARCHIVED'].includes(status)) return 'status-approved'
  if (status === 'REJECTED') return 'status-rejected'
  if (status === 'MANUAL_REVIEW') return 'status-review'
  if (['DOCUMENT_PENDING', 'MATERIAL_PENDING'].includes(status)) return 'status-pending'
  if (['OCR_PARSING', 'EXTERNAL_VERIFYING', 'RISK_ANALYZING', 'DECISION_PENDING', 'DECISIONING'].includes(status)) return 'status-analyzing'
  return 'status-submitted'
}

function formatMoney(value) {
  return `${Number(value || 0).toLocaleString('zh-CN')} 元`
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}
</script>
