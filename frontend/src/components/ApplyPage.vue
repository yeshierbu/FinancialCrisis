<template>
  <div class="animate-fadeIn mx-auto max-w-6xl space-y-7">
    <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="eyebrow text-primary-600">Loan application</p>
        <h1 class="mt-2 text-3xl font-semibold tracking-tight text-slate-950">填写申请信息</h1>
        <p class="mt-2 text-sm text-slate-500">完成三步，启动协作式智能审批。</p>
      </div>
      <button class="btn-secondary" @click="$emit('back')">返回首页</button>
    </div>

    <div class="grid gap-3 md:grid-cols-3">
      <div v-for="(step, index) in steps" :key="step" class="relative flex items-center gap-3 rounded-2xl border p-4 transition" :class="currentStep >= index ? 'border-primary-100 bg-primary-50/70' : 'border-slate-200 bg-white'">
        <span :class="['step-indicator', currentStep > index ? 'step-completed' : currentStep === index ? 'step-active' : 'step-inactive']">
          <CheckCircle v-if="currentStep > index" class="h-5 w-5" />
          <span v-else>{{ index + 1 }}</span>
        </span>
        <div>
          <p class="text-xs font-medium uppercase tracking-wider text-slate-400">Step 0{{ index + 1 }}</p>
          <p class="mt-0.5 text-sm font-semibold text-slate-800">{{ step }}</p>
        </div>
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px] lg:items-start">
      <form class="card space-y-7" @submit.prevent="handlePrimaryAction">
        <div v-if="currentStep === 0" class="space-y-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="eyebrow">01 · Identity</p>
              <h2 class="mt-2 text-xl font-semibold text-slate-950">基本信息</h2>
            </div>
            <div class="hidden rounded-xl bg-slate-50 px-3 py-2 text-right text-xs text-slate-500 sm:block">
              <span class="block font-semibold text-slate-700">信息加密演示</span>
              仅用于本次审批流程
            </div>
          </div>
          <div class="grid gap-5 md:grid-cols-2">
            <label class="block">
              <span class="form-label">姓名</span>
              <input v-model.trim="form.applicantName" class="form-input" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.applicantName }" required maxlength="50" autocomplete="name" placeholder="请输入申请人姓名" @blur="validateField('applicantName')" @input="clearFieldError('applicantName')" />
              <span v-if="fieldErrors.applicantName" class="mt-2 block text-xs text-red-600">{{ fieldErrors.applicantName }}</span>
            </label>
            <label class="block">
              <span class="form-label">身份证号</span>
              <input v-model.trim="form.idCardNo" class="form-input uppercase" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.idCardNo }" required maxlength="18" inputmode="text" autocomplete="off" pattern="^\d{17}[\dXx]$" placeholder="请输入 18 位身份证号" @blur="validateField('idCardNo')" @input="sanitizeIdCard" />
              <span v-if="fieldErrors.idCardNo" class="mt-2 block text-xs text-red-600">{{ fieldErrors.idCardNo }}</span>
            </label>
            <label class="block">
              <span class="form-label">手机号</span>
              <input v-model.trim="form.mobile" class="form-input" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.mobile }" required type="tel" inputmode="numeric" maxlength="11" autocomplete="tel" pattern="^1[3-9]\d{9}$" placeholder="请输入 11 位手机号" @blur="validateField('mobile')" @input="sanitizeMobile" />
              <span v-if="fieldErrors.mobile" class="mt-2 block text-xs text-red-600">{{ fieldErrors.mobile }}</span>
            </label>
            <label class="block">
              <span class="form-label">贷款产品</span>
              <select v-model="form.productCode" class="form-input" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.productCode }" required @change="validateField('productCode')">
                <option value="">请选择产品</option>
                <option value="CONSUMER_LOAN_STD">个人消费贷标准版</option>
                <option value="CONSUMER_LOAN_EXP">个人消费贷尊享版</option>
                <option value="HOUSEHOLD_LOAN">家装贷款</option>
              </select>
              <span v-if="fieldErrors.productCode" class="mt-2 block text-xs text-red-600">{{ fieldErrors.productCode }}</span>
            </label>
            <label class="block">
              <span class="form-label">申请金额</span>
              <div class="relative">
                <input v-model.number="form.loanAmount" class="form-input pr-14" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.loanAmount }" min="1000" step="0.01" required type="number" inputmode="decimal" placeholder="最低 1000 元" @blur="validateField('loanAmount')" @input="clearFieldError('loanAmount')" />
                <span class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm text-slate-400">元</span>
              </div>
              <span v-if="fieldErrors.loanAmount" class="mt-2 block text-xs text-red-600">{{ fieldErrors.loanAmount }}</span>
            </label>
            <label class="block">
              <span class="form-label">贷款期限</span>
              <select v-model.number="form.loanTerm" class="form-input" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.loanTerm }" required @change="validateField('loanTerm')">
                <option :value="0">请选择期限</option>
                <option v-for="term in loanTerms" :key="term" :value="term">{{ term }} 个月</option>
              </select>
              <span v-if="fieldErrors.loanTerm" class="mt-2 block text-xs text-red-600">{{ fieldErrors.loanTerm }}</span>
            </label>
          </div>
        </div>

        <div v-else-if="currentStep === 1" class="space-y-6">
          <div>
            <p class="eyebrow">02 · Capacity</p>
            <h2 class="mt-2 text-xl font-semibold text-slate-950">工作信息</h2>
            <p class="mt-2 text-sm text-slate-500">这些信息会帮助偿债能力 Agent 建立可解释的收入估算。</p>
          </div>
          <div class="grid gap-5 md:grid-cols-2">
            <label class="block">
              <span class="form-label">就业类型</span>
              <select v-model="form.employmentType" class="form-input" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.employmentType }" required @change="validateField('employmentType')">
                <option value="">请选择就业类型</option>
                <option value="FULL_TIME">全职</option>
                <option value="PART_TIME">兼职</option>
                <option value="SELF_EMPLOYED">自雇</option>
                <option value="RETIRED">退休</option>
              </select>
              <span v-if="fieldErrors.employmentType" class="mt-2 block text-xs text-red-600">{{ fieldErrors.employmentType }}</span>
            </label>
            <label class="block">
              <span class="form-label">工作年限</span>
              <select v-model.number="form.workYears" class="form-input" :class="{ 'border-red-300 ring-4 ring-red-100': fieldErrors.workYears }" required @change="validateField('workYears')">
                <option :value="0">请选择工作年限</option>
                <option :value="1">1 年以内</option>
                <option :value="3">1-3 年</option>
                <option :value="5">3-5 年</option>
                <option :value="10">5-10 年</option>
                <option :value="20">10 年以上</option>
              </select>
              <span v-if="fieldErrors.workYears" class="mt-2 block text-xs text-red-600">{{ fieldErrors.workYears }}</span>
            </label>
            <label class="block">
              <span class="form-label">公司名称</span>
              <input v-model.trim="form.companyName" class="form-input" placeholder="请输入公司名称" />
            </label>
            <label class="block">
              <span class="form-label">月收入</span>
              <div class="relative">
                <input v-model.number="monthlyIncome" class="form-input pr-14" min="0" type="number" placeholder="用于前端展示" />
                <span class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm text-slate-400">元</span>
              </div>
              <span class="mt-2 block text-xs text-slate-400">演示版暂不提交该字段。</span>
            </label>
          </div>
        </div>

        <div v-else class="space-y-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="eyebrow">03 · Evidence</p>
              <h2 class="mt-2 text-xl font-semibold text-slate-950">上传材料</h2>
              <p class="mt-2 text-sm text-slate-500">当前后端接收材料元数据，不上传真实文件内容。</p>
            </div>
            <span class="status-badge" :class="allDocumentsSelected ? 'status-approved' : 'status-pending'">
              {{ selectedDocumentCount }}/{{ documents.length }} 已选择
            </span>
          </div>

          <div class="grid gap-4 md:grid-cols-3">
            <div v-for="doc in documents" :key="doc.type" class="group rounded-2xl border border-dashed p-5 transition" :class="doc.file ? 'border-emerald-200 bg-emerald-50/40' : 'border-slate-200 hover:border-primary-200 hover:bg-primary-50/30'">
              <div class="mb-5 flex items-center justify-between">
                <div class="flex h-11 w-11 items-center justify-center rounded-2xl" :class="doc.file ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-slate-500'">
                  <component :is="doc.icon" class="h-6 w-6" />
                </div>
                <CheckCircle v-if="doc.file" class="h-5 w-5 text-emerald-500" />
              </div>
              <h3 class="font-semibold text-slate-900">{{ doc.name }}</h3>
              <p class="mt-1 text-sm leading-6 text-slate-500">{{ doc.description }}</p>
              <p v-if="doc.file" class="mt-4 truncate text-sm font-semibold text-emerald-700">{{ doc.file.name }}</p>
              <p v-else class="mt-4 text-xs text-slate-400">尚未选择文件</p>
              <label class="btn-secondary mt-4 w-full cursor-pointer !py-2.5">
                <input class="hidden" type="file" :accept="doc.accept" @change="handleFileChange(doc.type, $event)" />
                <Upload class="h-4 w-4" />
                {{ doc.file ? '重新选择' : '选择文件' }}
              </label>
              <p v-if="fieldErrors[doc.type]" class="mt-2 text-xs leading-5 text-red-600">{{ fieldErrors[doc.type] }}</p>
            </div>
          </div>

          <label class="flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
            <input v-model="agreed" class="mt-1 h-4 w-4 accent-primary-600" type="checkbox" @change="clearFieldError('agreement')" />
            <span>我确认提交的信息真实有效，并同意用于本次智能信贷审批演示。</span>
          </label>
          <p v-if="fieldErrors.agreement" class="-mt-3 text-xs text-red-600">{{ fieldErrors.agreement }}</p>
        </div>

        <div v-if="errorMessage" class="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
          {{ errorMessage }}
        </div>

        <div class="flex justify-between border-t border-slate-100 pt-6">
          <button type="button" class="btn-secondary" :disabled="currentStep === 0 || submitting" @click="currentStep--">
            上一步
          </button>
          <button type="submit" class="btn-primary" :disabled="submitting">
            <Loader2 v-if="submitting" class="h-5 w-5 animate-spin" />
            <span>{{ primaryButtonText }}</span>
            <ArrowRight v-if="!submitting && currentStep < steps.length - 1" class="h-4 w-4" />
          </button>
        </div>
      </form>

      <aside class="space-y-4 lg:sticky lg:top-24">
        <div class="rounded-3xl bg-slate-950 p-6 text-white shadow-xl shadow-slate-900/10">
          <div class="flex items-center gap-3">
            <span class="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/10 text-cyan-300"><BrainCircuit class="h-5 w-5" /></span>
            <div>
              <p class="text-xs uppercase tracking-[0.18em] text-slate-400">Smart review</p>
              <p class="mt-1 font-semibold">审批链路已准备</p>
            </div>
          </div>
          <div class="mt-6 space-y-4 text-sm">
            <div v-for="(item, index) in reviewSteps" :key="item" class="flex items-center gap-3">
              <span class="flex h-6 w-6 items-center justify-center rounded-full text-xs" :class="currentStep > index ? 'bg-emerald-400 text-slate-950' : currentStep === index ? 'bg-cyan-300 text-slate-950' : 'bg-white/10 text-slate-500'">
                <CheckCircle v-if="currentStep > index" class="h-3.5 w-3.5" />
                <span v-else>{{ index + 1 }}</span>
              </span>
              <span :class="currentStep >= index ? 'text-white' : 'text-slate-500'">{{ item }}</span>
            </div>
          </div>
        </div>
        <div class="card !p-5">
          <div class="flex items-center gap-2 text-slate-800">
            <ShieldCheck class="h-5 w-5 text-emerald-500" />
            <p class="font-semibold">演示提示</p>
          </div>
          <p class="mt-3 text-sm leading-6 text-slate-500">数据保存在后端内存中，应用重启后会清空。材料将通过 memory:// 地址模拟存储。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw, reactive, ref } from 'vue'
import { ArrowRight, BrainCircuit, CheckCircle, IdCard, Loader2, ShieldCheck, Upload, WalletCards } from 'lucide-vue-next'
import { loanApi } from '../services/api'

const emit = defineEmits(['back', 'submitted'])

const currentStep = ref(0)
const submitting = ref(false)
const agreed = ref(false)
const errorMessage = ref('')
const monthlyIncome = ref(null)
const fieldErrors = reactive({})

const steps = ['基本信息', '工作信息', '申请材料']
const reviewSteps = ['材料完整性校验', '反欺诈与偿债分析', '协作审查与决策']
const loanTerms = [6, 12, 24, 36, 48, 60]

const form = reactive({
  applicantName: '',
  idCardNo: '',
  mobile: '',
  productCode: '',
  loanAmount: null,
  loanTerm: 0,
  employmentType: '',
  companyName: '',
  workYears: 0,
})

const documents = reactive([
  {
    type: 'ID_CARD_FRONT',
    name: '身份证正面',
    description: '用于身份核验',
    accept: 'image/*,application/pdf',
    icon: markRaw(IdCard),
    file: null,
  },
  {
    type: 'ID_CARD_BACK',
    name: '身份证反面',
    description: '补充身份信息',
    accept: 'image/*,application/pdf',
    icon: markRaw(IdCard),
    file: null,
  },
  {
    type: 'BANK_STATEMENT',
    name: '银行流水',
    description: '用于收入能力评估',
    accept: 'image/*,application/pdf',
    icon: markRaw(WalletCards),
    file: null,
  },
])

const allowedProducts = ['CONSUMER_LOAN_STD', 'CONSUMER_LOAN_EXP', 'HOUSEHOLD_LOAN']
const allowedEmploymentTypes = ['FULL_TIME', 'PART_TIME', 'SELF_EMPLOYED', 'RETIRED']
const maxFileSize = 20 * 1024 * 1024
const allowedFileExtensions = ['.jpg', '.jpeg', '.png', '.pdf']

const selectedDocumentCount = computed(() => documents.filter((doc) => doc.file).length)
const allDocumentsSelected = computed(() => selectedDocumentCount.value === documents.length)

const primaryButtonText = computed(() => {
  if (submitting.value) return '提交中...'
  return currentStep.value === steps.length - 1 ? '提交申请' : '下一步'
})

function handleFileChange(type, event) {
  const file = event.target.files?.[0]
  const doc = documents.find((item) => item.type === type)
  if (!doc) return

  doc.file = null
  clearFieldError(type)

  if (!file) return

  const fileName = file.name.toLowerCase()
  const extensionAllowed = allowedFileExtensions.some((extension) => fileName.endsWith(extension))
  if (!extensionAllowed) {
    fieldErrors[type] = '仅支持 JPG、JPEG、PNG 或 PDF 文件。'
    event.target.value = ''
    return
  }

  if (file.size > maxFileSize) {
    fieldErrors[type] = '文件大小不能超过 20MB。'
    event.target.value = ''
    return
  }

  doc.file = file
}

async function handlePrimaryAction() {
  errorMessage.value = ''
  if (currentStep.value < steps.length - 1) {
    if (!validateCurrentStep()) return
    currentStep.value++
    return
  }

  if (!validateCurrentStep()) return

  submitting.value = true
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
      channelCode: 'WEB',
    })

    for (const doc of documents) {
      await loanApi.uploadDocument(application.applicationId, {
        documentType: doc.type,
        fileName: doc.file.name,
        fileUrl: `memory://uploads/${application.applicationId}/${encodeURIComponent(doc.file.name)}`,
        fileSize: doc.file.size,
        fileHash: `${doc.type}-${doc.file.size}-${doc.file.lastModified}`,
      })
    }

    emit('submitted', application.applicationId)
  } catch (error) {
    errorMessage.value = error.message || '提交失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}

function sanitizeMobile() {
  form.mobile = String(form.mobile || '').replace(/\D/g, '').slice(0, 11)
  clearFieldError('mobile')
}

function sanitizeIdCard() {
  form.idCardNo = String(form.idCardNo || '').toUpperCase().replace(/[^0-9X]/g, '').slice(0, 18)
  clearFieldError('idCardNo')
}

function validateField(field) {
  const value = form[field]
  let message = ''

  switch (field) {
    case 'applicantName':
      if (!String(value || '').trim()) message = '请输入申请人姓名。'
      break
    case 'idCardNo':
      if (!/^\d{17}[\dX]$/.test(String(value || '').toUpperCase())) message = '请输入 18 位身份证号，最后一位可为 X。'
      break
    case 'mobile':
      if (!/^1[3-9]\d{9}$/.test(String(value || ''))) message = '请输入 11 位数字手机号，且以 1 开头。'
      break
    case 'productCode':
      if (!allowedProducts.includes(value)) message = '请选择有效的贷款产品。'
      break
    case 'loanAmount': {
      const amount = Number(value)
      if (!Number.isFinite(amount) || amount < 1000) message = '申请金额不能低于 1000 元。'
      break
    }
    case 'loanTerm':
      if (!loanTerms.includes(Number(value))) message = '请选择有效的贷款期限。'
      break
    case 'employmentType':
      if (!allowedEmploymentTypes.includes(value)) message = '请选择就业类型。'
      break
    case 'workYears':
      if (!Number.isFinite(Number(value)) || Number(value) <= 0) message = '请选择工作年限。'
      break
    default:
      break
  }

  fieldErrors[field] = message
  return !message
}

function validateCurrentStep() {
  let valid = true

  if (currentStep.value === 0) {
    valid = ['applicantName', 'idCardNo', 'mobile', 'productCode', 'loanAmount', 'loanTerm']
      .map(validateField)
      .every(Boolean)
  } else if (currentStep.value === 1) {
    valid = ['employmentType', 'workYears'].map(validateField).every(Boolean)
  } else {
    const documentsValid = documents.every((doc) => {
      if (doc.file) {
        fieldErrors[doc.type] = ''
        return true
      }
      fieldErrors[doc.type] = `请上传${doc.name}。`
      return false
    })
    const agreementValid = agreed.value
    fieldErrors.agreement = agreementValid ? '' : '请先确认信息真实有效。'
    valid = documentsValid && agreementValid
  }

  if (!valid) errorMessage.value = '请先修正标红字段后继续。'
  return valid
}

function clearFieldError(field) {
  fieldErrors[field] = ''
  if (!Object.values(fieldErrors).some(Boolean)) errorMessage.value = ''
}
</script>
