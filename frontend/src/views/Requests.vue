<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="单号/标题" clearable style="width:200px" @keyup.enter="load" />
      <el-select v-model="status" clearable placeholder="状态" style="width:160px" @change="load">
        <el-option v-for="(lab,k) in statusOptions" :key="k" :label="lab" :value="k" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="$router.push('/requests/edit')" v-if="canEdit">新建申请</el-button>
      <el-button @click="exportExcel">导出 Excel</el-button>
    </div>
    <el-skeleton v-if="loading" animated :rows="6" />
    <EmptyState v-else-if="!rows.length" title="没有符合条件的申请" description="换个状态，或直接新建一张采购申请。" action-text="新建申请" action-to="/requests/edit" />
    <el-table v-else :data="rows" stripe>
      <el-table-column prop="requestNo" label="单号" width="180" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="deptName" label="部门" width="100" />
      <el-table-column prop="budgetAmount" label="预算" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{row}">{{ statusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{row}">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link @click="$router.push('/print/requests/'+row.id)">打印预览</el-button>
          <el-button link v-if="row.status==='DRAFT'||row.status==='REJECTED'" @click="$router.push('/requests/edit/'+row.id)">编辑</el-button>
          <el-button link type="success" v-if="row.status==='DRAFT'||row.status==='REJECTED'" @click="onSubmit(row)">提交</el-button>
          <el-button link type="warning" v-if="row.status==='PENDING_INQUIRY'||row.status==='PENDING_ORDER'" @click="$router.push('/inquiry/'+row.id)">询价</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" background layout="prev, pager, next" :total="total" v-model:current-page="page" @current-change="load" />

    <el-dialog v-model="detailVisible" title="申请详情" width="720px">
      <div v-if="detail">
        <p><b>{{ detail.request.title }}</b> （{{ detail.request.requestNo }}） 状态：{{ statusLabel(detail.request.status) }}</p>
        <p>申请人：{{ detail.applicantName }} / {{ detail.request.deptName }} 预算：{{ detail.request.budgetAmount }}</p>
        <el-table :data="detail.items" size="small">
          <el-table-column prop="materialName" label="物资" />
          <el-table-column prop="spec" label="规格" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="estimatePrice" label="估价" width="90" />
          <el-table-column prop="amount" label="金额" width="90" />
        </el-table>
        <div class="no-print" style="margin:8px 0">
          <el-button size="small" type="primary" @click="$router.push('/print/requests/'+detail.request.id)">打印预览</el-button>
        </div>
        <h4>审批记录</h4>
        <el-timeline>
          <el-timeline-item v-for="a in detail.approvals" :key="a.id" :timestamp="a.createdAt">
            {{ a.level }} - {{ a.result }} ：{{ a.comment || '无意见' }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { pageRequests, getRequest, submitRequest } from '@/api'
import { statusLabel } from '@/utils/status'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import http from '@/utils/request'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const store = useUserStore()
const canEdit = computed(() => store.hasAnyRole('ADMIN','APPLICANT'))
const rows = ref([]); const total = ref(0); const page = ref(1)
const keyword = ref(''); const status = ref('')
const statusOptions = { DRAFT:'草稿', PENDING_L1:'待一级审批', PENDING_L2:'待二级审批', PENDING_L3:'待三级审批', PENDING_INQUIRY:'待询价', PENDING_ORDER:'待下单', PENDING_RECEIVE:'待收货', COMPLETED:'已完成', REJECTED:'已驳回', CANCELLED:'已取消' }
const detailVisible = ref(false); const detail = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await pageRequests({
      page: page.value, size: 10, keyword: keyword.value, status: status.value,
      budgetSubjectId: route.query.budgetSubjectId || undefined
    })
    rows.value = data.records; total.value = Number(data.total)
  } finally { loading.value = false }
}
async function openDetail(row) { detail.value = await getRequest(row.id); detailVisible.value = true }
async function onSubmit(row) { await submitRequest(row.id); ElMessage.success('已提交'); load() }
async function exportExcel() {
  const blob = await http.get('/api/requests/export', { responseType: 'blob', params: { status: status.value } })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = '采购申请.xlsx'; a.click()
}
watch(() => [route.query.status, route.query.budgetSubjectId], () => {
  status.value = route.query.status ? String(route.query.status) : ''
  page.value = 1
  load()
})
onMounted(() => {
  if (route.query.status) status.value = String(route.query.status)
  load()
})
</script>

<style scoped>
.toolbar { display:flex; gap:8px; margin-bottom:12px; flex-wrap:wrap; }
</style>
