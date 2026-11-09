<template>
  <div class="ip-page">
    <PageHeader title="我的审批" sub="待办列表 + 审批助手倾向建议 + 流程时间轴" />
    <el-row :gutter="14">
      <el-col :xs="24" :lg="16">
        <div class="ip-card" style="padding:8px">
          <el-table :data="rows" stripe>
            <el-table-column prop="requestNo" label="单号" width="180" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="deptName" label="部门" width="100" />
            <el-table-column prop="budgetAmount" label="预算" width="100" />
            <el-table-column label="状态" width="120">
              <template #default="{row}">{{ statusLabel(row.status) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="280">
              <template #default="{row}">
                <el-button type="success" size="small" @click="open(row,'PASS')">通过</el-button>
                <el-button type="danger" size="small" @click="open(row,'REJECT')">驳回</el-button>
                <el-button link type="warning" @click="loadAssist(row)">审批助手</el-button>
                <el-button link type="primary" @click="preview = row">时间轴</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="ip-card" style="padding:16px;margin-bottom:14px">
          <div style="font-weight:700;margin-bottom:12px">审批进度</div>
          <ApprovalTimeline :items="timeline" />
          <el-button style="margin-top:8px" link type="primary" @click="$router.push('/process')">打开流程总览</el-button>
        </div>
        <div class="ip-card" style="padding:16px" v-loading="assistLoading">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
            <div style="font-weight:700">审批助手</div>
            <el-tag v-if="assist" size="small" :type="tendencyType">{{ tendencyLabel }}</el-tag>
          </div>
          <template v-if="assist">
            <div class="muted">置信度 {{ Math.round((assist.confidence || 0) * 100) }}% · 来源 {{ assist.source === 'llm' ? '大模型' : '规则' }}</div>
            <ul class="reasons">
              <li v-for="(r,i) in (assist.reasons || [])" :key="i">{{ r }}</li>
            </ul>
            <div class="suggest">{{ assist.suggestedComment }}</div>
            <el-button type="primary" size="small" style="margin-top:10px" @click="useAssistComment">填入审批意见并打开</el-button>
          </template>
          <EmptyState v-else title="尚未运行审批助手" description="点待办行的「审批助手」查看同意/驳回倾向与依据" />
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="visible" :title="action==='PASS'?'审批通过':'审批驳回'" width="520px">
      <el-alert v-if="assist && current && assist.requestId === current.id" :closable="false" show-icon
        :type="tendencyType === 'danger' ? 'error' : (tendencyType === 'warning' ? 'warning' : 'success')"
        :title="`助手倾向：${tendencyLabel}`"
        style="margin-bottom:10px" />
      <el-input v-model="comment" type="textarea" :rows="4" placeholder="审批意见" />
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button v-if="assist?.suggestedComment" @click="comment = assist.suggestedComment">用助手意见</el-button>
        <el-button type="primary" @click="confirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { approvalTodo, doApprove, aiApprovalAssist, getApprovalTimeline } from '@/api'
import { statusLabel } from '@/utils/status'
import { ElMessage } from 'element-plus'
import { burstConfetti } from '@/utils/confetti'
import PageHeader from '@/components/PageHeader.vue'
import ApprovalTimeline from '@/components/ApprovalTimeline.vue'
import EmptyState from '@/components/EmptyState.vue'

const rows = ref([])
const visible = ref(false)
const comment = ref('')
const action = ref('PASS')
const current = ref(null)
const preview = ref(null)
const timeline = ref([])
const assist = ref(null)
const assistLoading = ref(false)

const tendencyLabel = computed(() => {
  const t = assist.value?.tendency
  if (t === 'PASS') return '建议同意'
  if (t === 'REJECT') return '建议驳回'
  if (t === 'REVIEW') return '建议复核'
  return '—'
})
const tendencyType = computed(() => {
  const t = assist.value?.tendency
  if (t === 'PASS') return 'success'
  if (t === 'REJECT') return 'danger'
  return 'warning'
})

async function loadTimeline(row) {
  if (!row?.id) { timeline.value = []; return }
  try { timeline.value = await getApprovalTimeline(row.id) } catch { timeline.value = [] }
}
watch(preview, (row) => loadTimeline(row))

async function load() {
  rows.value = await approvalTodo()
  if (rows.value?.length) preview.value = rows.value[0]
}
function open(row, act) {
  current.value = row; action.value = act
  comment.value = (assist.value && assist.value.requestId === row.id && assist.value.suggestedComment) ? assist.value.suggestedComment : ''
  visible.value = true
}
async function loadAssist(row) {
  preview.value = row
  current.value = row
  assistLoading.value = true
  try {
    assist.value = await aiApprovalAssist(row.id)
    ElMessage.success('审批助手已更新')
  } catch (e) {
    ElMessage.error(e?.message || '审批助手失败')
  } finally { assistLoading.value = false }
}
function useAssistComment() {
  if (!assist.value || !current.value) return
  action.value = assist.value.tendency === 'REJECT' ? 'REJECT' : 'PASS'
  comment.value = assist.value.suggestedComment || ''
  visible.value = true
}
async function confirm() {
  await doApprove(current.value.id, { result: action.value, comment: comment.value })
  ElMessage.success('审批完成')
  burstConfetti()
  visible.value = false
  load()
}
onMounted(load)
</script>
<style scoped>
.muted { color: var(--el-text-color-secondary); font-size: 12px; margin-bottom: 8px; }
.reasons { margin: 0 0 10px 18px; padding: 0; font-size: 13px; line-height: 1.55; }
.suggest { background: var(--el-fill-color-light); border-radius: 8px; padding: 10px; font-size: 13px; line-height: 1.55; }
</style>
