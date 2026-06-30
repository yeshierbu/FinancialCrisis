<template>
  <footer class="mt-16 border-t border-gray-100 bg-white">
    <div class="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-8 text-sm text-gray-500 md:flex-row md:items-center md:justify-between">
      <div class="flex items-center gap-3">
        <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-600 text-white">
          <CreditCard class="h-5 w-5" />
        </span>
        <div>
          <p class="font-semibold text-gray-800">智能信贷审批系统</p>
          <p>演示环境，数据保存在后端内存中。</p>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-4">
        <span class="flex items-center gap-1"><Phone class="h-4 w-4" />400-888-8888</span>
        <span class="flex items-center gap-1"><Mail class="h-4 w-4" />support@example.com</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { CreditCard, Mail, Phone } from 'lucide-vue-next'
</script>
