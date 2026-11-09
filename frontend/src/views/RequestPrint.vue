<template>
  <div class="sheet-page">
    <div class="toolbar no-print">
      <el-button @click="$router.back()">返回</el-button>
      <el-button type="primary" @click="print">打印 / 导出 PDF</el-button>
    </div>
    <el-skeleton v-if="loading" animated :rows="8" />
    <article v-else-if="detail" class="sheet">
      <header>
        <div class="brand">智能采购 · 申请单</div>
        <h1>{{ req.title }}</h1>
        <p>{{ req.requestNo }} · {{ statusLabel(req.status) }}</p>
      </header>
      <table class="meta">
        <tr><th>申请人</th><td>{{ detail.applicantName || '-' }}</td><th>部门</th><td>{{ req.deptName || '-' }}</td></tr>
        <tr><th>预算金额</th><td>¥ {{ money(req.budgetAmount) }}</td><th>审批级数</th><td>{{ req.requiredLevels || '-' }}</td></tr>
        <tr><th>预算科目</th><td colspan="3">{{ detail.budgetSubject?.name || '未绑定' }}</td></tr>
        <tr><th>备注</th><td colspan="3">{{ req.remark || '—' }}</td></tr>
      </table>
      <h2>物资明细</h2>
      <table class="grid">
        <thead>
          <tr><th>物资</th><th>规格</th><th>单位</th><th>数量</th><th>估价</th><th>金额</th></tr>
        </thead>
        <tbody>
          <tr v-for="it in detail.items || []" :key="it.id">
            <td>{{ it.materialName }}</td>
            <td>{{ it.spec }}</td>
            <td>{{ it.unit }}</td>
            <td>{{ it.quantity }}</td>
            <td>{{ money(it.estimatePrice) }}</td>
            <td>{{ money(it.amount) }}</td>
          </tr>
        </tbody>
      </table>
      <h2>审批轨迹</h2>
      <table class="grid">
        <thead><tr><th>级别</th><th>结果</th><th>时间</th><th>意见</th></tr></thead>
        <tbody>
          <tr v-for="a in detail.approvals || []" :key="a.id">
            <td>{{ a.level }}</td>
            <td>{{ a.result === 'PASS' ? '通过' : a.result === 'REJECT' ? '驳回' : a.result }}</td>
            <td>{{ a.createdAt }}</td>
            <td>{{ a.comment || '无' }}</td>
          </tr>
          <tr v-if="!(detail.approvals || []).length"><td colspan="4">尚无审批记录</td></tr>
        </tbody>
      </table>
      <footer>本页为答辩演示打印稿 · 智能采购管理系统 · 打印时间 {{ printedAt }}</footer>
    </article>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getRequest } from '@/api'
import { statusLabel } from '@/utils/status'

const route = useRoute()
const detail = ref(null)
const loading = ref(true)
const printedAt = new Date().toLocaleString('zh-CN', { hour12: false })
const req = computed(() => detail.value?.request || {})
const money = (v) => Number(v || 0).toLocaleString()
function print() { window.print() }
onMounted(async () => {
  try { detail.value = await getRequest(route.params.id) }
  finally { loading.value = false }
})
</script>
<style scoped>
.sheet-page { min-height: 100%; background: #e8edf3; padding: 24px; }
.toolbar { display: flex; gap: 8px; max-width: 840px; margin: 0 auto 12px; }
.sheet {
  max-width: 840px; margin: 0 auto; background: #fff; color: #111;
  padding: 36px 40px 28px; box-shadow: 0 12px 40px rgba(15,23,42,.12);
}
.brand { letter-spacing: .18em; font-size: 12px; color: #0f766e; font-weight: 700; }
h1 { margin: 8px 0 4px; font-size: 26px; }
header p { margin: 0 0 16px; color: #475569; }
h2 { font-size: 15px; margin: 22px 0 8px; border-left: 3px solid #0d9488; padding-left: 8px; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
.meta th { width: 88px; background: #f8fafc; }
.meta th, .meta td, .grid th, .grid td { border: 1px solid #cbd5e1; padding: 8px 10px; text-align: left; }
.grid th { background: #f1f5f9; }
footer { margin-top: 22px; font-size: 12px; color: #64748b; }
@media print {
  .sheet-page { background: #fff; padding: 0; }
  .no-print { display: none !important; }
  .sheet { box-shadow: none; max-width: none; }
}
</style>
