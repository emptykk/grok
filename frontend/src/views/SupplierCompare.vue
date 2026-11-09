<template>
  <div class="ip-page">
    <PageHeader title="供应商对比" sub="勾选 2 到 3 家，并排看雷达和关键指标。适合答辩时解释为什么选定华信。">
      <el-button type="primary" :disabled="picked.length < 2" :loading="loading" @click="compare">开始对比</el-button>
    </PageHeader>
    <div class="ip-card pad">
      <el-checkbox-group v-model="picked" :max="3">
        <el-checkbox v-for="s in suppliers" :key="s.id" :label="s.id">{{ s.name }}</el-checkbox>
      </el-checkbox-group>
      <p class="hint">已选 {{ picked.length }} / 3。至少两家。</p>
    </div>
    <el-row v-if="profiles.length" :gutter="14" style="margin-top:14px">
      <el-col :xs="24" :lg="10">
        <div class="ip-card pad">
          <div class="sec">能力雷达</div>
          <ChartBox :option="radarOption" height="360px" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="14">
        <div class="ip-card" style="padding:8px">
          <el-table :data="rows" size="small">
            <el-table-column prop="metric" label="指标" width="120" />
            <el-table-column v-for="p in profiles" :key="p.id" :label="p.name">
              <template #default="{ row }">{{ row.values[p.id] }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { pageSuppliers, supplierProfile } from '@/api'
import { useThemeStore } from '@/stores/theme'
import PageHeader from '@/components/PageHeader.vue'
import ChartBox from '@/components/ChartBox.vue'

const theme = useThemeStore()
const suppliers = ref([])
const picked = ref([1, 2])
const profiles = ref([])
const loading = ref(false)
const colors = ['#14b8a6', '#60a5fa', '#f59e0b']

const radarOption = computed(() => {
  const dark = theme.dark
  const names = (profiles.value[0]?.radar || []).map(i => i.name)
  return {
    color: colors,
    tooltip: {},
    legend: { bottom: 0, textStyle: { color: dark ? '#cbd5e1' : '#334155' } },
    radar: {
      indicator: names.map(n => ({ name: n, max: 100 })),
      axisName: { color: dark ? '#cbd5e1' : '#334155' }
    },
    series: [{
      type: 'radar',
      data: profiles.value.map(p => ({
        name: p.name,
        value: (p.radar || []).map(i => Number(i.value)),
        areaStyle: { opacity: 0.15 }
      }))
    }]
  }
})
const rows = computed(() => {
  const specs = [
    ['准入分', p => p.admissionScore ?? '—'],
    ['历史订单', p => p.orderCount ?? 0],
    ['累计金额', p => '¥' + Number(p.totalAmount || 0).toLocaleString()],
    ['成交占比', p => (p.amountShare ?? 0) + '%'],
    ['准时率', p => p.onTimeRate == null ? '样本不足' : Math.round(p.onTimeRate * 100) + '%'],
    ['合格率', p => p.qualifyRate == null ? '—' : Math.round(p.qualifyRate * 100) + '%'],
    ['风险', p => (p.riskTags || []).map(t => t.label).join('、') || '—']
  ]
  return specs.map(([metric, pick]) => ({
    metric,
    values: Object.fromEntries(profiles.value.map(p => [p.id, pick(p)]))
  }))
})
async function compare() {
  loading.value = true
  try {
    profiles.value = await Promise.all(picked.value.map(id => supplierProfile(id)))
  } finally { loading.value = false }
}
onMounted(async () => {
  const page = await pageSuppliers({ page: 1, size: 20 })
  suppliers.value = (page.records || []).filter(s => s.status === 1)
  if (!picked.value.every(id => suppliers.value.some(s => s.id === id))) {
    picked.value = suppliers.value.slice(0, 2).map(s => s.id)
  }
  if (picked.value.length >= 2) compare()
})
</script>
<style scoped>
.pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 8px; }
.hint { color: var(--ip-text-secondary); font-size: 12px; margin: 8px 0 0; }
</style>
