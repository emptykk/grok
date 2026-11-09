<template>
  <div class="ip-page">
    <PageHeader :title="profile.name || '供应商画像'" :sub="profile.category || '加载中'">
      <el-button @click="$router.push('/suppliers')">返回名录</el-button>
    </PageHeader>
    <el-skeleton v-if="loading" animated :rows="6" />
    <template v-else>
      <div class="tags">
        <el-tag v-for="t in profile.riskTags || []" :key="t.code" :type="tagType(t.level)" effect="dark">{{ t.label }}</el-tag>
        <span v-if="profile.amountShare" class="share">成交占比 {{ profile.amountShare }}%</span>
      </div>
      <el-row :gutter="14">
        <el-col :xs="24" :md="8">
          <div class="ip-card pad">
            <div class="big">{{ profile.rating ?? '-' }} <span class="unit">星</span></div>
            <div class="score">准入分 {{ profile.admissionScore ?? '未评' }}</div>
            <div class="meta">{{ profile.contact }}</div>
            <div class="meta">{{ profile.address }}</div>
            <el-divider />
            <div class="kv"><span>合作年限</span><b>{{ profile.cooperationYears || 0 }} 年</b></div>
            <div class="kv"><span>历史订单</span><b>{{ profile.orderCount || 0 }}</b></div>
            <div class="kv"><span>累计金额</span><b>¥ {{ Number(profile.totalAmount || 0).toLocaleString() }}</b></div>
            <div class="kv"><span>准时率</span><b>{{ pct(profile.onTimeRate) }}</b></div>
            <div class="kv"><span>合格率</span><b>{{ pct(profile.qualifyRate) }}</b></div>
            <p class="meta">准时率按入库时间对比订单期望到货日计算，样本 {{ profile.onTimeSample || 0 }} 单。合格率按关联退换货单折算。</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="16">
          <div class="ip-card pad" style="margin-bottom:14px">
            <div class="sec">能力雷达</div>
            <ChartBox :option="radarOption" height="320px" />
            <p class="meta">实心点为评分记录均值；没有该维度记录时，用准入分作演示基线，并在图例中区分。</p>
          </div>
          <div class="ip-card pad" style="margin-bottom:14px">
            <div class="sec">历史摘要</div>
            <p class="summary">{{ profile.historySummary }}</p>
          </div>
          <div class="ip-card pad">
            <div class="sec">近期订单</div>
            <el-table :data="profile.recentOrders || []" size="small" empty-text="暂无订单">
              <el-table-column prop="no" label="单号" />
              <el-table-column prop="amount" label="金额" />
              <el-table-column prop="date" label="日期" />
              <el-table-column prop="status" label="状态" />
            </el-table>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { supplierProfile } from '@/api'
import { useThemeStore } from '@/stores/theme'
import PageHeader from '@/components/PageHeader.vue'
import ChartBox from '@/components/ChartBox.vue'

const route = useRoute()
const theme = useThemeStore()
const profile = ref({})
const loading = ref(true)
const pct = (v) => v == null ? '样本不足' : Math.round(Number(v) * 100) + '%'
function tagType(level) {
  if (level === 'high') return 'danger'
  if (level === 'mid') return 'warning'
  return 'success'
}
const radarOption = computed(() => {
  const dark = theme.dark
  const radar = profile.value.radar || []
  return {
    tooltip: {},
    radar: {
      indicator: radar.map(i => ({ name: i.measured ? i.name : i.name + '*', max: 100 })),
      splitArea: { areaStyle: { color: dark ? ['rgba(20,184,166,0.04)', 'rgba(20,184,166,0.1)'] : ['rgba(13,148,136,0.04)', 'rgba(13,148,136,0.1)'] } },
      axisName: { color: dark ? '#cbd5e1' : '#334155' }
    },
    series: [{
      type: 'radar',
      data: [{
        value: radar.map(i => Number(i.value)),
        name: profile.value.name || '评分',
        areaStyle: { color: 'rgba(20,184,166,0.28)' },
        lineStyle: { color: '#14b8a6' }
      }]
    }]
  }
})
onMounted(async () => {
  try { profile.value = await supplierProfile(route.params.id) }
  finally { loading.value = false }
})
</script>
<style scoped>
.pad { padding: 16px; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 12px; }
.share { font-size: 12px; color: var(--ip-text-secondary); }
.big { font-size: 40px; font-weight: 800; letter-spacing: -0.03em; color: var(--ip-accent); }
.unit, .score, .meta { color: var(--ip-text-secondary); font-size: 13px; }
.score { margin-top: 4px; }
.meta { margin-top: 6px; line-height: 1.5; }
.kv { display:flex; justify-content:space-between; padding: 8px 0; border-bottom: 1px dashed var(--ip-border); font-size: 13px; }
.sec { font-weight: 700; margin-bottom: 10px; }
.summary { margin: 0; line-height: 1.7; }
</style>
