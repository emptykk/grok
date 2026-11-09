<template>
  <div class="ip-page">
    <PageHeader title="工作台" sub="待办、金额趋势、品类结构与库存预警，数字来自实时汇总">
      <el-button @click="$router.push('/process')">流程总览</el-button>
      <el-button type="warning" :loading="reportLoading" @click="genReport">一键生成管理周报</el-button>
    </PageHeader>

    <el-skeleton v-if="loading" animated :rows="8" />
    <template v-else>
      <div class="stat-grid" v-auto-animate>
        <StatCard label="待一级审批" :value="summary.pendingL1 || 0" hint="部门主管待办" />
        <StatCard label="待二级审批" :value="summary.pendingL2 || 0" hint="采购负责人待办" tone="info" />
        <StatCard label="待三级审批" :value="summary.pendingL3 || 0" hint="金额达标后的财务终审" tone="warn" />
        <StatCard label="待收货订单" :value="summary.pendingReceive || 0" hint="仓管待验收" tone="danger" />
      </div>

      <div class="shortcuts" v-auto-animate>
        <button v-for="s in shortcuts" :key="s.path" type="button" class="shortcut" @click="$router.push(s.path)">
          <el-icon><component :is="s.icon" /></el-icon>
          <span>{{ s.title }}</span>
        </button>
      </div>

      <el-row :gutter="14" style="margin-top:14px">
        <el-col :xs="24" :lg="14">
          <div class="ip-card pad">
            <div class="sec-title">近六月采购金额 <small>{{ trendSource }}</small></div>
            <ChartBox v-if="hasTrend" :option="trendOption" height="300px" />
            <EmptyState v-else title="还没有可绘制的金额" description="完成几笔订单后，这里会按月汇总" />
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="ip-card pad">
            <div class="sec-title">品类金额占比</div>
            <ChartBox v-if="categories.length" :option="pieOption" height="300px" />
            <EmptyState v-else title="暂无品类数据" />
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="14" style="margin-top:14px">
        <el-col :xs="24" :lg="12">
          <div class="ip-card pad">
            <div class="sec-title">库存预警</div>
            <el-table :data="summary.inventoryAlerts || []" size="small" empty-text="暂无预警" class="ip-animate-list">
              <el-table-column prop="name" label="物资" />
              <el-table-column prop="quantity" label="当前" width="80" />
              <el-table-column prop="safeStock" label="安全" width="80" />
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :lg="12">
          <div class="ip-card pad">
            <div class="sec-title">最近单据动态</div>
            <ApprovalTimeline :items="timeline" />
            <el-button style="margin-top:8px" link type="primary" @click="$router.push('/process')">打开可交互流程总览</el-button>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="14" style="margin-top:14px">
        <el-col :xs="24" :lg="14">
          <div class="ip-card pad">
            <div class="sec-title">供应商采购额 TOP</div>
            <el-table :data="summary.supplierTop || []" size="small" empty-text="暂无订单">
              <el-table-column type="index" width="50" />
              <el-table-column prop="name" label="供应商" />
              <el-table-column prop="amount" label="金额" />
              <el-table-column label="画像" width="100">
                <template #default="{ row }">
                  <el-button v-if="row.supplierId" link type="primary" @click="$router.push(`/suppliers/${row.supplierId}/profile`)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="ip-card pad">
            <div class="sec-title">预算占用</div>
            <div v-for="b in summary.budgetPressure || []" :key="b.name" class="budget-row">
              <div class="b-name">{{ b.deptName }} · {{ b.name }}</div>
              <el-progress :percentage="Math.min(100, Number(b.ratio) || 0)" :stroke-width="8" :color="Number(b.ratio) >= 80 ? '#f59e0b' : '#0d9488'" />
            </div>
            <EmptyState v-if="!(summary.budgetPressure || []).length" title="暂无预算科目" />
          </div>
        </el-col>
      </el-row>
    </template>

    <el-dialog v-model="reportVisible" title="管理周报" width="760px" class="report-dialog">
      <article v-if="report" class="report">
        <p class="kicker">采购管理周报 · {{ report.source === 'llm' ? '大模型撰稿' : '规则汇总' }}</p>
        <h2>近 {{ report.metrics?.days || 7 }} 日采购运行简报</h2>
        <p class="lead">本报告由系统根据申请、订单、库存与合同到期自动整理。没有大模型 Key 时仍保留指标与规则说明。</p>
        <h3>一、本期指标</h3>
        <div class="metrics" v-if="report.metrics">
          <div><b>{{ report.metrics.newRequests }}</b><span>新申请</span></div>
          <div><b>{{ report.metrics.newOrders }}</b><span>新订单</span></div>
          <div><b>¥{{ report.metrics.orderAmount }}</b><span>订单金额</span></div>
          <div><b>{{ report.metrics.inventoryAlerts }}</b><span>库存预警</span></div>
          <div><b>{{ report.metrics.contractsExpiring30d }}</b><span>30 日到期合同</span></div>
        </div>
        <div v-if="riskLines.length" class="risk">
          <b>风险提示</b>
          <p v-for="(line, i) in riskLines" :key="i">{{ line }}</p>
        </div>
        <h3>二、分析正文</h3>
        <p v-for="(para, i) in reportParas" :key="i" class="para">{{ para }}</p>
      </article>
      <template #footer>
        <el-button @click="reportVisible=false">关闭</el-button>
        <el-button @click="downloadReport">导出 TXT</el-button>
        <el-button type="primary" @click="copyReport">复制全文</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboard, aiWeeklyReport } from '@/api'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { flatMenus } from '@/config/nav'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import ApprovalTimeline from '@/components/ApprovalTimeline.vue'
import EmptyState from '@/components/EmptyState.vue'

const store = useUserStore()
const theme = useThemeStore()
const summary = ref({})
const loading = ref(true)
const reportVisible = ref(false)
const reportLoading = ref(false)
const report = ref(null)
const reportText = ref('')
const reportParas = computed(() => String(reportText.value || '').split(/\n+/).map(s => s.trim()).filter(Boolean))
const riskLines = computed(() => {
  const m = report.value?.metrics || {}
  const lines = []
  if (Number(m.inventoryAlerts) > 0) lines.push(`库存低于安全线 ${m.inventoryAlerts} 项，建议仓管优先补货。`)
  if (Number(m.contractsExpiring30d) > 0) lines.push(`未来 30 天有 ${m.contractsExpiring30d} 份合同到期，请采购复核续签。`)
  return lines
})

const shortcuts = computed(() => flatMenus(store.roles).filter(m =>
  ['/requests', '/approvals', '/process', '/knowledge', '/anomaly', '/budgets', '/notifications', '/ops'].includes(m.path)
).slice(0, 8))

const trend = computed(() => summary.value.monthlyTrend || [])
const hasTrend = computed(() => trend.value.some(r => Number(r.amount) > 0))
const trendSource = computed(() => trend.value[0]?.source === 'request' ? '暂无订单，暂用申请预算' : '订单成交')
const categories = computed(() => (summary.value.categoryShare || []).map(c => ({ name: c.name, value: Number(c.value) })))
const timeline = computed(() => summary.value.recentFlow || [])

const trendOption = computed(() => {
  const dark = theme.dark
  return {
    color: ['#14b8a6'],
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: trend.value.map(r => r.month), axisLine: { lineStyle: { color: '#94a3b8' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: dark ? '#1f2a3a' : '#e2e8f0' } } },
    series: [{
      type: 'line', smooth: true, data: trend.value.map(r => Number(r.amount) || 0),
      areaStyle: { color: 'rgba(20,184,166,0.18)' },
      symbol: 'circle', symbolSize: 8
    }]
  }
})
const pieOption = computed(() => {
  const dark = theme.dark
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: dark ? '#94a3b8' : '#64748b' } },
    series: [{
      type: 'pie', radius: ['42%', '68%'],
      itemStyle: { borderRadius: 6, borderColor: dark ? '#111827' : '#fff', borderWidth: 2 },
      label: { color: dark ? '#e5e7eb' : '#0f172a' },
      data: categories.value
    }]
  }
})

async function genReport() {
  reportLoading.value = true
  try {
    report.value = await aiWeeklyReport(7)
    reportText.value = report.value.reportText || ''
    reportVisible.value = true
  } catch (e) {
    ElMessage.error(e?.message || '周报生成失败')
  } finally { reportLoading.value = false }
}
function copyReport() {
  navigator.clipboard?.writeText(reportText.value || '')
  ElMessage.success('已复制周报')
}
function downloadReport() {
  const blob = new Blob([reportText.value || ''], { type: 'text/plain;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `采购管理周报-${new Date().toISOString().slice(0,10)}.txt`
  a.click()
  URL.revokeObjectURL(a.href)
}

onMounted(async () => {
  try { summary.value = await getDashboard() } catch { summary.value = {} }
  finally { loading.value = false }
})
</script>
<style scoped>
.sec-title { font-weight: 700; margin-bottom: 10px; letter-spacing: -0.01em; }
.sec-title small { font-weight: 500; color: var(--ip-text-secondary); margin-left: 8px; }
.pad { padding: 16px; }
.shortcuts { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 14px; }
.shortcut {
  display: flex; align-items: center; gap: 8px;
  border: 1px solid var(--ip-border); background: var(--ip-surface); color: var(--ip-text);
  border-radius: 999px; padding: 8px 14px; cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease;
}
.shortcut:hover { transform: translateY(-2px); border-color: var(--ip-accent); color: var(--ip-accent); }
.budget-row { margin-bottom: 10px; }
.b-name { font-size: 12px; color: var(--ip-text-secondary); margin-bottom: 4px; }
.report { color: var(--ip-text); }
.kicker { letter-spacing: .14em; font-size: 12px; color: var(--ip-accent); font-weight: 700; margin: 0; }
.report h2 { margin: 6px 0 8px; letter-spacing: -0.03em; }
.lead, .para { line-height: 1.7; font-size: 14px; }
.metrics { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; margin: 10px 0 14px; }
.metrics div { background: var(--ip-surface-2); border-radius: 10px; padding: 10px; }
.metrics b { display: block; font-size: 16px; }
.metrics span { color: var(--ip-text-secondary); font-size: 12px; }
.risk { border-left: 3px solid var(--ip-warning); background: rgba(245, 158, 11, .12); padding: 10px 12px; border-radius: 8px; margin-bottom: 12px; }
.risk p { margin: 4px 0 0; }
@media (max-width: 800px) { .metrics { grid-template-columns: 1fr 1fr; } }
</style>
