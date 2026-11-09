<template>
  <div class="ip-page">
    <PageHeader title="异常采购解读" sub="供应商集中、预算占用、金额突增与黑名单成交。无 Key 时规则降级。">
      <el-select v-model="months" style="width:120px" @change="load">
        <el-option :value="1" label="近 1 个月" />
        <el-option :value="3" label="近 3 个月" />
        <el-option :value="6" label="近 6 个月" />
      </el-select>
      <el-button :loading="loading" @click="load">重新分析</el-button>
    </PageHeader>
    <el-skeleton v-if="loading && !report" animated :rows="6" />
    <template v-else-if="report">
      <div class="meta">
        <el-tag :type="report.source === 'llm' ? 'success' : 'warning'">{{ report.source === 'llm' ? '大模型解读' : '规则降级' }}</el-tag>
        <span>申请 {{ report.requestCount ?? '-' }} · 订单 {{ report.orderCount ?? '-' }} · 总额 ¥{{ Number(report.orderTotal || 0).toLocaleString() }}</span>
      </div>
      <div class="grid" v-auto-animate>
        <div v-for="a in findings" :key="a.id" class="ip-card lift pad">
          <div class="top">
            <el-tag :type="a.level === '高' ? 'danger' : (a.level === '中' ? 'warning' : 'info')" effect="dark">{{ a.level }}</el-tag>
            <span class="metric">{{ a.metric }}</span>
          </div>
          <h3>{{ a.title }}</h3>
          <p class="desc">{{ a.desc }}</p>
          <p class="advice">建议：{{ a.advice }}</p>
          <el-button v-if="jumpOf(a)" type="primary" link @click="$router.push(jumpOf(a).path)">{{ jumpOf(a).label }}</el-button>
        </div>
      </div>
      <el-row :gutter="14" style="margin-top:14px">
        <el-col :xs="24" :lg="14">
          <div class="ip-card pad">
            <div class="sec">分析说明</div>
            <div class="ans">{{ report.analysis }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="ip-card pad">
            <div class="sec">供应商金额分布</div>
            <ChartBox v-if="bars.length" :option="barOption" height="260px" />
            <EmptyState v-else title="窗口内没有订单" />
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { aiAnomaly, pageSuppliers } from '@/api'
import { useThemeStore } from '@/stores/theme'
import PageHeader from '@/components/PageHeader.vue'
import ChartBox from '@/components/ChartBox.vue'
import EmptyState from '@/components/EmptyState.vue'

const theme = useThemeStore()
const months = ref(6)
const loading = ref(false)
const report = ref(null)
const suppliers = ref([])
const findings = computed(() => report.value?.findings || [])
function jumpOf(a) {
  const id = String(a?.id || '')
  if (id.startsWith('budget')) return { path: '/budgets', label: '打开预算' }
  if (id === 'concentration' || id === 'blacklist') return { path: '/suppliers', label: '打开供应商' }
  if (id === 'spike') return { path: '/orders', label: '打开订单' }
  if (id === 'calm') return { path: '/dashboard', label: '回工作台' }
  return null
}
const bars = computed(() => {
  const map = report.value?.supplierAmountMap || {}
  return Object.entries(map).map(([id, amount]) => ({
    name: suppliers.value.find(s => String(s.id) === String(id))?.name || ('#' + id),
    amount: Number(amount)
  }))
})
const barOption = computed(() => ({
  color: ['#14b8a6'],
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 12, top: 16, bottom: 48 },
  xAxis: { type: 'category', data: bars.value.map(b => b.name), axisLabel: { color: theme.dark ? '#94a3b8' : '#64748b', interval: 0, rotate: 20 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: theme.dark ? '#1f2a3a' : '#e2e8f0' } } },
  series: [{ type: 'bar', data: bars.value.map(b => b.amount), barWidth: 22, itemStyle: { borderRadius: [6, 6, 0, 0] } }]
}))
async function load() {
  loading.value = true
  try { report.value = await aiAnomaly(months.value) }
  finally { loading.value = false }
}
onMounted(async () => {
  try {
    const page = await pageSuppliers({ page: 1, size: 50 })
    suppliers.value = page.records || []
  } catch { suppliers.value = [] }
  load()
})
</script>
<style scoped>
.meta { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; color: var(--ip-text-secondary); font-size: 13px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.pad { padding: 16px; }
.top { display: flex; justify-content: space-between; align-items: center; }
.metric { font-weight: 800; color: var(--ip-accent); font-size: 18px; }
h3 { margin: 10px 0 8px; font-size: 16px; }
.desc, .advice { font-size: 13px; color: var(--ip-text-secondary); line-height: 1.6; }
.advice { color: var(--ip-text); }
.sec { font-weight: 700; margin-bottom: 10px; }
.ans { white-space: pre-wrap; line-height: 1.7; font-size: 14px; }
</style>
