<template>
  <div class="ip-page">
    <PageHeader title="答辩演示脚本" :sub="demoSub">
      <el-button @click="$router.push('/defense-brief')">答辩一页纸</el-button>
      <el-button @click="openHelp">快捷键 ?</el-button>
    </PageHeader>
    <div class="round">第四轮：时间机器、供应商对比、预算仪表、快捷键</div>
    <div class="steps">
      <button v-for="(s, i) in steps" :key="s.title" type="button" class="step" :style="{ animationDelay: (i * 45) + 'ms' }" @click="go(s)">
        <span class="idx">{{ String(i + 1).padStart(2, '0') }}</span>
        <span class="body">
          <b>{{ s.title }}</b>
          <small>{{ s.who }} · {{ s.path }}</small>
          <em>{{ s.say }}</em>
        </span>
        <el-icon><ArrowRight /></el-icon>
      </button>
    </div>
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const demoSub = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS === 'false'
  ? '约 12–15 分钟。公网口令由部署环境单独配置，页面不展示。第四轮入口在最上面，按 ? 打开快捷键。'
  : '约 12–15 分钟。本地演示口令见 README。第四轮入口在最上面，按 ? 可随时打开快捷键。'
function openHelp() { window.dispatchEvent(new CustomEvent('ip-shortcuts')) }
function go(s) {
  if (s.help) openHelp()
  else router.push(s.path)
}
const steps = [
  { title: '答辩一页纸', who: 'admin 开场', path: '/defense-brief', say: '技术栈、页面数、6 个 AI 嵌入点、三级数据权限。' },
  { title: '全链路时间机器', who: 'admin', path: '/trace/1', say: '第四轮。笔记本补货单从申请讲到发票付款。' },
  { title: '供应商对比', who: 'wangwu', path: '/suppliers/compare', say: '第四轮。勾选华信与宏达，并排雷达。' },
  { title: '预算仪表', who: 'admin 或 caiwu', path: '/budgets', say: '第四轮。信息中心超 80%，下钻到投影申请。' },
  { title: '快捷键面板', who: '任意', help: true, path: '?', say: '第四轮。点这一条或按键盘 ?，列出 Ctrl+K、导览和常用跳转。' },
  { title: '工作台真数', who: 'admin', path: '/dashboard', say: 'KPI、近六月金额、品类占比、预算占用，并生成管理周报。' },
  { title: '采购全流程', who: 'admin', path: '/process', say: '每个状态都有样例数，点节点看审批轨迹。' },
  { title: '审批规则阶梯', who: '任意', path: '/approval-rules', say: '金额阈值决定一级、二级还是财务三级。试算 68000。' },
  { title: '审批时间轴', who: 'lisi / wangwu / caiwu', path: '/approvals', say: '待办右侧是这一单的真实轨迹，可跑审批助手。' },
  { title: '供应商雷达', who: 'wangwu', path: '/suppliers/1/profile', say: '华信画像、风险标签、历史订单。' },
  { title: '比价最低价', who: 'wangwu', path: '/inquiry/9', say: '最低价高亮，打印预览比价表。' },
  { title: '制度问答', who: '任意', path: '/knowledge', say: '问超过 1 万元要几级审批，看引用卡片。' },
  { title: '异常解读', who: 'wangwu', path: '/anomaly', say: '集中度与预算卡片可跳到供应商或预算页。' },
  { title: '运行中心', who: 'admin', path: '/ops', say: '手动跑库存预警、看大模型 Key 是否脱敏。' },
  { title: '演示账号与范围', who: '当前用户', path: '/profile', say: '角色、SELF/DEPT/ALL、最近操作。' }
]
</script>
<style scoped>
.round { font-size: 12px; color: var(--ip-accent); font-weight: 700; margin: -6px 0 10px; letter-spacing: .04em; }
.steps { display: flex; flex-direction: column; gap: 10px; }
.step {
  display: flex; align-items: center; gap: 14px; text-align: left;
  border: 1px solid var(--ip-border); background: var(--ip-surface); color: inherit;
  border-radius: 14px; padding: 14px 16px; cursor: pointer;
  animation: rise .4s ease both;
}
.step:hover { border-color: var(--ip-accent); transform: translateX(4px); }
.idx { font-weight: 800; color: var(--ip-accent); font-size: 18px; width: 36px; }
.body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.body small, .body em { color: var(--ip-text-secondary); font-style: normal; font-size: 12px; }
@keyframes rise { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
</style>
