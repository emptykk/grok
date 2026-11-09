<template>
  <div class="sheet-page">
    <div class="toolbar no-print">
      <el-button @click="$router.back()">返回比价</el-button>
      <el-button type="primary" :disabled="!quotes.length" @click="print">打印比价表</el-button>
    </div>
    <el-skeleton v-if="loading" animated :rows="8" />
    <article v-else class="sheet">
      <header>
        <div class="brand">智能采购 · 比价表</div>
        <h1>申请 #{{ requestId }} 报价对比</h1>
        <p>询价单 {{ inquiry?.id || '—' }} · 当前轮次 {{ inquiry?.currentRound || 1 }} · 最低价已用色块标出</p>
      </header>
      <table class="grid">
        <thead>
          <tr>
            <th>供应商</th><th>总价</th><th>交期(天)</th><th>相对最低价</th><th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in ranked" :key="q.id || q.supplierId" :class="{ low: isLow(q) }">
            <td>{{ name(q.supplierId) }} <span v-if="isLow(q)" class="tag">最低价</span></td>
            <td>¥ {{ money(q.totalPrice) }}</td>
            <td>{{ q.deliveryDays }}</td>
            <td>{{ delta(q) }}</td>
            <td>{{ q.remark || '—' }}</td>
          </tr>
          <tr v-if="!ranked.length"><td colspan="5">还没有报价，请返回询价页录入。</td></tr>
        </tbody>
      </table>
      <footer>打印时间 {{ printedAt }} · 无大模型 Key 时，页面上的结论卡仍按规则生成，本表只列已录入报价。</footer>
    </article>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { inquiryByRequest } from '@/api'

const route = useRoute()
const requestId = route.params.requestId
const loading = ref(true)
const inquiry = ref(null)
const quotes = ref([])
const suppliers = ref([])
const printedAt = new Date().toLocaleString('zh-CN', { hour12: false })
const ranked = computed(() => [...quotes.value].sort((a, b) => Number(a.totalPrice || 0) - Number(b.totalPrice || 0)))
const low = computed(() => ranked.value.length ? Number(ranked.value[0].totalPrice || 0) : null)
function isLow(q) { return low.value != null && Number(q.totalPrice) === low.value }
function name(id) { return suppliers.value.find(s => s.id === id)?.name || ('供应商#' + id) }
function money(v) { return Number(v || 0).toLocaleString() }
function delta(q) {
  if (low.value == null || low.value === 0) return '—'
  const gap = Number(q.totalPrice || 0) - low.value
  return gap === 0 ? '基准' : `+¥${gap.toLocaleString()}`
}
function print() { window.print() }
onMounted(async () => {
  try {
    const data = await inquiryByRequest(requestId)
    inquiry.value = data.inquiry
    quotes.value = data.quotes || []
    suppliers.value = data.suppliers || []
  } finally { loading.value = false }
})
</script>
<style scoped>
.sheet-page { min-height: 100%; background: #e8edf3; padding: 24px; }
.toolbar { display: flex; gap: 8px; max-width: 840px; margin: 0 auto 12px; }
.sheet { max-width: 840px; margin: 0 auto; background: #fff; color: #111; padding: 36px 40px; box-shadow: 0 12px 40px rgba(15,23,42,.12); }
.brand { letter-spacing: .18em; font-size: 12px; color: #0f766e; font-weight: 700; }
h1 { margin: 8px 0 4px; font-size: 26px; }
header p, footer { color: #475569; font-size: 13px; }
table { width: 100%; border-collapse: collapse; margin-top: 16px; font-size: 13px; }
th, td { border: 1px solid #cbd5e1; padding: 8px 10px; text-align: left; }
th { background: #f1f5f9; }
tr.low td { background: #ecfdf5; font-weight: 700; }
.tag { display: inline-block; margin-left: 6px; font-size: 11px; background: #0d9488; color: #fff; border-radius: 999px; padding: 1px 6px; }
@media print {
  .sheet-page { background: #fff; padding: 0; }
  .no-print { display: none !important; }
  .sheet { box-shadow: none; max-width: none; }
}
</style>
