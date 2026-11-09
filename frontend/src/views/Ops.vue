<template>
  <div class="ip-page">
    <PageHeader title="系统运行中心" sub="定时任务、健康检查、大模型脱敏状态与审计摘要。仅管理员。">
      <el-button :loading="loading" @click="load">刷新</el-button>
    </PageHeader>
    <el-skeleton v-if="loading && !data" animated :rows="6" />
    <template v-else-if="data">
      <div class="stat-grid">
        <StatCard label="服务状态" :value="data.health?.status || '-'" :hint="`数据库 ${data.health?.db} · ${data.health?.dbProduct || ''}`" />
        <StatCard label="运行秒数" :value="data.health?.uptimeSeconds || 0" :hint="`Java ${data.health?.java} · ${data.health?.profile}`" tone="info" />
        <StatCard label="今日审计" :value="data.audit?.todayCount || 0" hint="操作日志条数" tone="warn" />
        <StatCard label="失败记录" :value="data.audit?.failCount || 0" hint="success = 0 的历史日志" tone="danger" />
      </div>

      <el-row :gutter="14" style="margin-top:14px">
        <el-col :xs="24" :lg="14">
          <div class="ip-card pad">
            <div class="sec">定时任务</div>
            <div v-for="job in data.jobCatalog || []" :key="job.code" class="job">
              <div>
                <b>{{ job.name }}</b>
                <div class="muted">{{ job.cron }} · {{ job.endpoint }}</div>
                <div class="muted" v-if="job.lastAt">最近 {{ job.lastTrigger }} · {{ job.lastStatus }} · {{ job.lastSummary }}</div>
              </div>
              <el-button type="primary" :loading="running === job.code" @click="run(job.code)">立即执行</el-button>
            </div>
            <el-table :data="data.jobs || []" size="small" style="margin-top:12px" empty-text="还没有执行记录">
              <el-table-column prop="createdAt" label="时间" width="170" />
              <el-table-column prop="jobName" label="任务" />
              <el-table-column prop="triggerType" label="触发" width="100" />
              <el-table-column prop="status" label="结果" width="90" />
              <el-table-column prop="summary" label="摘要" />
              <el-table-column prop="durationMs" label="ms" width="70" />
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="ip-card pad" style="margin-bottom:14px">
            <div class="sec">大模型配置（脱敏）</div>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="状态">{{ data.ai?.configured ? '已配置' : '未配置，规则降级' }}</el-descriptions-item>
              <el-descriptions-item label="模型">{{ data.ai?.model || '-' }}</el-descriptions-item>
              <el-descriptions-item label="地址">{{ data.ai?.baseUrl || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Key">{{ data.ai?.apiKeyMasked || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="来源">{{ data.ai?.source }}</el-descriptions-item>
            </el-descriptions>
            <el-button style="margin-top:10px" @click="$router.push('/ai-settings')">去配置</el-button>
          </div>
          <div class="ip-card pad" style="margin-bottom:14px">
            <div class="sec">导入导出入口</div>
            <div class="io">
              <el-button size="small" @click="$router.push('/requests')">申请 Excel</el-button>
              <el-button size="small" @click="$router.push('/budgets')">预算 Excel</el-button>
              <el-button size="small" @click="$router.push('/inquiry/9')">比价 Excel / 打印</el-button>
              <el-button size="small" @click="$router.push('/materials')">物资导入导出</el-button>
              <el-button size="small" @click="$router.push('/suppliers')">供应商导出</el-button>
              <el-button size="small" @click="$router.push('/orders')">订单导出</el-button>
              <el-button size="small" @click="$router.push('/logs')">日志 CSV</el-button>
              <el-button size="small" @click="$router.push('/knowledge')">制度上传</el-button>
            </div>
          </div>
          <div class="ip-card pad" style="margin-bottom:14px">
            <div class="sec">可配置审批</div>
            <p class="muted">金额阶梯决定一级、二级或财务三级，规则存在数据库里。</p>
            <el-button type="primary" plain @click="$router.push('/approval-rules')">打开审批规则阶梯</el-button>
          </div>
          <div class="ip-card pad">
            <div class="sec">审计摘要</div>
            <div v-for="m in data.audit?.byModule || []" :key="m.module" class="mod">
              <span>{{ m.module }}</span><b>{{ m.count }}</b>
            </div>
            <el-button link type="primary" @click="$router.push('/logs')">打开操作日志</el-button>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOpsOverview, runInventoryJob, runContractJob } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'

const data = ref(null)
const loading = ref(false)
const running = ref('')
async function load() {
  loading.value = true
  try { data.value = await getOpsOverview() }
  finally { loading.value = false }
}
async function run(code) {
  running.value = code
  try {
    const res = code === 'inventory-alert' ? await runInventoryJob() : await runContractJob()
    ElMessage.success('执行完成 ' + JSON.stringify(res))
    await load()
  } finally { running.value = '' }
}
onMounted(load)
</script>
<style scoped>
.pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 12px; }
.job { display:flex; justify-content:space-between; gap:12px; align-items:center; padding: 10px 0; border-bottom: 1px solid var(--ip-border); }
.muted { color: var(--ip-text-secondary); font-size: 12px; margin-top: 2px; }
.io { display: flex; flex-wrap: wrap; gap: 8px; }
.mod { display:flex; justify-content:space-between; padding: 6px 0; font-size: 13px; }
</style>
