<template>
  <div class="ip-page">
    <PageHeader title="供应商准入 / 年审" sub="评分、黑名单与年审计划">
      <span class="badge-mock">真实 API</span>
      <el-button @click="load">刷新</el-button>
    </PageHeader>
    <div class="ip-card" style="padding:8px 8px 0">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="name" label="供应商" />
        <el-table-column prop="score" label="评分" width="80" />
        <el-table-column prop="level" label="等级" width="70" />
        <el-table-column prop="blacklist" label="黑名单" width="90" />
        <el-table-column prop="lastAudit" label="最近年审" />
        <el-table-column prop="nextAudit" label="下次年审" />
        <el-table-column prop="note" label="备注" />
        <el-table-column label="画像" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/suppliers/${row.id}/profile`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loading && !rows.length" />
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { pageSupplierAudits } from '@/api/extended'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    const data = await pageSupplierAudits({ page: 1, size: 50 })
    rows.value = data.records || data || []
  } finally { loading.value = false }
}
onMounted(load)
</script>
