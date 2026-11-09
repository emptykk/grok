<template>
  <div class="ip-page">
    <PageHeader title="预算执行仪表" sub="占用超过 80% 会进入预警。点科目可下钻到绑定该科目的申请。">
      <el-button @click="exportExcel">导出 Excel</el-button>
      <el-button @click="load">刷新</el-button>
    </PageHeader>
    <el-skeleton v-if="loading && !rows.length" animated :rows="6" />
    <template v-else>
      <div class="rings">
        <button v-for="b in rows" :key="b.id" type="button" class="ring ip-card" @click="drill(b)">
          <el-progress type="dashboard" :percentage="pct(b)" :color="color(b)" :width="108" />
          <b>{{ b.dept }}</b>
          <span>{{ b.name }}</span>
          <em>已用 ¥{{ money(b.used) }} / {{ money(b.total) }}</em>
        </button>
      </div>
      <div class="ip-card pad" style="margin-top:14px">
        <div class="sec">超阈值预警（≥ 80%）</div>
        <el-table :data="warnings" size="small" empty-text="没有科目越过 80%">
          <el-table-column prop="dept" label="部门" width="120" />
          <el-table-column prop="name" label="科目" />
          <el-table-column label="占用" width="220">
            <template #default="{ row }">
              <el-progress :percentage="pct(row)" :color="color(row)" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column label="" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="drill(row)">相关申请</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-if="!rows.length" title="还没有预算科目" />
      </div>
    </template>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageBudgets } from '@/api/extended'
import { downloadFile } from '@/utils/download'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const rows = ref([])
const loading = ref(false)
const money = (v) => Number(v || 0).toLocaleString()
function pct(b) {
  const total = Number(b.total || 0)
  if (!total) return 0
  return Math.max(0, Math.min(100, Math.round((Number(b.used || 0) / total) * 100)))
}
function color(b) {
  const p = pct(b)
  if (p >= 95) return '#ef4444'
  if (p >= 80) return '#f59e0b'
  return '#14b8a6'
}
const warnings = computed(() => rows.value.filter(b => pct(b) >= 80))
function drill(b) {
  router.push({ path: '/requests', query: { budgetSubjectId: b.id } })
}
function exportExcel() { downloadFile('/api/budgets/export', '预算科目.xlsx') }
async function load() {
  loading.value = true
  try {
    const data = await pageBudgets({ page: 1, size: 50 })
    rows.value = data.records || data || []
  } finally { loading.value = false }
}
onMounted(load)
</script>
<style scoped>
.rings { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.ring {
  border: 1px solid var(--ip-border); background: var(--ip-surface); color: inherit;
  border-radius: 16px; padding: 12px; cursor: pointer; text-align: center;
}
.ring b, .ring span, .ring em { display: block; }
.ring span, .ring em { color: var(--ip-text-secondary); font-size: 12px; font-style: normal; margin-top: 4px; }
.pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 10px; }
</style>
