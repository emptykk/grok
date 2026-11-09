<template>
  <div class="ip-page">
    <PageHeader title="库存预警" sub="当前库存相对安全库存的色阶。低于安全线为红，接近为黄。">
      <el-button :loading="loading" @click="load">刷新</el-button>
    </PageHeader>
    <el-skeleton v-if="loading && !rows.length" animated :rows="6" />
    <div v-else class="ip-card" style="padding:8px">
      <el-table :data="rows" class="ip-animate-list">
        <el-table-column label="物资"><template #default="{ row }">{{ row.material?.name }}</template></el-table-column>
        <el-table-column label="编码" width="110"><template #default="{ row }">{{ row.material?.code }}</template></el-table-column>
        <el-table-column prop="quantity" label="当前" width="80" />
        <el-table-column label="安全" width="80"><template #default="{ row }">{{ row.material?.safeStock }}</template></el-table-column>
        <el-table-column label="库存水位" min-width="220">
          <template #default="{ row }">
            <div class="bar-line">
              <el-progress :percentage="pct(row)" :stroke-width="10" :color="color(row)" :format="() => pct(row) + '%'" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tag(row)" effect="dark">{{ label(row) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!rows.length" title="还没有库存台账" description="入库验收之后，这里会按安全库存着色。" />
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { listInventories } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const loading = ref(false)
function safe(row) { return Number(row.material?.safeStock || 0) }
function qty(row) { return Number(row.quantity || 0) }
function ratio(row) {
  const s = safe(row)
  if (!s) return qty(row) > 0 ? 1 : 0
  return qty(row) / s
}
function pct(row) { return Math.max(0, Math.min(100, Math.round(ratio(row) * 100))) }
function color(row) {
  const r = ratio(row)
  if (r < 1) return '#ef4444'
  if (r < 1.2) return '#f59e0b'
  return '#10b981'
}
function tag(row) {
  const r = ratio(row)
  if (r < 1) return 'danger'
  if (r < 1.2) return 'warning'
  return 'success'
}
function label(row) {
  const r = ratio(row)
  if (r < 1) return '低于安全'
  if (r < 1.2) return '接近安全'
  return '充足'
}
async function load() {
  loading.value = true
  try { rows.value = await listInventories() || [] }
  finally { loading.value = false }
}
onMounted(load)
</script>
<style scoped>
.bar-line { padding-right: 8px; }
</style>
