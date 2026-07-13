<template>
  <div class="animate-fadeIn space-y-6">
    <section class="overflow-hidden rounded-3xl bg-slate-950 p-7 text-white shadow-xl shadow-slate-900/10 sm:p-9">
      <div class="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div class="mb-5 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-semibold text-cyan-300">
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-400"></span>
            审批服务运行中
          </div>
          <p class="text-sm font-semibold text-slate-400">管理端 · 审批驾驶舱</p>
          <h1 class="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">聚焦风险，快速完成审批决策</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-400">
            查看全量申请状态、处理人工复核工单，并回溯 Agent 审批过程。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="inline-flex items-center gap-2 rounded-xl border border-white/15 bg-white/10 px-4 py-2.5 text-sm font-semibold transition hover:bg-white/15" @click="loadDashboard">
            <RefreshCw class="h-4 w-4" :class="loading ? 'animate-spin' : ''" />
            刷新数据
          </button>
          <button class="inline-flex items-center gap-2 rounded-xl bg-cyan-400 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300" @click="$emit('view-list')">
            <ClipboardList class="h-4 w-4" />
            全部申请
          </button>
        </div>
      </div>
    </section>

    <div v-if="errorMessage" class="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
      <CircleAlert class="mt-0.5 h-5 w-5 shrink-0" />
      <span>{{ errorMessage }}</span>
    </div>

    <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div v-for="item in metrics" :key="item.label" class="card group relative overflow-hidden">
        <div class="flex items-start justify-between">
          <div>
            <p class="text-sm font-medium text-slate-500">{{ item.label }}</p>
            <p class="mt-3 text-3xl font-bold tracking-tight text-slate-950">{{ item.value }}</p>
            <p class="mt-2 text-xs text-slate-400">{{ item.hint }}</p>
          </div>
          <span :class="['flex h-11 w-11 items-center justify-center rounded-2xl', item.iconClass]">
            <component :is="item.icon" class="h-5 w-5" />
          </span>
        </div>
      </div>
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
      <div class="card overflow-hidden p-0">
        <div class="flex items-center justify-between border-b border-slate-100 px-6 py-5">
          <div>
            <div class="flex items-center gap-2">
              <h2 class="text-lg font-bold text-slate-950">待人工复核</h2>
              <span class="rounded-full bg-orange-100 px-2.5 py-1 text-xs font-semibold text-orange-700">{{ pendingReviews.length }}</span>
            </div>
            <p class="mt-1 text-sm text-slate-500">优先处理需要人工判断的风险案件</p>
          </div>
          <ShieldAlert class="h-6 w-6 text-orange-500" />
        </div>

        <div v-if="loading && !pendingReviews.length" class="flex items-center gap-3 p-8 text-sm text-slate-500">
          <Loader2 class="h-5 w-5 animate-spin" />
          正在读取复核队列...
        </div>
        <div v-else-if="!pendingReviews.length" class="px-6 py-14 text-center">
          <span class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
            <BadgeCheck class="h-7 w-7" />
          </span>
          <h3 class="mt-4 font-semibold text-slate-900">当前没有待复核案件</h3>
          <p class="mt-2 text-sm text-slate-500">新工单进入队列后会显示在这里。</p>
        </div>
        <div v-else class="divide-y divide-slate-100">
          <button
            v-for="review in pendingReviews"
            :key="review.applicationId"
            class="flex w-full items-center gap-4 px-6 py-5 text-left transition hover:bg-slate-50"
            @click="openReview(review)"
          >
            <span :class="['flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl text-sm font-bold', getRiskClass(review.riskLevel)]">
              {{ getRiskShortName(review.riskLevel) }}
            </span>
            <span class="min-w-0 flex-1">
              <span class="flex flex-wrap items-center gap-x-3 gap-y-1">
                <span class="font-semibold text-slate-900">{{ review.applicantName }}</span>
                <span class="text-xs text-slate-400">{{ review.ticketNo }}</span>
              </span>
              <span class="mt-1.5 block truncate text-sm text-slate-500">
                {{ getProductName(review.productCode) }} · {{ formatDate(review.createdAt) }}
              </span>
            </span>
            <span class="hidden rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600 sm:inline-flex">
              {{ getRiskText(review.riskLevel) }}
            </span>
            <ChevronRight class="h-5 w-5 shrink-0 text-slate-300" />
          </button>
        </div>
      </div>

      <div class="card overflow-hidden p-0">
        <div class="flex items-center justify-between border-b border-slate-100 px-6 py-5">
          <div>
            <h2 class="text-lg font-bold text-slate-950">最近申请</h2>
            <p class="mt-1 text-sm text-slate-500">最新进入审批流程的客户申请</p>
          </div>
          <button class="text-sm font-semibold text-primary-600 hover:text-primary-700" @click="$emit('view-list')">查看全部</button>
        </div>

        <div v-if="loading && !recentApplications.length" class="flex items-center gap-3 p-8 text-sm text-slate-500">
          <Loader2 class="h-5 w-5 animate-spin" />
          正在读取申请数据...
        </div>
        <div v-else-if="!recentApplications.length" class="p-12 text-center text-sm text-slate-500">暂无申请数据</div>
        <div v-else class="divide-y divide-slate-100">
          <button
            v-for="item in recentApplications"
            :key="item.applicationId"
            class="flex w-full items-center gap-4 px-6 py-4 text-left transition hover:bg-slate-50"
            @click="$emit('view-application', item.applicationId)"
          >
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-50 font-semibold text-primary-700">
              {{ getInitial(item.applicantName) }}
            </span>
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-semibold text-slate-900">{{ item.applicantName }}</span>
              <span class="mt-1 block text-xs text-slate-400">{{ item.applicationNo }} · {{ formatMoney(item.loanAmount) }}</span>
            </span>
            <span :class="['status-badge shrink-0', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
          </button>
        </div>
      </div>
    </section>

    <div v-if="reviewPanelOpen" class="fixed inset-0 z-[70] flex items-end justify-center bg-slate-950/55 p-0 backdrop-blur-sm sm:items-center sm:p-6" @click.self="closeReview">
      <section class="max-h-[94vh] w-full max-w-5xl overflow-y-auto rounded-t-3xl bg-white shadow-2xl sm:rounded-3xl">
        <div class="sticky top-0 z-10 flex items-start justify-between border-b border-slate-100 bg-white/95 px-6 py-5 backdrop-blur sm:px-8">
          <div>
            <p class="text-sm font-semibold text-primary-600">人工复核工作台</p>
            <h2 class="mt-1 text-xl font-bold text-slate-950">
              {{ selectedReview?.applicantName || "案件详情" }}
              <span v-if="selectedReview?.ticketNo" class="ml-2 text-sm font-normal text-slate-400">{{ selectedReview.ticketNo }}</span>
            </h2>
          </div>
          <button class="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700" aria-label="关闭" @click="closeReview">
            <X class="h-5 w-5" />
          </button>
        </div>

        <div v-if="detailLoading" class="flex items-center justify-center gap-3 p-16 text-slate-500">
          <Loader2 class="h-6 w-6 animate-spin" />
          正在加载案件详情...
        </div>

        <div v-else-if="detailError" class="p-8">
          <div class="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">{{ detailError }}</div>
        </div>

        <div v-else-if="reviewDetail" class="grid gap-8 p-6 sm:p-8 lg:grid-cols-[1fr_0.9fr]">
          <div class="space-y-6">
            <div class="grid gap-3 sm:grid-cols-3">
              <div class="rounded-2xl bg-slate-50 p-4">
                <p class="text-xs text-slate-400">申请人</p>
                <p class="mt-2 font-semibold text-slate-900">{{ reviewDetail.applicantName }}</p>
              </div>
              <div class="rounded-2xl bg-slate-50 p-4">
                <p class="text-xs text-slate-400">贷款产品</p>
                <p class="mt-2 text-sm font-semibold text-slate-900">{{ getProductName(reviewDetail.productCode) }}</p>
              </div>
              <div class="rounded-2xl bg-slate-50 p-4">
                <p class="text-xs text-slate-400">风险等级</p>
                <p class="mt-2 font-semibold text-slate-900">{{ getRiskText(selectedReview.riskLevel) }}</p>
              </div>
            </div>

            <div>
              <h3 class="text-sm font-bold text-slate-900">风险摘要</h3>
              <p class="mt-3 rounded-2xl border border-orange-100 bg-orange-50/70 p-4 text-sm leading-7 text-slate-700">
                {{ reviewDetail.riskSummary || "暂无风险摘要，请结合审批时间线进行判断。" }}
              </p>
            </div>

            <div>
              <div class="mb-4 flex items-center justify-between">
                <h3 class="text-sm font-bold text-slate-900">审批审计时间线</h3>
                <span class="text-xs text-slate-400">{{ auditTimeline.length }} 条记录</span>
              </div>
              <div v-if="!auditTimeline.length" class="rounded-2xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-400">暂无审计记录</div>
              <div v-else class="space-y-4">
                <div v-for="event in auditTimeline" :key="`${event.eventType}-${event.occurredAt}`" class="relative flex gap-4 pl-1">
                  <div class="flex flex-col items-center">
                    <span class="mt-1 h-2.5 w-2.5 rounded-full bg-primary-500 ring-4 ring-primary-50"></span>
                    <span class="mt-2 h-full w-px bg-slate-100"></span>
                  </div>
                  <div class="min-w-0 pb-3">
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-sm font-semibold text-slate-800">{{ event.eventName || event.eventType }}</p>
                      <span class="text-xs text-slate-400">{{ formatDate(event.occurredAt) }}</span>
                    </div>
                    <p class="mt-1 text-sm leading-6 text-slate-500">{{ event.summary || "-" }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <form class="h-fit rounded-3xl border border-slate-200 bg-slate-50 p-5 sm:p-6" @submit.prevent="submitDecision">
            <div class="mb-5">
              <p class="text-xs font-semibold uppercase tracking-wider text-slate-400">Final decision</p>
              <h3 class="mt-1 text-lg font-bold text-slate-950">提交复核结论</h3>
            </div>

            <div class="mb-5 grid grid-cols-2 gap-3 rounded-2xl bg-white p-1.5 shadow-sm">
              <button type="button" :class="['rounded-xl px-4 py-3 text-sm font-semibold transition', decisionType === 'approve' ? 'bg-emerald-500 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-50']" @click="decisionType = 'approve'">
                <span class="inline-flex items-center gap-2"><CircleCheck class="h-4 w-4" />审批通过</span>
              </button>
              <button type="button" :class="['rounded-xl px-4 py-3 text-sm font-semibold transition', decisionType === 'reject' ? 'bg-red-500 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-50']" @click="decisionType = 'reject'">
                <span class="inline-flex items-center gap-2"><CircleX class="h-4 w-4" />审批拒绝</span>
              </button>
            </div>

            <div v-if="decisionType === 'approve'" class="grid gap-4 sm:grid-cols-2">
              <label class="block sm:col-span-2">
                <span class="form-label">批准金额（元）</span>
                <input v-model.number="decisionForm.approvedAmount" class="form-input" min="0" type="number" placeholder="留空则使用系统推荐金额" />
              </label>
              <label class="block">
                <span class="form-label">年利率（%）</span>
                <input v-model.number="decisionForm.interestRate" class="form-input" min="0" step="0.01" type="number" placeholder="10.80" />
              </label>
              <label class="block">
                <span class="form-label">贷款期限（月）</span>
                <input v-model.number="decisionForm.loanTerm" class="form-input" min="1" type="number" placeholder="原申请期限" />
              </label>
            </div>

            <label v-else class="block">
              <span class="form-label">拒绝原因代码</span>
              <select v-model="decisionForm.rejectReasonCode" class="form-input">
                <option value="MANUAL_RISK_REJECT">综合风险不符合要求</option>
                <option value="FRAUD_RISK_REJECT">存在欺诈风险</option>
                <option value="REPAYMENT_CAPACITY_LOW">偿债能力不足</option>
                <option value="DOCUMENT_INCONSISTENT">申请材料不一致</option>
              </select>
            </label>

            <label class="mt-4 block">
              <span class="form-label">复核意见 <span class="text-red-500">*</span></span>
              <textarea v-model.trim="decisionForm.reviewComment" class="form-input min-h-28 resize-y" maxlength="500" required placeholder="请说明判断依据和处理意见"></textarea>
              <span class="mt-1 block text-right text-xs text-slate-400">{{ decisionForm.reviewComment.length }}/500</span>
            </label>

            <div v-if="decisionError" class="mt-4 rounded-xl border border-red-100 bg-red-50 p-3 text-sm text-red-700">{{ decisionError }}</div>

            <button :class="['mt-5 inline-flex w-full items-center justify-center gap-2 rounded-xl px-5 py-3 font-semibold text-white transition disabled:bg-slate-300', decisionType === 'approve' ? 'bg-emerald-500 hover:bg-emerald-600' : 'bg-red-500 hover:bg-red-600']" :disabled="submittingDecision" type="submit">
              <Loader2 v-if="submittingDecision" class="h-5 w-5 animate-spin" />
              <Send v-else class="h-5 w-5" />
              {{ submittingDecision ? "正在提交..." : decisionType === "approve" ? "确认通过" : "确认拒绝" }}
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, reactive, ref } from "vue";
import {
  BadgeCheck,
  ChartNoAxesCombined,
  ChevronRight,
  CircleAlert,
  CircleCheck,
  CircleX,
  ClipboardList,
  Clock3,
  Landmark,
  Loader2,
  RefreshCw,
  Send,
  ShieldAlert,
  TrendingUp,
  X,
} from "lucide-vue-next";
import { adminApi, loanApi } from "../services/api";

defineEmits(["view-application", "view-list"]);

const applications = ref([]);
const pendingReviews = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const reviewPanelOpen = ref(false);
const selectedReview = ref(null);
const reviewDetail = ref(null);
const auditTimeline = ref([]);
const detailLoading = ref(false);
const detailError = ref("");
const submittingDecision = ref(false);
const decisionError = ref("");
const decisionType = ref("approve");
const decisionForm = reactive({
  approvedAmount: null,
  interestRate: null,
  loanTerm: null,
  reviewComment: "",
  rejectReasonCode: "MANUAL_RISK_REJECT",
});

const recentApplications = computed(() => applications.value.slice(0, 5));
const metrics = computed(() => {
  const total = applications.value.length;
  const approved = applications.value.filter((item) => item.status === "APPROVED").length;
  const rejected = applications.value.filter((item) => item.status === "REJECTED").length;
  const approvalRate = approved + rejected ? Math.round((approved / (approved + rejected)) * 100) : 0;

  return [
    { label: "申请总量", value: total, hint: "当前全部信贷申请", icon: markRaw(Landmark), iconClass: "bg-primary-50 text-primary-600" },
    { label: "待人工复核", value: pendingReviews.value.length, hint: "需要管理员处理", icon: markRaw(Clock3), iconClass: "bg-orange-50 text-orange-600" },
    { label: "已审批通过", value: approved, hint: `已拒绝 ${rejected} 笔`, icon: markRaw(TrendingUp), iconClass: "bg-emerald-50 text-emerald-600" },
    { label: "决策通过率", value: `${approvalRate}%`, hint: "已结束申请口径", icon: markRaw(ChartNoAxesCombined), iconClass: "bg-violet-50 text-violet-600" },
  ];
});

onMounted(loadDashboard);

async function loadDashboard() {
  loading.value = true;
  errorMessage.value = "";
  const [applicationResult, reviewResult] = await Promise.allSettled([
    loanApi.listApplications(),
    adminApi.listPendingReviews({ pageNo: 1, pageSize: 100 }),
  ]);

  if (applicationResult.status === "fulfilled") {
    applications.value = [...applicationResult.value].sort(
      (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0),
    );
  }
  if (reviewResult.status === "fulfilled") {
    pendingReviews.value = reviewResult.value || [];
  }

  const errors = [applicationResult, reviewResult]
    .filter((result) => result.status === "rejected")
    .map((result) => result.reason?.message)
    .filter(Boolean);
  if (errors.length) errorMessage.value = errors.join("；");
  loading.value = false;
}

async function openReview(review) {
  selectedReview.value = review;
  reviewPanelOpen.value = true;
  reviewDetail.value = null;
  auditTimeline.value = [];
  detailError.value = "";
  decisionError.value = "";
  decisionType.value = "approve";
  Object.assign(decisionForm, {
    approvedAmount: null,
    interestRate: null,
    loanTerm: null,
    reviewComment: "",
    rejectReasonCode: "MANUAL_RISK_REJECT",
  });

  detailLoading.value = true;
  const [detailResult, auditResult] = await Promise.allSettled([
    adminApi.getReviewDetail(review.applicationId),
    adminApi.getAuditTimeline(review.applicationId),
  ]);
  if (detailResult.status === "fulfilled") reviewDetail.value = detailResult.value;
  else detailError.value = detailResult.reason?.message || "读取复核详情失败。";

  if (auditResult.status === "fulfilled") {
    auditTimeline.value = auditResult.value?.timeline || [];
  }
  detailLoading.value = false;
}

function closeReview() {
  if (submittingDecision.value) return;
  reviewPanelOpen.value = false;
}

async function submitDecision() {
  decisionError.value = "";
  if (!decisionForm.reviewComment) {
    decisionError.value = "请填写复核意见。";
    return;
  }

  submittingDecision.value = true;
  try {
    const applicationId = selectedReview.value.applicationId;
    if (decisionType.value === "approve") {
      await adminApi.approveReview(applicationId, {
        approvedAmount: decisionForm.approvedAmount || undefined,
        interestRate: decisionForm.interestRate || undefined,
        loanTerm: decisionForm.loanTerm || undefined,
        reviewComment: decisionForm.reviewComment,
      });
    } else {
      await adminApi.rejectReview(applicationId, {
        reviewComment: decisionForm.reviewComment,
        rejectReasonCode: decisionForm.rejectReasonCode,
      });
    }
    reviewPanelOpen.value = false;
    await loadDashboard();
  } catch (error) {
    decisionError.value = error.message || "提交复核结论失败。";
  } finally {
    submittingDecision.value = false;
  }
}

function getInitial(name) {
  return Array.from(name || "客").slice(-1)[0];
}

function getProductName(code) {
  return { CONSUMER_LOAN_STD: "个人消费贷标准版", CONSUMER_LOAN_EXP: "个人消费贷尊享版", HOUSEHOLD_LOAN: "家装贷款" }[code] || code || "-";
}

function getRiskText(level) {
  return { LOW: "低风险", MEDIUM: "中风险", HIGH: "高风险", CRITICAL: "极高风险" }[level] || level || "待评估";
}

function getRiskShortName(level) {
  return { LOW: "低", MEDIUM: "中", HIGH: "高", CRITICAL: "极" }[level] || "待";
}

function getRiskClass(level) {
  if (["HIGH", "CRITICAL"].includes(level)) return "bg-red-50 text-red-600";
  if (level === "MEDIUM") return "bg-orange-50 text-orange-600";
  return "bg-emerald-50 text-emerald-600";
}

function getStatusText(status) {
  return { SUBMITTED: "已提交", DOCUMENT_PENDING: "待补件", MATERIAL_PENDING: "待补件", OCR_PARSING: "材料解析", EXTERNAL_VERIFYING: "外部核验", RISK_ANALYZING: "风险分析", DECISION_PENDING: "等待决策", DECISIONING: "审批中", MANUAL_REVIEW: "人工复核", APPROVED: "已通过", REJECTED: "已拒绝", ARCHIVED: "已归档" }[status] || status || "-";
}

function getStatusClass(status) {
  if (["APPROVED", "ARCHIVED"].includes(status)) return "status-approved";
  if (status === "REJECTED") return "status-rejected";
  if (status === "MANUAL_REVIEW") return "status-review";
  if (["DOCUMENT_PENDING", "MATERIAL_PENDING"].includes(status)) return "status-pending";
  if (["OCR_PARSING", "EXTERNAL_VERIFYING", "RISK_ANALYZING", "DECISION_PENDING", "DECISIONING"].includes(status)) return "status-analyzing";
  return "status-submitted";
}

function formatMoney(value) {
  return `${Number(value || 0).toLocaleString("zh-CN")} 元`;
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "-";
}
</script>
