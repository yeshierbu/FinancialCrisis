<template>
  <div class="animate-fadeIn mx-auto max-w-4xl space-y-6">
    <div
      class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <p class="text-sm font-medium text-primary-600">贷款申请</p>
        <h1 class="text-2xl font-bold text-gray-950">填写申请信息</h1>
      </div>
      <button class="btn-secondary" @click="$emit('back')">返回首页</button>
    </div>

    <div class="grid gap-3 md:grid-cols-3">
      <div
        v-for="(step, index) in steps"
        :key="step"
        class="flex items-center gap-3 rounded-lg border border-gray-100 bg-white p-4"
      >
        <span
          :class="[
            'step-indicator',
            currentStep >= index ? 'step-active' : 'step-inactive',
          ]"
        >
          <CheckCircle v-if="currentStep > index" class="h-5 w-5" />
          <span v-else>{{ index + 1 }}</span>
        </span>
        <span class="text-sm font-medium text-gray-700">{{ step }}</span>
      </div>
    </div>

    <form class="card space-y-6" @submit.prevent="handlePrimaryAction">
      <div v-if="currentStep === 0" class="space-y-6">
        <h2 class="text-lg font-semibold text-gray-900">基本信息</h2>
        <div class="grid gap-5 md:grid-cols-2">
          <label class="block">
            <span class="form-label">姓名</span>
            <input
              v-model.trim="form.applicantName"
              class="form-input"
              maxlength="8"
              required
              placeholder="请输入申请人姓名（最多8个汉字）"
              @input="limitApplicantName"
            />
            <p v-if="validationErrors.name" class="mt-1 text-xs text-red-600">
              {{ validationErrors.name }}
            </p>
          </label>
          <label class="block">
            <span class="form-label">身份证号</span>
            <input
              v-model.trim="form.idCardNo"
              class="form-input"
              required
              placeholder="请输入身份证号"
            />
            <p v-if="validationErrors.idCard" class="mt-1 text-xs text-red-600">
              {{ validationErrors.idCard }}
            </p>
          </label>
          <label class="block">
            <span class="form-label">手机号</span>
            <input
              v-model.trim="form.mobile"
              class="form-input"
              required
              placeholder="请输入手机号"
            />
            <p v-if="validationErrors.mobile" class="mt-1 text-xs text-red-600">
              {{ validationErrors.mobile }}
            </p>
          </label>
          <label class="block">
            <span class="form-label">贷款产品</span>
            <select v-model="form.productCode" class="form-input" required>
              <option value="">请选择产品</option>
              <option value="CONSUMER_LOAN_STD">个人消费贷标准版</option>
              <option value="CONSUMER_LOAN_EXP">个人消费贷尊享版</option>
              <option value="HOUSEHOLD_LOAN">家装贷款</option>
            </select>
          </label>
          <label class="block">
            <span class="form-label">申请金额</span>
            <input
              v-model.number="form.loanAmount"
              class="form-input"
              min="1"
              required
              type="number"
              placeholder="最低 1000 元"
            />
            <p v-if="validationErrors.amount" class="mt-1 text-xs text-red-600">
              {{ validationErrors.amount }}
            </p>
          </label>
          <label class="block">
            <span class="form-label">贷款期限</span>
            <select v-model.number="form.loanTerm" class="form-input" required>
              <option :value="0">请选择期限</option>
              <option v-for="term in loanTerms" :key="term" :value="term">
                {{ term }} 个月
              </option>
            </select>
          </label>
        </div>
      </div>

      <div v-else-if="currentStep === 1" class="space-y-6">
        <h2 class="text-lg font-semibold text-gray-900">工作信息</h2>
        <div class="grid gap-5 md:grid-cols-2">
          <label class="block">
            <span class="form-label">就业类型</span>
            <select v-model="form.employmentType" class="form-input" required>
              <option value="">请选择就业类型</option>
              <option value="FULL_TIME">全职</option>
              <option value="PART_TIME">兼职</option>
              <option value="SELF_EMPLOYED">自雇</option>
              <option value="RETIRED">退休</option>
            </select>
          </label>
          <label class="block">
            <span class="form-label">工作年限</span>
            <select v-model.number="form.workYears" class="form-input" required>
              <option :value="0">请选择工作年限</option>
              <option :value="1">1 年以内</option>
              <option :value="3">1-3 年</option>
              <option :value="5">3-5 年</option>
              <option :value="10">5-10 年</option>
              <option :value="20">10 年以上</option>
            </select>
          </label>
          <label class="block">
            <span class="form-label">公司名称</span>
            <input
              v-model.trim="form.companyName"
              class="form-input"
              placeholder="请输入公司名称"
            />
          </label>
          <label class="block">
            <span class="form-label">月收入</span>
            <input
              v-model.number="monthlyIncome"
              class="form-input"
              min="0"
              type="number"
              placeholder="用于前端展示，不提交后端"
            />
          </label>
        </div>
      </div>

      <div v-else class="space-y-6">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">上传材料</h2>
            <p class="mt-1 text-sm text-gray-500">
              图片将发送至阿里云百炼 Qwen OCR 识别，原图不会保存在本地数据库。
            </p>
          </div>
          <span
            class="status-badge"
            :class="allDocumentsSelected ? 'status-approved' : 'status-pending'"
          >
            {{ selectedDocumentCount }}/{{ documents.length }} 已选择
          </span>
        </div>

        <div class="grid gap-4 md:grid-cols-3">
          <div
            v-for="doc in documents"
            :key="doc.type"
            class="rounded-lg border border-dashed border-gray-200 p-5"
          >
            <div
              class="mb-4 flex h-11 w-11 items-center justify-center rounded-lg bg-gray-100 text-gray-500"
            >
              <component :is="doc.icon" class="h-6 w-6" />
            </div>
            <h3 class="font-semibold text-gray-900">{{ doc.name }}</h3>
            <p class="mt-1 text-sm text-gray-500">{{ doc.description }}</p>
            <p
              v-if="doc.file"
              class="mt-3 truncate text-sm font-medium text-success-600"
            >
              {{ doc.file.name }}
            </p>
            <label class="btn-secondary mt-4 w-full cursor-pointer">
              <input
                class="hidden"
                type="file"
                :accept="doc.accept"
                @change="handleFileChange(doc.type, $event)"
              />
              <Upload class="h-4 w-4" />
              选择文件
            </label>
          </div>
        </div>

        <label
          class="flex items-start gap-3 rounded-lg bg-gray-50 p-4 text-sm text-gray-600"
        >
          <input v-model="agreed" class="mt-1" type="checkbox" />
          <span
            >我确认提交的信息真实有效，并同意用于本次智能信贷审批演示。</span
          >
        </label>
      </div>

      <div
        v-if="errorMessage"
        class="rounded-lg border border-red-100 bg-red-50 p-4 text-sm text-red-700"
      >
        {{ errorMessage }}
      </div>

      <div class="flex justify-between border-t border-gray-100 pt-6">
        <button
          type="button"
          class="btn-secondary"
          :disabled="currentStep === 0 || submitting"
          @click="currentStep--"
        >
          上一步
        </button>
        <button
          type="submit"
          class="btn-primary"
          :disabled="submitting || !canContinue"
        >
          <Loader2 v-if="submitting" class="h-5 w-5 animate-spin" />
          <span>{{ primaryButtonText }}</span>
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, markRaw, reactive, ref } from "vue";
import {
  CheckCircle,
  FileText,
  IdCard,
  Loader2,
  Upload,
  WalletCards,
} from "lucide-vue-next";
import { loanApi } from "../services/api";

const emit = defineEmits(["back", "submitted"]);

const currentStep = ref(0);
const submitting = ref(false);
const agreed = ref(false);
const errorMessage = ref("");
const monthlyIncome = ref(null);
const validationErrors = reactive({
  name: "",
  idCard: "",
  mobile: "",
  amount: "",
});

const steps = ["基本信息", "工作信息", "申请材料"];
const loanTerms = [6, 12, 24, 36, 48, 60];

const form = reactive({
  applicantName: "",
  idCardNo: "",
  mobile: "",
  productCode: "",
  loanAmount: null,
  loanTerm: 0,
  employmentType: "",
  companyName: "",
  workYears: 0,
});

const documents = reactive([
  {
    type: "ID_CARD_FRONT",
    name: "身份证正面",
    description: "支持 JPG、JPEG、PNG，最大 10MB",
    accept: "image/jpeg,image/png",
    icon: markRaw(IdCard),
    file: null,
  },
  {
    type: "ID_CARD_BACK",
    name: "身份证反面",
    description: "用于身份核验",
    accept: "image/jpeg,image/png",
    icon: markRaw(IdCard),
    file: null,
  },
  {
    type: "BANK_STATEMENT",
    name: "银行流水",
    description: "用于收入能力评估",
    accept: "image/jpeg,image/png",
    icon: markRaw(WalletCards),
    file: null,
  },
]);

const selectedDocumentCount = computed(
  () => documents.filter((doc) => doc.file).length,
);
const allDocumentsSelected = computed(
  () => selectedDocumentCount.value === documents.length,
);

function validateName(name) {
  if (!name) return "请输入姓名";
  if (Array.from(name).length > 8) return "姓名不能超过8个汉字";
  const regex = /^[\u4e00-\u9fa5a-zA-Z]+$/g;
  if (!regex.test(name)) return "姓名只能包含中文或英文";
  return "";
}

function limitApplicantName() {
  form.applicantName = Array.from(form.applicantName).slice(0, 8).join("");
}

function validateIdCard(idCard) {
  if (!idCard) return "请输入身份证号";
  const regex =
    /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
  if (!regex.test(idCard)) return "请输入有效的18位身份证号";
  return "";
}

function validateMobile(mobile) {
  if (!mobile) return "请输入手机号";
  const regex = /^1[3-9]\d{9}$/;
  if (!regex.test(mobile)) return "请输入有效的11位手机号";
  return "";
}

const canContinue = computed(() => {
  if (currentStep.value === 0) {
    validationErrors.amount =
      form.loanAmount && form.loanAmount < 1000 ? "申请金额必须大于1000元" : "";
    return (
      form.applicantName &&
      form.idCardNo &&
      form.mobile &&
      form.productCode &&
      form.loanAmount >= 1000 &&
      form.loanTerm > 0
    );
  }
  if (currentStep.value === 1) {
    return form.employmentType && form.workYears > 0;
  }
  return allDocumentsSelected.value && agreed.value;
});

const primaryButtonText = computed(() => {
  if (submitting.value) return "提交中...";
  return currentStep.value === steps.length - 1 ? "提交申请" : "下一步";
});

function handleFileChange(type, event) {
  const file = event.target.files?.[0];
  const doc = documents.find((item) => item.type === type);
  if (doc) {
    doc.file = file || null;
  }
}

async function handlePrimaryAction() {
  errorMessage.value = "";

  if (currentStep.value === 0) {
    validationErrors.name = validateName(form.applicantName);
    validationErrors.idCard = validateIdCard(form.idCardNo);
    validationErrors.mobile = validateMobile(form.mobile);
    validationErrors.amount =
      form.loanAmount && form.loanAmount < 1000 ? "申请金额必须大于1000元" : "";

    if (
      validationErrors.name ||
      validationErrors.idCard ||
      validationErrors.mobile ||
      validationErrors.amount
    ) {
      return;
    }
  }

  if (currentStep.value < steps.length - 1) {
    currentStep.value++;
    return;
  }

  if (!canContinue.value) return;

  submitting.value = true;
  try {
    const application = await loanApi.createApplication({
      productCode: form.productCode,
      applicantName: form.applicantName,
      idCardNo: form.idCardNo,
      mobile: form.mobile,
      loanAmount: form.loanAmount,
      loanTerm: form.loanTerm,
      employmentType: form.employmentType,
      companyName: form.companyName,
      workYears: form.workYears,
      channelCode: "WEB",
    });

    for (const doc of documents) {
      const result = await loanApi.uploadDocument(
        application.applicationId,
        doc.type,
        doc.file,
      );
      if (result.ocrStatus !== "SUCCESS") {
        throw new Error(`${doc.name} OCR 识别失败，请上传清晰的 JPG 或 PNG 图片。`);
      }
    }

    emit("submitted", application.applicationId);
  } catch (error) {
    errorMessage.value = error.message || "提交失败，请稍后重试。";
  } finally {
    submitting.value = false;
  }
}
</script>
