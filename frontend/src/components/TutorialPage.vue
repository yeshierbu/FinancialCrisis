<template>
  <div class="animate-fadeIn mx-auto max-w-5xl space-y-8">
    <div
      class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <p class="text-sm font-medium text-primary-600">使用指南</p>
        <h1 class="text-2xl font-bold text-gray-950">完成一笔智能信贷申请</h1>
      </div>
      <button class="btn-secondary" @click="$emit('back')">返回首页</button>
    </div>

    <div class="grid gap-4 md:grid-cols-5">
      <button
        v-for="(step, index) in tutorialSteps"
        :key="step.title"
        class="rounded-lg border p-4 text-left transition"
        :class="
          currentStep === index
            ? 'border-primary-200 bg-primary-50'
            : 'border-gray-100 bg-white hover:bg-gray-50'
        "
        @click="currentStep = index"
      >
        <span
          :class="[
            'step-indicator mb-3',
            currentStep >= index ? 'step-active' : 'step-inactive',
          ]"
          >{{ index + 1 }}</span
        >
        <span class="block text-sm font-semibold text-gray-900">{{
          step.title
        }}</span>
      </button>
    </div>

    <section class="card">
      <div class="flex items-start gap-4">
        <div
          class="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-600"
        >
          <component :is="activeStep.icon" class="h-6 w-6" />
        </div>
        <div>
          <h2 class="text-xl font-semibold text-gray-950">
            {{ activeStep.title }}
          </h2>
          <div class="mt-4 space-y-3 text-sm leading-6 text-gray-600">
            <p v-for="item in activeStep.content" :key="item">{{ item }}</p>
          </div>
        </div>
      </div>
    </section>

    <div class="flex justify-end gap-3">
      <button
        class="btn-secondary"
        :disabled="currentStep === 0"
        @click="currentStep--"
      >
        <ChevronLeft class="h-5 w-5" />
        上一步
      </button>
      <button
        v-if="currentStep < tutorialSteps.length - 1"
        class="btn-primary"
        @click="currentStep++"
      >
        下一步
        <ChevronRight class="h-5 w-5" />
      </button>
      <button v-else class="btn-primary" @click="$emit('start-application')">
        开始申请
        <ArrowRight class="h-5 w-5" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw, ref } from "vue";
import {
  ArrowRight,
  Brain,
  ChevronLeft,
  ChevronRight,
  FileCheck,
  Upload,
  UserPlus,
} from "lucide-vue-next";

defineEmits(["back", "start-application"]);

const currentStep = ref(0);

const tutorialSteps = [
  {
    title: "填写信息",
    icon: markRaw(UserPlus),
    content: [
      "填写姓名、身份证号、手机号、产品、金额和期限。",
      "后端会保存申请主表，并生成申请编号。",
    ],
  },
  {
    title: "补充工作信息",
    icon: markRaw(UserPlus),
    content: [
      "选择就业类型、工作年限并填写公司名称。",
      "这些字段用于后续偿债能力和风险评估扩展。",
    ],
  },
  {
    title: "选择材料",
    icon: markRaw(Upload),
    content: [
      "选择身份证正面、身份证反面和银行流水。",
      "演示版提交文件元数据，后端以 memory:// 地址模拟文件存储。",
    ],
  },
  {
    title: "智能审批",
    icon: markRaw(Brain),
    content: [
      "后端编排材料解析、反欺诈、偿债能力和合规决策。",
      "每次状态变化都会写入时间线，便于查询和审计。",
    ],
  },
  {
    title: "查看结果",
    icon: markRaw(FileCheck),
    content: [
      "在申请记录中选择任意申请，可以查看当前状态、时间线和审批报告地址。",
    ],
  },
];

const activeStep = computed(() => tutorialSteps[currentStep.value]);
</script>
