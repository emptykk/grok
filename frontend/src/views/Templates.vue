<template>
  <div class="ip-page">
    <PageHeader title="智能填单模板" sub="常用物资组合，一键带到申请">
      <span class="badge-mock">真实 API</span>
      <el-button @click="load">刷新</el-button>
    </PageHeader>
    <div class="ip-card" style="padding:8px 8px 0">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column prop="name" label="模板名" />
        <el-table-column prop="items" label="明细" />
        <el-table-column prop="usage" label="使用次数" width="100" />
      </el-table>
      <EmptyState v-if="!loading && !rows.length" />
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { pageTemplates } from '@/api/extended'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    const data = await pageTemplates({ page: 1, size: 50 })
    rows.value = data.records || data || []
  } finally { loading.value = false }
}
onMounted(load)
</script>
