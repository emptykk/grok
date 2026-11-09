<template>
  <el-card>
    <template #header>入库验收</template>
    <el-form inline>
      <el-form-item label="订单">
        <el-select v-model="orderId" filterable style="width:280px" @change="loadOrder">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo + ' / ' + statusLabel(o.status)" :value="o.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table v-if="detail" :data="detail.items" border>
      <el-table-column prop="materialName" label="物资" />
      <el-table-column prop="quantity" label="订购" width="80" />
      <el-table-column prop="receivedQty" label="已收" width="80" />
      <el-table-column label="本次入库" width="140">
        <template #default="{row}">
          <el-input-number v-model="row._qty" :min="0" :max="Number(row.quantity)-Number(row.receivedQty||0)" :controls="false" />
        </template>
      </el-table-column>
    </el-table>
    <el-input v-model="remark" type="textarea" placeholder="备注" style="margin-top:12px" />
    <el-button type="primary" style="margin-top:12px" :disabled="!detail" @click="submit">确认入库</el-button>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { pageOrders, getOrder, createInbound } from '@/api'
import { statusLabel } from '@/utils/status'
import { ElMessage } from 'element-plus'

const route = useRoute()
const orders = ref([]); const orderId = ref(null); const detail = ref(null); const remark = ref('')

async function loadOrders() {
  const data = await pageOrders({ page: 1, size: 100 })
  orders.value = (data.records || []).filter(o => o.status === 'PENDING_RECEIVE' || o.status === 'PARTIAL' || o.status === 'COMPLETED')
  if (route.params.orderId) {
    orderId.value = Number(route.params.orderId)
    await loadOrder()
  }
}
async function loadOrder() {
  if (!orderId.value) return
  const d = await getOrder(orderId.value)
  d.items.forEach(i => { i._qty = Number(i.quantity) - Number(i.receivedQty || 0) })
  detail.value = d
}
async function submit() {
  const items = detail.value.items.filter(i => i._qty > 0).map(i => ({ orderItemId: i.id, quantity: i._qty }))
  if (!items.length) return ElMessage.warning('请填写入库数量')
  await createInbound({ orderId: orderId.value, remark: remark.value, items })
  ElMessage.success('入库成功')
  loadOrder(); loadOrders()
}
onMounted(loadOrders)
</script>
