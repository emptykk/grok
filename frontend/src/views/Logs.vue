<template>
  <div class="ip-page">
    <PageHeader title="操作日志" sub="按用户、模块、关键字筛选，查看详情并导出 CSV">
      <el-button type="primary" @click="exportCsv">导出 CSV</el-button>
    </PageHeader>
    <div class="ip-card pad">
      <div class="filters">
        <el-input v-model="query.username" placeholder="用户" clearable style="width:140px" @keyup.enter="search" />
        <el-input v-model="query.module" placeholder="模块" clearable style="width:140px" @keyup.enter="search" />
        <el-input v-model="query.action" placeholder="动作" clearable style="width:140px" @keyup.enter="search" />
        <el-input v-model="query.keyword" placeholder="详情 / 路径" clearable style="width:200px" @keyup.enter="search" />
        <el-select v-model="query.success" clearable placeholder="结果" style="width:120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" class="ip-animate-list" @row-click="open">
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="username" label="用户" width="110" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="动作" width="140" />
        <el-table-column prop="detail" label="详情" show-overflow-tooltip />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success === 0 ? 'danger' : 'success'" size="small">{{ row.success === 0 ? '失败' : '成功' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="" width="80">
          <template #default="{ row }"><el-button link type="primary" @click.stop="open(row)">详情</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" background layout="prev, pager, next, total" :total="total" v-model:current-page="page" @current-change="load" />
    </div>

    <el-drawer v-model="visible" title="日志详情" size="420px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ detail.action }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ detail.success === 0 ? '失败' : '成功' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="路径">{{ detail.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ detail.detail }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageLogs, getLogDetail } from '@/api'
import { downloadFile } from '@/utils/download'
import PageHeader from '@/components/PageHeader.vue'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const query = reactive({ username: '', module: '', action: '', keyword: '', success: undefined })
const visible = ref(false)
const detail = ref(null)

function params() {
  return {
    page: page.value, size: 12,
    username: query.username || undefined,
    module: query.module || undefined,
    action: query.action || undefined,
    keyword: query.keyword || undefined,
    success: query.success
  }
}
async function load() {
  const d = await pageLogs(params())
  rows.value = d.records
  total.value = Number(d.total)
}
function search() { page.value = 1; load() }
async function open(row) {
  detail.value = await getLogDetail(row.id)
  visible.value = true
}
function exportCsv() {
  const { page: _p, size: _s, ...rest } = params()
  downloadFile('/api/logs/export', '操作日志.csv', rest)
}
onMounted(load)
</script>
<style scoped>
.pad { padding: 12px; }
.filters { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
</style>
