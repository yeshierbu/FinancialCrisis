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
            查看最终审批结果，并处理需要人工判断的复核工单。
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

    <section class="card overflow-hidden p-0">
      <div class="flex flex-col gap-4 border-b border-slate-100 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div class="flex items-center gap-2">
            <BookOpen class="h-5 w-5 text-primary-600" />
            <h2 class="text-lg font-bold text-slate-950">政策知识库</h2>
          </div>
          <p class="mt-1 text-sm text-slate-500">上传 PDF、DOCX、TXT 或 MD，系统自动切片并写入 Qdrant</p>
        </div>
        <span class="rounded-full bg-primary-50 px-3 py-1.5 text-xs font-semibold text-primary-700">
          {{ policyDocuments.length }} 个政策版本
        </span>
      </div>

      <div class="grid gap-6 p-6 lg:grid-cols-[1fr_0.9fr]">
        <form class="grid gap-4 sm:grid-cols-2" @submit.prevent="uploadPolicy">
          <label class="block sm:col-span-2">
            <span class="form-label">政策文件 <span class="text-red-500">*</span></span>
            <input ref="policyFileInput" class="form-input file:mr-4 file:rounded-lg file:border-0 file:bg-primary-50 file:px-3 file:py-2 file:font-semibold file:text-primary-700" type="file" accept=".pdf,.docx,.txt,.md" required @change="selectPolicyFile" />
          </label>
          <label class="block">
            <span class="form-label">政策编号</span>
            <input v-model.trim="policyForm.documentId" class="form-input" required placeholder="POLICY-CONSUMER-001" />
          </label>
          <label class="block">
            <span class="form-label">版本</span>
            <input v-model.trim="policyForm.version" class="form-input" required placeholder="1.0" />
          </label>
          <label class="block sm:col-span-2">
            <span class="form-label">政策标题</span>
            <input v-model.trim="policyForm.title" class="form-input" required placeholder="消费贷准入政策" />
          </label>
          <label class="block">
            <span class="form-label">适用产品</span>
            <select v-model="policyForm.productCode" class="form-input" required>
              <option value="CONSUMER_LOAN_STD">个人消费贷标准版</option>
              <option value="CONSUMER_LOAN_EXP">个人消费贷尊享版</option>
              <option value="HOUSEHOLD_LOAN">家装贷款</option>
            </select>
          </label>
          <label class="block">
            <span class="form-label">生效日期</span>
            <input v-model="policyForm.effectiveFrom" class="form-input" type="date" required />
          </label>
          <label class="block">
            <span class="form-label">失效日期（可选）</span>
            <input v-model="policyForm.effectiveTo" class="form-input" type="date" />
          </label>
          <div class="flex items-end">
            <button class="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 px-5 py-3 font-semibold text-white transition hover:bg-primary-700 disabled:bg-slate-300" type="submit" :disabled="policyUploading">
              <Loader2 v-if="policyUploading" class="h-5 w-5 animate-spin" />
              <UploadCloud v-else class="h-5 w-5" />
              {{ policyUploading ? "正在解析并向量化..." : "上传并同步知识库" }}
            </button>
          </div>
          <p v-if="policyMessage" :class="['sm:col-span-2 rounded-xl p-3 text-sm', policyMessageType === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700']">
            {{ policyMessage }}
          </p>
        </form>

        <div class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
          <h3 class="text-sm font-bold text-slate-900">最近导入</h3>
          <div v-if="!policyDocuments.length" class="py-10 text-center text-sm text-slate-400">尚未导入政策文件</div>
          <div v-else class="mt-3 max-h-80 space-y-2 overflow-y-auto">
            <div v-for="policy in policyDocuments.slice(0, 8)" :key="`${policy.documentId}-${policy.version}`" class="rounded-xl bg-white p-3 shadow-sm">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="truncate text-sm font-semibold text-slate-900">{{ policy.title }}</p>
                  <p class="mt-1 text-xs text-slate-400">{{ policy.documentId }} · v{{ policy.version }} · {{ getProductName(policy.productCode) }}</p>
                </div>
                <span :class="['shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold', policy.vectorSyncStatus === 'SYNCED' ? 'bg-emerald-100 text-emerald-700' : policy.vectorSyncStatus === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700']">
                  {{ policy.vectorSyncStatus === 'SYNCED' ? '已同步' : policy.vectorSyncStatus === 'FAILED' ? '失败' : '处理中' }}
                </span>
              </div>
            </div>
          </div>
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
            <span class="inline-flex shrink-0 items-center gap-1 text-sm font-semibold text-primary-600">
              查看详情 <ChevronRight class="h-4 w-4" />
            </span>
          </button>
        </div>
      </div>

      <div class="card overflow-hidden p-0">
        <div class="flex items-center justify-between border-b border-slate-100 px-6 py-5">
          <div>
            <h2 class="text-lg font-bold text-slate-950">最近审批结果</h2>
            <p class="mt-1 text-sm text-slate-500">仅展示已经完成的最终审批结论</p>
          </div>
          <button class="text-sm font-semibold text-primary-600 hover:text-primary-700" @click="$emit('view-list')">查看全部</button>
        </div>

        <div v-if="loading && !recentApplications.length" class="flex items-center gap-3 p-8 text-sm text-slate-500">
          <Loader2 class="h-5 w-5 animate-spin" />
          正在读取申请数据...
        </div>
        <div v-else-if="!recentApplications.length" class="p-12 text-center text-sm text-slate-500">暂无最终审批结果</div>
        <div v-else class="divide-y divide-slate-100">
          <div
            v-for="item in recentApplications"
            :key="item.applicationId"
            class="flex w-full items-center gap-4 px-6 py-4 text-left"
          >
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-50 font-semibold text-primary-700">
              {{ getInitial(item.applicantName) }}
            </span>
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-semibold text-slate-900">{{ item.applicantName }}</span>
              <span class="mt-1 block text-xs text-slate-400">{{ item.applicationNo }} · {{ formatMoney(item.loanAmount) }}</span>
            </span>
            <span :class="['status-badge shrink-0', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
          </div>
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
              <div class="mb-3 flex items-center justify-between">
                <h3 class="text-sm font-bold text-slate-900">申请信息</h3>
                <span class="text-xs text-slate-400">{{ reviewDetail.applicationNo }}</span>
              </div>
              <dl class="grid gap-x-5 gap-y-4 rounded-2xl border border-slate-100 bg-white p-5 sm:grid-cols-2">
                <div>
                  <dt class="text-xs text-slate-400">申请金额</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ formatMoney(reviewDetail.loanAmount) }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-slate-400">申请期限</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ reviewDetail.loanTerm || "-" }} 个月</dd>
                </div>
                <div>
                  <dt class="text-xs text-slate-400">身份证号</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ maskIdCard(reviewDetail.idCardNo) }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-slate-400">手机号</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ maskMobile(reviewDetail.mobile) }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-slate-400">就业类型</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ getEmploymentText(reviewDetail.employmentType) }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-slate-400">工作单位</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ reviewDetail.companyName || "-" }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-slate-400">工作年限</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ reviewDetail.workYears == null ? "-" : `${reviewDetail.workYears} 年` }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-slate-400">申请时间</dt>
                  <dd class="mt-1 text-sm font-semibold text-slate-900">{{ formatDate(reviewDetail.appliedAt) }}</dd>
                </div>
              </dl>
            </div>

            <div>
              <h3 class="text-sm font-bold text-slate-900">风险与偿债能力</h3>
              <div class="mt-3 grid gap-3 sm:grid-cols-2">
                <div class="rounded-2xl bg-slate-50 p-4">
                  <p class="text-xs text-slate-400">风险分数</p>
                  <p class="mt-2 font-semibold text-slate-900">{{ formatMetric(reviewDetail.riskScore) }}</p>
                </div>
                <div class="rounded-2xl bg-slate-50 p-4">
                  <p class="text-xs text-slate-400">稳定月收入</p>
                  <p class="mt-2 font-semibold text-slate-900">{{ formatMoneyOrDash(reviewDetail.stableMonthlyIncome) }}</p>
                </div>
                <div class="rounded-2xl bg-slate-50 p-4">
                  <p class="text-xs text-slate-400">月债务支出</p>
                  <p class="mt-2 font-semibold text-slate-900">{{ formatMoneyOrDash(reviewDetail.monthlyDebtPayment) }}</p>
                </div>
                <div class="rounded-2xl bg-slate-50 p-4">
                  <p class="text-xs text-slate-400">建议授信额度</p>
                  <p class="mt-2 font-semibold text-slate-900">{{ formatMoneyOrDash(reviewDetail.recommendedCreditLimit) }}</p>
                </div>
              </div>
            </div>

            <div>
              <h3 class="text-sm font-bold text-slate-900">风险摘要</h3>
              <p class="mt-3 rounded-2xl border border-orange-100 bg-orange-50/70 p-4 text-sm leading-7 text-slate-700">
                {{ reviewDetail.riskSummary || "暂无风险摘要，请根据申请材料做出最终判断。" }}
              </p>
            </div>

            <div class="rounded-2xl border border-primary-100 bg-primary-50/60 p-5">
              <p class="text-xs font-semibold uppercase tracking-wider text-primary-500">Final result</p>
              <h3 class="mt-2 text-base font-bold text-slate-950">待人工确定最终结果</h3>
              <p class="mt-2 text-sm leading-6 text-slate-600">系统已完成自动风险分析，本案件不展示中间状态变化。请在右侧直接提交通过或拒绝的最终结论。
              </p>
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
  BookOpen,
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
  UploadCloud,
  X,
} from "lucide-vue-next";
import { adminApi, loanApi } from "../services/api";

defineEmits(["view-list"]);

const applications = ref([]);
const pendingReviews = ref([]);
const policyDocuments = ref([]);
const policyFile = ref(null);
const policyFileInput = ref(null);
const policyUploading = ref(false);
const policyMessage = ref("");
const policyMessageType = ref("success");
const policyForm = reactive({
  documentId: "",
  title: "",
  version: "1.0",
  productCode: "CONSUMER_LOAN_STD",
  effectiveFrom: new Date().toISOString().slice(0, 10),
  effectiveTo: "",
});
const loading = ref(false);
const errorMessage = ref("");
const reviewPanelOpen = ref(false);
const selectedReview = ref(null);
const reviewDetail = ref(null);
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

const recentApplications = computed(() => applications.value
  .filter((item) => ["APPROVED", "REJECTED", "ARCHIVED"].includes(item.status))
  .slice(0, 5));
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
  const [applicationResult, reviewResult, policyResult] = await Promise.allSettled([
    loanApi.listApplications(),
    adminApi.listPendingReviews({ pageNo: 1, pageSize: 100 }),
    adminApi.listPolicyDocuments(),
  ]);

  if (applicationResult.status === "fulfilled") {
    applications.value = [...applicationResult.value].sort(
      (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0),
    );
  }
  if (reviewResult.status === "fulfilled") {
    pendingReviews.value = reviewResult.value || [];
  }
  if (policyResult.status === "fulfilled") policyDocuments.value = policyResult.value || [];

  const errors = [applicationResult, reviewResult, policyResult]
    .filter((result) => result.status === "rejected")
    .map((result) => result.reason?.message)
    .filter(Boolean);
  if (errors.length) errorMessage.value = errors.join("；");
  loading.value = false;
}

function selectPolicyFile(event) {
  policyFile.value = event.target.files?.[0] || null;
  if (policyFile.value && !policyForm.title) {
    policyForm.title = policyFile.value.name.replace(/\.[^.]+$/, "");
  }
}

async function uploadPolicy() {
  policyMessage.value = "";
  if (!policyFile.value) {
    policyMessageType.value = "error";
    policyMessage.value = "请选择政策文件。";
    return;
  }
  policyUploading.value = true;
  try {
    const result = await adminApi.uploadPolicyDocument(policyForm, policyFile.value);
    policyMessageType.value = "success";
    policyMessage.value = `导入成功：${result.chunkCount} 个分片已同步到 Qdrant。`;
    policyFile.value = null;
    if (policyFileInput.value) policyFileInput.value.value = "";
    await loadDashboard();
  } catch (error) {
    policyMessageType.value = "error";
    policyMessage.value = error.message || "政策导入失败。";
  } finally {
    policyUploading.value = false;
  }
}

async function openReview(review) {
  selectedReview.value = review;
  reviewPanelOpen.value = true;
  reviewDetail.value = null;
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
  try {
    reviewDetail.value = await adminApi.getReviewDetail(review.applicationId);
  } catch (error) {
    detailError.value = error?.message || "读取复核详情失败。";
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

function getEmploymentText(value) {
  return { SALARIED: "受薪人士", SELF_EMPLOYED: "自营职业", FREELANCER: "自由职业", RETIRED: "退休" }[value] || value || "-";
}

function maskIdCard(value) {
  if (!value || value.length < 8) return value || "-";
  return `${value.slice(0, 4)}**********${value.slice(-4)}`;
}

function maskMobile(value) {
  if (!value || value.length < 7) return value || "-";
  return `${value.slice(0, 3)}****${value.slice(-4)}`;
}

function formatMetric(value) {
  return value == null ? "-" : Number(value).toLocaleString("zh-CN", { maximumFractionDigits: 2 });
}

function formatMoneyOrDash(value) {
  return value == null ? "-" : formatMoney(value);
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
