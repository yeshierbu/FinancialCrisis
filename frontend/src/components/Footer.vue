<template>
  <footer class="mt-20 border-t border-slate-200/70 bg-white/70">
    <div class="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-9 text-sm text-slate-500 sm:px-6 md:flex-row md:items-center md:justify-between">
      <div class="flex items-center gap-3">
        <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-950 text-white">
          <CreditCard class="h-5 w-5" />
        </span>
        <div>
          <p class="font-semibold text-slate-800">智能信贷审批系统</p>
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
