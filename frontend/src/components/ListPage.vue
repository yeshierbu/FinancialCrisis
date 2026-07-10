<template>
  <div class="animate-fadeIn space-y-6">
    <div
      class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <p class="text-sm font-medium text-primary-600">申请记录</p>
        <h1 class="text-2xl font-bold text-gray-950">后端申请列表</h1>
      </div>
      <div class="flex gap-3">
        <button class="btn-secondary" @click="loadApplications">
          <RefreshCw class="h-5 w-5" :class="loading ? 'animate-spin' : ''" />
          刷新
        </button>
        <button class="btn-secondary" @click="$emit('back')">返回首页</button>
      </div>
    </div>

    <div class="card">
      <div class="grid gap-4 md:grid-cols-[1fr_220px]">
        <label class="relative block">
          <Search
            class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
          />
          <input
            v-model="searchKeyword"
            class="form-input form-input-icon-left"
            placeholder="搜索申请编号、申请人或产品"
          />
        </label>
        <select v-model="statusFilter" class="form-input">
          <option value="">全部状态</option>
          <option v-for="status in statusOptions" :key="status" :value="status">
            {{ getStatusText(status) }}
          </option>
        </select>
      </div>
    </div>

    <div class="grid gap-4 md:grid-cols-4">
      <div v-for="item in statistics" :key="item.label" class="card">
        <p class="text-2xl font-bold" :class="item.color">{{ item.value }}</p>
        <p class="mt-1 text-sm text-gray-500">{{ item.label }}</p>
      </div>
    </div>

    <div
      v-if="errorMessage"
      class="rounded-lg border border-red-100 bg-red-50 p-4 text-sm text-red-700"
    >
      {{ errorMessage }}
    </div>

    <div class="card overflow-hidden p-0">
      <div v-if="loading" class="flex items-center gap-3 p-6 text-gray-600">
        <Loader2 class="h-5 w-5 animate-spin" />
        正在从后端读取申请记录...
      </div>

      <div
        v-else-if="filteredApplications.length === 0"
        class="p-12 text-center"
      >
        <FileQuestion class="mx-auto mb-4 h-12 w-12 text-gray-300" />
        <h2 class="text-lg font-semibold text-gray-900">暂无申请记录</h2>
        <p class="mt-2 text-sm text-gray-500">
          内存数据库为空，请先发起一笔申请。
        </p>
      </div>

      <div v-else class="overflow-x-auto">
        <table class="w-full min-w-[860px] text-left text-sm">
          <thead class="bg-gray-50 text-xs uppercase text-gray-500">
            <tr>
              <th class="px-6 py-3 font-medium">申请编号</th>
              <th class="px-6 py-3 font-medium">申请人</th>
              <th class="px-6 py-3 font-medium">产品</th>
              <th class="px-6 py-3 font-medium">金额</th>
              <th class="px-6 py-3 font-medium">期限</th>
              <th class="px-6 py-3 font-medium">状态</th>
              <th class="px-6 py-3 font-medium">提交时间</th>
              <th class="px-6 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="item in pagedApplications"
              :key="item.applicationId"
              class="hover:bg-gray-50"
            >
              <td class="px-6 py-4 font-medium text-primary-700">
                {{ item.applicationNo }}
              </td>
              <td class="px-6 py-4 text-gray-800">{{ item.applicantName }}</td>
              <td class="px-6 py-4 text-gray-600">
                {{ getProductName(item.productCode) }}
              </td>
              <td class="px-6 py-4 text-gray-800">
                {{ formatMoney(item.loanAmount) }}
              </td>
              <td class="px-6 py-4 text-gray-600">{{ item.loanTerm }} 个月</td>
              <td class="px-6 py-4">
                <span :class="['status-badge', getStatusClass(item.status)]">{{
                  getStatusText(item.status)
                }}</span>
              </td>
              <td class="px-6 py-4 text-gray-500">
                {{ formatDate(item.createdAt) }}
              </td>
              <td class="px-6 py-4">
                <button
                  class="rounded-lg p-2 text-primary-600 hover:bg-primary-50"
                  title="查看详情"
                  @click="$emit('view-detail', item.applicationId)"
                >
                  <Eye class="h-5 w-5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-if="filteredApplications.length > 0"
        class="flex items-center justify-between border-t border-gray-100 px-6 py-4 text-sm text-gray-500"
      >
        <span>共 {{ filteredApplications.length }} 条记录</span>
        <div class="flex items-center gap-2">
          <button
            class="btn-secondary px-3 py-2"
            :disabled="currentPage === 1"
            @click="currentPage--"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <span class="px-2 font-medium text-gray-800"
            >{{ currentPage }} / {{ totalPages }}</span
          >
          <button
            class="btn-secondary px-3 py-2"
            :disabled="currentPage === totalPages"
            @click="currentPage++"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  FileQuestion,
  Loader2,
  RefreshCw,
  Search,
} from "lucide-vue-next";
import { loanApi } from "../services/api";

defineEmits(["back", "view-detail"]);

const applications = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const searchKeyword = ref("");
const statusFilter = ref("");
const currentPage = ref(1);
const pageSize = 10;

const statusOptions = [
  "SUBMITTED",
  "DOCUMENT_PENDING",
  "OCR_PARSING",
  "RISK_ANALYZING",
  "DECISION_PENDING",
  "MANUAL_REVIEW",
  "APPROVED",
  "REJECTED",
];

const filteredApplications = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  return applications.value.filter((item) => {
    const matchesKeyword =
      !keyword ||
      [item.applicationNo, item.applicantName, getProductName(item.productCode)]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword));
    const matchesStatus =
      !statusFilter.value || item.status === statusFilter.value;
    return matchesKeyword && matchesStatus;
  });
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredApplications.value.length / pageSize)),
);
const pagedApplications = computed(() =>
  filteredApplications.value.slice(
    (currentPage.value - 1) * pageSize,
    currentPage.value * pageSize,
  ),
);

const statistics = computed(() => [
  {
    label: "总申请数",
    value: applications.value.length,
    color: "text-primary-600",
  },
  {
    label: "审批通过",
    value: applications.value.filter((item) => item.status === "APPROVED")
      .length,
    color: "text-success-600",
  },
  {
    label: "人工复核",
    value: applications.value.filter((item) => item.status === "MANUAL_REVIEW")
      .length,
    color: "text-warning-600",
  },
  {
    label: "审批拒绝",
    value: applications.value.filter((item) => item.status === "REJECTED")
      .length,
    color: "text-danger-600",
  },
]);

watch([searchKeyword, statusFilter], () => {
  currentPage.value = 1;
});

onMounted(loadApplications);

async function loadApplications() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await loanApi.listApplications();
    applications.value = [...data].sort(
      (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0),
    );
  } catch (error) {
    errorMessage.value = error.message || "读取申请列表失败。";
  } finally {
    loading.value = false;
  }
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
  return `${Number(value || 0).toLocaleString("zh-CN")} 元`;
}

function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
</script>
