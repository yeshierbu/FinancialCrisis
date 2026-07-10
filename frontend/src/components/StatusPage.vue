<template>
  <div class="animate-fadeIn mx-auto max-w-5xl space-y-6">
    <div
      class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <p class="text-sm font-medium text-primary-600">申请进度</p>
        <h1 class="text-2xl font-bold text-gray-950">审批状态查询</h1>
      </div>
      <div class="flex gap-3">
        <button class="btn-secondary" @click="$emit('view-list')">
          申请记录
        </button>
        <button class="btn-secondary" @click="$emit('back')">返回首页</button>
      </div>
    </div>

    <div v-if="loading" class="card flex items-center gap-3 text-gray-600">
      <Loader2 class="h-5 w-5 animate-spin" />
      正在读取后端审批状态...
    </div>

    <div
      v-else-if="errorMessage"
      class="card border-red-100 bg-red-50 text-red-700"
    >
      {{ errorMessage }}
    </div>

    <div v-else-if="!application" class="card text-center">
      <FileQuestion class="mx-auto mb-4 h-12 w-12 text-gray-300" />
      <h2 class="text-lg font-semibold text-gray-900">暂无可查询的申请</h2>
      <p class="mt-2 text-sm text-gray-500">
        请先提交一笔贷款申请，或在申请记录中选择一条记录查看。
      </p>
      <button class="btn-primary mt-6" @click="$emit('back')">返回首页</button>
    </div>

    <template v-else>
      <section class="card">
        <div
          class="flex flex-col gap-5 md:flex-row md:items-start md:justify-between"
        >
          <div>
            <p class="text-sm text-gray-500">申请编号</p>
            <h2 class="mt-1 text-2xl font-bold text-gray-950">
              {{ application.applicationNo }}
            </h2>
            <p class="mt-2 text-sm text-gray-500">
              最后更新：{{
                formatDate(statusInfo?.lastUpdatedAt || application.updatedAt)
              }}
            </p>
          </div>
          <span
            :class="[
              'status-badge',
              getStatusClass(statusInfo?.status || application.status),
            ]"
          >
            {{ statusInfo?.statusDesc || getStatusText(application.status) }}
          </span>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-4">
          <div
            v-for="item in summaryItems"
            :key="item.label"
            class="rounded-lg bg-gray-50 p-4"
          >
            <p class="text-sm text-gray-500">{{ item.label }}</p>
            <p class="mt-2 font-semibold text-gray-900">{{ item.value }}</p>
          </div>
        </div>
      </section>

      <section class="grid gap-6 lg:grid-cols-[1fr_0.85fr]">
        <div class="card">
          <div
            class="flex items-center justify-between cursor-pointer"
            @click="timelineExpanded = !timelineExpanded"
          >
            <h3 class="mb-5 text-lg font-semibold text-gray-900">状态时间线</h3>
            <ChevronDown
              class="h-5 w-5 text-gray-400 transition-transform"
              :class="{ 'rotate-180': timelineExpanded }"
            />
          </div>

          <div
            v-if="!timelineExpanded && normalizedTimeline.length > 0"
            class="flex gap-4"
          >
            <div class="flex flex-col items-center">
              <span
                class="flex h-9 w-9 items-center justify-center rounded-full bg-primary-600 text-white"
              >
                <CheckCircle2 class="h-5 w-5" />
              </span>
            </div>
            <div>
              <p class="font-medium text-gray-900">
                {{
                  getStatusText(
                    normalizedTimeline[normalizedTimeline.length - 1].status,
                  )
                }}
              </p>
              <p class="mt-1 text-sm text-gray-500">
                {{
                  formatDate(
                    normalizedTimeline[normalizedTimeline.length - 1].time,
                  )
                }}
              </p>
            </div>
          </div>

          <div v-else class="space-y-5">
            <div
              v-for="(event, index) in normalizedTimeline"
              :key="`${event.status}-${event.time}-${index}`"
              class="flex gap-4"
            >
              <div class="flex flex-col items-center">
                <span
                  :class="[
                    'flex h-9 w-9 items-center justify-center rounded-full',
                    index === normalizedTimeline.length - 1
                      ? 'bg-primary-600 text-white'
                      : 'bg-success-500 text-white',
                  ]"
                >
                  <CheckCircle2 class="h-5 w-5" />
                </span>
                <span
                  v-if="index < normalizedTimeline.length - 1"
                  class="mt-2 h-full min-h-8 w-px bg-gray-200"
                ></span>
              </div>
              <div class="pb-2">
                <p class="font-medium text-gray-900">
                  {{ getStatusText(event.status) }}
                </p>
                <p class="mt-1 text-sm text-gray-500">
                  {{ formatDate(event.time) }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="space-y-6">
          <div class="card">
            <h3 class="mb-4 text-lg font-semibold text-gray-900">当前步骤</h3>
            <p class="text-sm leading-6 text-gray-600">
              {{ application.currentStep || "暂无步骤说明" }}
            </p>
          </div>

          <div class="card">
            <h3 class="mb-4 text-lg font-semibold text-gray-900">审批报告</h3>
            <template v-if="report">
              <p class="text-sm text-gray-500">
                报告类型：{{ report.reportType }}
              </p>
              <p class="mt-2 break-all text-sm text-gray-600">
                {{ report.reportUrl }}
              </p>
            </template>
            <p v-else class="text-sm leading-6 text-gray-600">
              申请审批通过或拒绝后，后端会生成审批报告地址。
            </p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import {
  CheckCircle2,
  ChevronDown,
  FileQuestion,
  Loader2,
} from "lucide-vue-next";
import { loanApi } from "../services/api";

const props = defineProps({
  applicationId: {
    type: [Number, String],
    default: null,
  },
});

defineEmits(["back", "view-list"]);

const loading = ref(false);
const errorMessage = ref("");
const application = ref(null);
const statusInfo = ref(null);
const report = ref(null);
const timelineExpanded = ref(false);

const summaryItems = computed(() => {
  if (!application.value) return [];
  return [
    { label: "申请人", value: application.value.applicantName || "-" },
    { label: "产品", value: getProductName(application.value.productCode) },
    { label: "金额", value: formatMoney(application.value.loanAmount) },
    { label: "期限", value: `${application.value.loanTerm || "-"} 个月` },
  ];
});

const normalizedTimeline = computed(() => statusInfo.value?.timeline || []);

onMounted(loadStatus);

async function loadStatus() {
  loading.value = true;
  errorMessage.value = "";
  report.value = null;

  try {
    const id = await resolveApplicationId();
    if (!id) {
      application.value = null;
      statusInfo.value = null;
      return;
    }

    const [app, status] = await Promise.all([
      loanApi.getApplication(id),
      loanApi.getApplicationStatus(id),
    ]);
    application.value = app;
    statusInfo.value = status;

    if (["APPROVED", "REJECTED"].includes(status.status)) {
      try {
        report.value = await loanApi.getReport(id);
      } catch {
        report.value = null;
      }
    }
  } catch (error) {
    errorMessage.value = error.message || "读取申请状态失败。";
  } finally {
    loading.value = false;
  }
}

async function resolveApplicationId() {
  if (props.applicationId) return Number(props.applicationId);

  const latest = localStorage.getItem("latestApplicationId");
  if (latest) return Number(latest);

  const list = await loanApi.listApplications();
  const sorted = [...list].sort(
    (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0),
  );
  return sorted[0]?.applicationId || null;
}

function getProductName(code) {
  const products = {
    CONSUMER_LOAN_STD: "个人消费贷标准版",
    CONSUMER_LOAN_EXP: "个人消费贷尊享版",
    HOUSEHOLD_LOAN: "家装贷款",
  };
  return products[code] || code || "-";
}

function getStatusText(status) {
  const map = {
    SUBMITTED: "已提交",
    DOCUMENT_PENDING: "等待补充材料",
    MATERIAL_PENDING: "等待补充材料",
    OCR_PARSING: "材料解析中",
    EXTERNAL_VERIFYING: "外部核验中",
    RISK_ANALYZING: "风险分析中",
    DECISION_PENDING: "等待决策",
    DECISIONING: "审批决策中",
    MANUAL_REVIEW: "人工复核中",
    APPROVED: "审批通过",
    REJECTED: "审批拒绝",
    ARCHIVED: "已归档",
  };
  return map[status] || status || "-";
}

function getStatusClass(status) {
  if (["APPROVED", "ARCHIVED"].includes(status)) return "status-approved";
  if (status === "REJECTED") return "status-rejected";
  if (status === "MANUAL_REVIEW") return "status-review";
  if (["DOCUMENT_PENDING", "MATERIAL_PENDING"].includes(status))
    return "status-pending";
  if (
    [
      "OCR_PARSING",
      "EXTERNAL_VERIFYING",
      "RISK_ANALYZING",
      "DECISION_PENDING",
      "DECISIONING",
    ].includes(status)
  )
    return "status-analyzing";
  return "status-submitted";
}

function formatMoney(value) {
  const amount = Number(value || 0);
  return `${amount.toLocaleString("zh-CN")} 元`;
}

function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
</script>
