<template>
  <div class="ip-page">
    <PageHeader title="合同管理" sub="订单关联合同、到期与风险摘要；支持智能填单助手">
      <el-button type="primary" @click="openCreate">新建合同</el-button>
      <el-button @click="load">刷新</el-button>
    </PageHeader>
    <div class="ip-card" style="padding:8px 8px 0">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="no" label="合同号" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="supplier" label="供应商" />
        <el-table-column prop="amount" label="金额" />
        <el-table-column prop="end" label="到期日" width="120">
          <template #default="{ row }">
            <span :class="{ soon: expiring(row) }">{{ row.end || '-' }}</span>
            <el-tag v-if="expiring(row)" size="small" type="danger" style="margin-left:6px">临期</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="风险摘要" width="150">
          <template #default="{ row }">
            <el-button type="warning" size="small" @click="aiSum(row)">查看风险摘要</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-skeleton v-if="loading && !rows.length" animated :rows="4" />
      <EmptyState v-if="!loading && !rows.length" title="还没有合同" description="可由订单生成，或在这里手工新建。">
        <el-button type="primary" @click="openCreate">新建合同</el-button>
      </EmptyState>
    </div>

    <el-dialog v-model="dlg" title="新建合同" width="640px">
      <div style="display:flex;justify-content:flex-end;margin-bottom:8px">
        <el-button type="warning" plain size="small" @click="assistOpen = true">智能填单助手</el-button>
      </div>
      <el-form label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="供应商ID"><el-input-number v-model="form.supplierId" :min="1" /></el-form-item>
        <el-form-item label="订单ID"><el-input-number v-model="form.orderId" :min="0" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.amount" :min="0" :step="100" /></el-form-item>
        <el-form-item label="开始日"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="到期日"><el-date-picker v-model="form.endDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="正文"><el-input v-model="form.contentText" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="riskOpen" title="合同风险摘要" size="420px">
      <el-tag size="small" type="warning" style="margin-bottom:10px">无 Key 时为规则摘要</el-tag>
      <div class="risk-body">{{ riskText }}</div>
    </el-drawer>

    <FormAssistDrawer
      v-model="assistOpen"
      scene="CONTRACT"
      title="智能填单助手 · 合同"
      :form="form"
      @apply-field="onApplyField"
    />
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageContracts, summarizeContract } from '@/api/extended'
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
const riskOpen = ref(false)
const riskText = ref('')
const form = reactive({
  title: '', supplierId: 1, orderId: null, amount: 0,
  startDate: '', endDate: '', contentText: '', remark: ''
})

function expiring(row) {
  if (!row.end) return false
  const end = new Date(row.end)
  if (Number.isNaN(end.getTime())) return false
  const days = (end.getTime() - Date.now()) / 86400000
  return days >= 0 && days <= 30
}
async function aiSum(row) {
  const data = await summarizeContract(row.id)
  riskText.value = (data.summary || '暂无摘要') + (data.risks?.length ? '\n\n风险点：' + data.risks.join('；') : '')
  riskOpen.value = true
}
function openCreate() {
  Object.assign(form, { title: '', supplierId: 1, orderId: null, amount: 0, startDate: '', endDate: '', contentText: '', remark: '' })
  dlg.value = true
}
function onApplyField(s) {
  if (!s?.field) return
  if (s.field === 'amount') form.amount = Number(s.value || 0)
  else if (s.field in form) form[s.field] = s.value
  ElMessage.success('已采纳：' + (s.label || s.field))
}
async function save() {
  saving.value = true
  try {
    await http.post('/api/contracts', { ...form })
    ElMessage.success('合同已创建')
    dlg.value = false
    load()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally { saving.value = false }
}
async function load() {
  loading.value = true
  try {
    const data = await pageContracts({ page: 1, size: 50 })
    rows.value = data.records || data || []
  } finally { loading.value = false }
}
onMounted(load)
</script>
<style scoped>
.soon { color: var(--ip-danger); font-weight: 700; }
.risk-body { white-space: pre-wrap; line-height: 1.7; font-size: 14px; }
</style>
