<template>
  <div v-if="open" class="tour">
    <div class="card">
      <div class="kicker">答辩导览 {{ index + 1 }} / {{ steps.length }} <span v-if="seen">· 已看过，可重播</span></div>
      <div class="track"><i :style="{ width: ((index + 1) / steps.length * 100) + '%' }" /></div>
      <h3>{{ current.title }}</h3>
      <p>{{ current.desc }}</p>
      <div class="actions">
        <el-button text @click="skip">跳过并记住</el-button>
        <el-button :disabled="index === 0" @click="index--">上一步</el-button>
        <el-button v-if="current.path" @click="go">前往该页</el-button>
        <el-button type="primary" @click="next">{{ index === steps.length - 1 ? '完成' : '下一步' }}</el-button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const KEY = 'ip-tour-seen-v2'
const router = useRouter()
const user = useUserStore()
const open = ref(false)
const index = ref(0)
const seen = ref(localStorage.getItem(KEY) === '1')

const steps = computed(() => {
  const admin = (user.roles || []).includes('ADMIN')
  const list = [
    { title: '工作台', desc: 'KPI、金额趋势、品类占比和预算占用都来自汇总接口。骨架屏结束后数字会滚入。右上角可生成管理周报。', path: '/dashboard' },
    { title: '流程总览', desc: '每个节点都有样例数量，包括已取消。点节点看单据，再跳到申请、审批、询价或入库。', path: '/process' },
    { title: '审批轨迹', desc: '李四、王五、财务各自看到待办。右侧时间轴是这一单的真实审批记录，不是示意图。', path: '/approvals' },
    { title: '供应商画像', desc: '打开华信：雷达图、采购额集中标签、历史订单。带星号的维度表示用准入分作基线。', path: '/suppliers/1/profile' },
    { title: '运行中心', desc: admin ? '手动触发库存预警和合同到期扫描，查看健康、任务记录和大模型 Key 脱敏状态。' : '运行中心仅管理员可见。换 admin 可演示任务触发与审计摘要。', path: admin ? '/ops' : '/dashboard' },
    { title: '大模型配置', desc: admin ? '在这里填写 DeepSeek。不填 Key 时，周报、审批助手、比价和制度问答都按规则降级，主流程不受影响。' : '大模型配置在管理员菜单。无 Key 时全站智能能力规则降级。', path: admin ? '/ai-settings' : '/ai' }
  ]
  return list
})
const current = computed(() => steps.value[index.value] || steps.value[0])

function markSeen() {
  localStorage.setItem(KEY, '1')
  seen.value = true
}
function start(force = false) {
  if (!force && seen.value) return
  index.value = 0
  open.value = true
}
function skip() { markSeen(); open.value = false }
function next() {
  if (index.value >= steps.value.length - 1) { markSeen(); open.value = false }
  else index.value++
}
function go() {
  if (current.value?.path) router.push(current.value.path)
}
defineExpose({ start, seen })
</script>
<style scoped>
.tour { position: fixed; right: 20px; bottom: 20px; z-index: 2500; width: min(400px, calc(100vw - 32px)); }
.card {
  background: var(--ip-surface);
  border: 1px solid var(--ip-accent);
  border-radius: 16px;
  padding: 16px 16px 12px;
  box-shadow: 0 16px 50px rgba(13, 148, 136, 0.28);
}
.kicker { font-size: 12px; letter-spacing: .06em; color: var(--ip-accent); font-weight: 700; }
.track { height: 3px; background: var(--ip-border); border-radius: 99px; margin: 8px 0 4px; overflow: hidden; }
.track i { display: block; height: 100%; background: linear-gradient(90deg, #0d9488, #5eead4); }
h3 { margin: 8px 0 4px; }
p { margin: 0 0 12px; color: var(--ip-text-secondary); font-size: 13px; line-height: 1.6; }
.actions { display: flex; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }
</style>
