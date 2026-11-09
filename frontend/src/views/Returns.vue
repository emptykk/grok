<template>
  <div class="ip-page">
    <PageHeader title="退货换货" sub="到货差异处理闭环；支持智能填单助手起草原因">
      <el-button type="primary" @click="openCreate">新建退换货</el-button>
      <el-button @click="load">刷新</el-button>
    </PageHeader>
    <div class="ip-card" style="padding:8px 8px 0">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="no" label="单号" />
        <el-table-column prop="orderNo" label="订单" />
        <el-table-column prop="material" label="物资" />
        <el-table-column prop="qty" label="数量" width="70" />
        <el-table-column prop="type" label="类型" width="80" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="createdAt" label="创建日" />
      </el-table>
      <EmptyState v-if="!loading && !rows.length" />
    </div>

    <el-dialog v-model="dlg" title="新建退换货" width="560px">
      <div style="display:flex;justify-content:flex-end;margin-bottom:8px">
        <el-button type="warning" plain size="small" @click="assistOpen = true">智能填单助手</el-button>
      </div>
      <el-form label-width="100px">
        <el-form-item label="订单ID"><el-input-number v-model="form.orderId" :min="1" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="退货 RETURN" value="RETURN" />
            <el-option label="换货 EXCHANGE" value="EXCHANGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">提交</el-button>
      </template>
    </el-dialog>

    <FormAssistDrawer
      v-model="assistOpen"
      scene="RETURN"
      title="智能填单助手 · 退换货"
      :form="form"
      @apply-field="onApplyField"
    />
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageReturns } from '@/api/extended'
import http from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import FormAssistDrawer from '@/components/FormAssistDrawer.vue'
import { ElMessage } from 'element-plus'

const rows = ref([])
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const assistOpen = ref(false)
const form = reactive({ orderId: 1, type: 'RETURN', reason: '' })

function openCreate() {
  Object.assign(form, { orderId: 1, type: 'RETURN', reason: '' })
  dlg.value = true
}
function onApplyField(s) {
  if (!s?.field) return
  if (s.field in form) form[s.field] = s.value
  ElMessage.success('已采纳：' + (s.label || s.field))
}
async function save() {
  saving.value = true
  try {
    await http.post('/api/returns', { ...form, items: [] })
    ElMessage.success('退换货单已创建')
    dlg.value = false
    load()
  } catch (e) {
    ElMessage.error(e?.message || '创建失败')
  } finally { saving.value = false }
}
async function load() {
  loading.value = true
  try {
    const data = await pageReturns({ page: 1, size: 50 })
    rows.value = data.records || data || []
  } finally { loading.value = false }
}
onMounted(load)
</script>
