<template>
  <div class="ip-page">
    <PageHeader title="流程总览" sub="点击节点查看该状态的单据数量，并可跳进对应业务页">
      <el-button :loading="loading" @click="load">刷新</el-button>
    </PageHeader>
    <el-skeleton v-if="loading && !nodes.length" animated :rows="4" />
    <div v-else class="ip-card pad">
      <div class="rail">
        <template v-for="(s, idx) in mainNodes" :key="s.key">
          <button type="button" class="process-node clickable" :class="{ active: activeKey === s.key, hot: s.count > 0 }" @click="select(s)">
            <div class="n">{{ String(idx + 1).padStart(2, '0') }}</div>
            <div class="l">{{ s.label }}</div>
            <div class="c">{{ s.count }}</div>
          </button>
          <div v-if="idx < mainNodes.length - 1" class="process-arrow">→</div>
        </template>
      </div>
      <div class="side-nodes">
        <button v-for="s in sideNodes" :key="s.key" type="button" class="side" :class="{ active: activeKey === s.key }" @click="select(s)">
          {{ s.label }} <b>{{ s.count }}</b>
        </button>
      </div>
    </div>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :md="14" :xs="24">
        <div class="ip-card pad">
          <div class="sec">{{ active?.label || '节点' }} · {{ active?.count || 0 }} 单</div>
          <p class="desc">{{ describe(active?.key) }}</p>
          <el-table :data="active?.samples || []" size="small" empty-text="该状态暂无单据" class="ip-animate-list" @row-click="openSample">
            <el-table-column prop="requestNo" label="单号" width="160" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="deptName" label="部门" width="100" />
            <el-table-column prop="budgetAmount" label="预算" width="100" />
          </el-table>
          <div style="margin-top:12px;display:flex;gap:8px;flex-wrap:wrap">
            <el-button type="primary" :disabled="!active" @click="jump()">进入该环节</el-button>
            <el-button v-if="firstSample" @click="openSample(firstSample)">看第一条轨迹</el-button>
            <el-button v-if="firstSample" type="warning" plain @click="$router.push('/trace/' + firstSample.id)">讲这条单的完整故事</el-button>
          </div>
        </div>
      </el-col>
      <el-col :md="10" :xs="24">
        <div class="ip-card pad">
          <div class="sec">{{ focusTitle }}</div>
          <ApprovalTimeline :items="timeline" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProcessBoard, getApprovalTimeline } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import ApprovalTimeline from '@/components/ApprovalTimeline.vue'

const router = useRouter()
const loading = ref(false)
const nodes = ref([])
const activeKey = ref('PENDING_L1')
const timeline = ref([])
const focusTitle = ref('选择一条单据查看审批轨迹')

const mainOrder = ['DRAFT', 'PENDING_L1', 'PENDING_L2', 'PENDING_L3', 'PENDING_INQUIRY', 'PENDING_ORDER', 'PENDING_RECEIVE', 'COMPLETED']
const mainNodes = computed(() => mainOrder.map(k => nodes.value.find(n => n.key === k)).filter(Boolean))
const sideNodes = computed(() => nodes.value.filter(n => n.key === 'REJECTED' || n.key === 'CANCELLED'))
const active = computed(() => nodes.value.find(n => n.key === activeKey.value))
const firstSample = computed(() => active.value?.samples?.[0])

const copy = {
  DRAFT: '申请人保存但未提交。点进去可继续编辑。',
  PENDING_L1: '部门主管待办。李四登录后在「我的审批」处理。',
  PENDING_L2: '采购负责人待办。王五处理二级审批。',
  PENDING_L3: '金额达到三级规则后，由财务终审。',
  PENDING_INQUIRY: '审批已通过，采购负责人录入报价并比价。',
  PENDING_ORDER: '供应商已选定，等待生成采购订单。',
  PENDING_RECEIVE: '订单在途，仓管按单验收。',
  COMPLETED: '入库闭环完成，可回看金额与画像。',
  REJECTED: '审批驳回，申请人可修改后重提。',
  CANCELLED: '申请人撤回或取消。'
}
function describe(key) { return copy[key] || '采购主流程节点' }

function select(node) {
  activeKey.value = node.key
  if (node.samples?.[0]) openSample(node.samples[0])
  else { timeline.value = []; focusTitle.value = '该状态暂无单据' }
}
function jump(sample) {
  const key = active.value?.key
  const id = sample?.id || firstSample.value?.id
  if ((key === 'PENDING_INQUIRY' || key === 'PENDING_ORDER') && id) return router.push('/inquiry/' + id)
  if (key === 'PENDING_RECEIVE') return router.push('/inbound')
  if (['PENDING_L1', 'PENDING_L2', 'PENDING_L3'].includes(key)) return router.push('/approvals')
  router.push({ path: '/requests', query: key ? { status: key } : {} })
}
async function openSample(row) {
  focusTitle.value = `${row.requestNo} · ${row.title}`
  try { timeline.value = await getApprovalTimeline(row.id) } catch { timeline.value = [] }
}
async function load() {
  loading.value = true
  try {
    const data = await getProcessBoard()
    nodes.value = data.nodes || []
    if (!nodes.value.some(n => n.key === activeKey.value)) activeKey.value = nodes.value[0]?.key
    if (firstSample.value) await openSample(firstSample.value)
  } finally { loading.value = false }
}
onMounted(load)
</script>
<style scoped>
.pad { padding: 18px; }
.sec { font-weight: 700; margin-bottom: 8px; }
.desc { color: var(--ip-text-secondary); font-size: 13px; margin-top: 0; }
.rail { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.clickable { cursor: pointer; min-width: 108px; background: var(--ip-surface); color: inherit; font: inherit; }
.clickable.hot .c { color: var(--ip-accent); }
.c { font-size: 22px; font-weight: 800; letter-spacing: -0.03em; margin-top: 4px; }
.n { font-size: 11px; color: var(--ip-text-secondary); letter-spacing: .08em; }
.l { font-weight: 700; margin-top: 2px; }
.side-nodes { display: flex; gap: 8px; margin-top: 14px; }
.side {
  border: 1px dashed var(--ip-border); background: transparent; color: var(--ip-text);
  border-radius: 999px; padding: 6px 12px; cursor: pointer;
}
.side.active { border-color: var(--ip-danger); color: var(--ip-danger); }
</style>
