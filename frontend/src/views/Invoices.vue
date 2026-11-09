<template>
  <div class="ip-page">
    <PageHeader title="发票与付款" sub="对账状态与付款申请（不接真实财务）">
      <span class="badge-mock">真实 API</span>
      <el-button @click="load">刷新</el-button>
    </PageHeader>
    <div class="ip-card" style="padding:8px 8px 0">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="no" label="发票号" />
        <el-table-column prop="orderNo" label="订单" />
        <el-table-column prop="supplier" label="供应商" />
        <el-table-column prop="amount" label="金额" />
        <el-table-column prop="tax" label="税额" />
        <el-table-column prop="status" label="发票状态" />
        <el-table-column label="付款进度" min-width="160">
          <template #default="{ row }">
            <el-progress :percentage="payPct(row)" :status="payPct(row) === 100 ? 'success' : undefined" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="付款状态" width="100" />
      </el-table>
      <EmptyState v-if="!loading && !rows.length" />
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { pageInvoices } from '@/api/extended'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const loading = ref(false)
function payPct(row) {
  const s = String(row.payStatus || row.status || '').toUpperCase()
  if (s.includes('PAID') || s.includes('已付')) return 100
  if (s.includes('APPROV') || s.includes('申请')) return 70
  if (s.includes('MATCH') || s.includes('对账')) return 50
  if (s && s !== '-') return 30
  return 15
}
async function load() {
  loading.value = true
  try {
    const data = await pageInvoices({ page: 1, size: 50 })
    rows.value = data.records || data || []
  } finally { loading.value = false }
}
onMounted(load)
</script>
