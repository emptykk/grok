<template>
  <div class="ip-page">
    <PageHeader title="答辩一页纸" sub="给老师的工作量底稿。数字来自当前菜单与已落地的能力，不是宣传口径。">
      <el-button @click="$router.push('/demo-script')">按脚本讲</el-button>
    </PageHeader>
    <div class="stat-grid">
      <StatCard label="导航页面" :value="pageCount" hint="角色过滤前的菜单项" />
      <StatCard label="嵌入式 AI" :value="aiPoints.length" hint="无 Key 时全部规则降级" tone="info" />
      <StatCard label="数据范围" :value="3" hint="SELF / DEPT / ALL" tone="warn" />
      <StatCard label="审批级数" :value="3" hint="按金额命中，可配置" tone="danger" />
    </div>
    <el-row :gutter="14" style="margin-top:14px">
      <el-col :xs="24" :lg="12">
        <div class="ip-card pad">
          <div class="sec">技术栈</div>
          <div class="stack">
            <span v-for="s in stack" :key="s">{{ s }}</span>
          </div>
          <p class="hint">单体 Spring Boot，不做微服务。H2 可直接答辩，MySQL 用同一套脚本。</p>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="ip-card pad">
          <div class="sec">AI 嵌在业务里</div>
          <ul>
            <li v-for="a in aiPoints" :key="a.name"><b>{{ a.name }}</b> · {{ a.where }}</li>
          </ul>
        </div>
      </el-col>
    </el-row>
    <div class="ip-card pad" style="margin-top:14px">
      <div class="sec">老师可能追问</div>
      <div class="qa">
        <div><b>审批是写死的吗？</b><span>不是。三档金额规则在库里，/approval-rules 可试算。</span></div>
        <div><b>数据权限呢？</b><span>申请人只看自己，主管看本部门，采购/仓管/财务/管理员看全部。顶栏有芯片。</span></div>
        <div><b>没有大模型 Key 怎么办？</b><span>填单、审批助手、比价、周报、制度问答、异常解读都走规则或检索片段，主流程不中断。</span></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { menuGroups } from '@/config/nav'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'

const pageCount = menuGroups.reduce((n, g) => n + g.items.length, 0)
const stack = ['Vue 3', 'Element Plus', 'ECharts', 'Spring Boot 3', 'MyBatis-Plus', 'JWT', 'H2 / MySQL', 'DeepSeek']
const aiPoints = [
  { name: '智能填单', where: '申请 / 合同抽屉' },
  { name: '审批助手', where: '我的审批' },
  { name: '比价结论', where: '询价页' },
  { name: '管理周报', where: '工作台' },
  { name: '制度 RAG', where: '制度问答' },
  { name: '异常解读', where: '异常页' }
]
</script>
<style scoped>
.pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 10px; }
.stack { display: flex; flex-wrap: wrap; gap: 8px; }
.stack span { border: 1px solid var(--ip-border); border-radius: 999px; padding: 4px 10px; font-size: 13px; }
.hint { color: var(--ip-text-secondary); font-size: 13px; }
ul { margin: 0; padding-left: 18px; line-height: 1.7; }
.qa { display: flex; flex-direction: column; gap: 10px; }
.qa div { display: flex; flex-direction: column; gap: 2px; }
.qa span { color: var(--ip-text-secondary); font-size: 13px; line-height: 1.5; }
</style>
