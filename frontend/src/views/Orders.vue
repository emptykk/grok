<template>
  <el-card>
    <el-skeleton v-if="loading" animated :rows="5" />
    <EmptyState v-else-if="!rows.length" title="还没有采购订单" description="询价选定供应商后，在比价页生成订单。" />
    <el-table v-else :data="rows" stripe>
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="requestId" label="申请ID" width="90" />
      <el-table-column prop="supplierId" label="供应商ID" width="100" />
      <el-table-column prop="totalAmount" label="金额" width="120" />
      <el-table-column label="状态" width="120"><template #default="{row}">{{ statusLabel(row.status) }}</template></el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-button link type="primary" @click="open(row)">详情</el-button>
          <el-button link type="success" v-if="row.status==='PENDING_RECEIVE'||row.status==='PARTIAL'" @click="$router.push('/inbound/'+row.id)">去入库</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" v-model:current-page="page" @current-change="load" />

    <el-dialog v-model="visible" title="订单详情" width="700px">
      <div v-if="detail">
        <p>订单 {{ detail.order.orderNo }} / 供应商：{{ detail.supplier?.name }} / 金额：{{ detail.order.totalAmount }}</p>
        <el-button v-if="detail.order.requestId" link type="primary" @click="$router.push('/trace/' + detail.order.requestId)">打开这条申请的全链路</el-button>
        <div v-if="traceEvents.length" class="mini">
          <div v-for="(e, i) in traceEvents" :key="i" class="ev"><b>{{ e.phase }}</b> {{ e.title }}</div>
        </div>
        <el-table :data="detail.items" size="small">
          <el-table-column prop="materialName" label="物资" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="receivedQty" label="已收" width="80" />
          <el-table-column prop="unitPrice" label="单价" width="90" />
          <el-table-column prop="amount" label="金额" width="90" />
        </el-table>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { pageOrders, getOrder, getRequestTrace } from '@/api'
import { statusLabel } from '@/utils/status'
import EmptyState from '@/components/EmptyState.vue'
const rows = ref([]); const total = ref(0); const page = ref(1)
const loading = ref(false)
const visible = ref(false); const detail = ref(null); const traceEvents = ref([])
async function load() {
  loading.value = true
  try {
    const data = await pageOrders({ page: page.value, size: 10 })
    rows.value = data.records; total.value = Number(data.total)
  } finally { loading.value = false }
}
async function open(row) {
  detail.value = await getOrder(row.id)
  traceEvents.value = []
  visible.value = true
  if (detail.value?.order?.requestId) {
    try {
      const trace = await getRequestTrace(detail.value.order.requestId)
      traceEvents.value = (trace.events || []).slice(0, 8)
    } catch { traceEvents.value = [] }
  }
}
onMounted(load)
</script>
<style scoped>
.mini { margin: 8px 0 12px; display: flex; flex-direction: column; gap: 4px; }
.ev { font-size: 12px; color: var(--ip-text-secondary); }
.ev b { color: var(--ip-accent); margin-right: 6px; }
</style>
