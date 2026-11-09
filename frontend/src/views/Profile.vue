<template>
  <div class="ip-page">
    <PageHeader title="个人中心" sub="角色、数据范围和你刚才留下的操作痕迹" />
    <el-row :gutter="14">
      <el-col :md="10" :xs="24">
        <div class="ip-card pad">
          <div class="avatar">{{ (user.realName || '?').slice(0,1) }}</div>
          <h3>{{ user.realName }}</h3>
          <p class="sub">{{ user.username }}</p>
          <div class="chips">
            <el-tag v-for="r in user.roles || []" :key="r">{{ roleName(r) }}</el-tag>
            <el-tag type="warning">{{ scope.label }} · {{ scope.code }}</el-tag>
          </div>
          <el-descriptions :column="1" size="small" style="margin-top:16px">
            <el-descriptions-item label="部门">{{ user.deptName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="能看到">{{ scopeHint }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="ip-card pad" style="margin-top:14px">
          <div class="sec">换一个演示账号</div>
          <p class="hint">{{ accountHint }}</p>
          <ul class="accounts">
            <li v-for="a in accounts" :key="a.u"><b>{{ a.u }}</b> {{ a.say }}</li>
          </ul>
          <el-button @click="logout">退出并去登录页</el-button>
        </div>
      </el-col>
      <el-col :md="14" :xs="24">
        <div class="ip-card pad" style="margin-bottom:14px">
          <div class="sec">外观与数据源</div>
          <div class="row">
            <span>深色工业风</span>
            <el-switch :model-value="theme.dark" @change="theme.toggle()" />
          </div>
          <div class="row">
            <span>接口失败时才使用演示 mock</span>
            <el-switch :model-value="theme.useMock" @change="theme.setMock($event)" />
          </div>
        </div>
        <div class="ip-card pad">
          <div class="sec">最近操作</div>
          <el-skeleton v-if="loading" :rows="4" animated />
          <template v-else>
            <div v-for="log in logs" :key="log.id" class="log">
              <b>{{ log.module }} · {{ log.action }}</b>
              <span>{{ log.detail || log.requestUri }}</span>
              <em>{{ log.createdAt }}</em>
            </div>
            <EmptyState v-if="!logs.length" title="还没有你的操作记录" description="登录、审批或提问之后会出现在这里。" />
          </template>
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { resolveScope } from '@/config/nav'
import { pageLogs } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user || {})
const scope = computed(() => resolveScope(user.value))
const theme = useThemeStore()
const logs = ref([])
const accountHint = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS === 'false'
  ? '公网环境不在页面展示本地口令。退出后使用部署者单独提供的口令登录。'
  : '系统不会在登录态里静默换人。退出后回登录页，点对应身份卡片即可。本地口令见 README。'
const loading = ref(true)
const names = {
  ADMIN: '系统管理员', APPLICANT: '采购申请人', DEPT_MANAGER: '部门主管',
  PURCHASE_MANAGER: '采购负责人', WAREHOUSE: '仓管', FINANCE: '财务'
}
const accounts = [
  { u: 'zhangsan', say: '申请人，数据范围仅本人' },
  { u: 'lisi', say: '部门主管，一级审批，本部门' },
  { u: 'wangwu', say: '采购负责人，比价与画像' },
  { u: 'zhaoliu', say: '仓管，入库与库存预警' },
  { u: 'caiwu', say: '财务，三级审批与付款' },
  { u: 'admin', say: '运行中心、日志、大模型配置' }
]
const scopeHint = computed(() => {
  if (scope.value.code === 'ALL') return '全部业务单据'
  if (scope.value.code === 'DEPT') return '仅本部门申请与预算'
  return '仅自己提交的申请'
})
function roleName(code) { return names[code] || code }
function logout() {
  userStore.logout()
  router.push('/login')
}
onMounted(async () => {
  try {
    const data = await pageLogs({ page: 1, size: 6, username: user.value.username })
    logs.value = data.records || []
  } catch { logs.value = [] }
  finally { loading.value = false }
})
</script>
<style scoped>
.pad { padding: 20px; }
.avatar {
  width: 56px; height: 56px; border-radius: 16px;
  display:flex; align-items:center; justify-content:center;
  background: linear-gradient(135deg,#0d9488,#14b8a6); color:#fff; font-size:22px; font-weight:800;
}
.sub { color: var(--ip-text-secondary); margin-top: 4px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.sec { font-weight: 700; margin-bottom: 10px; }
.row { display:flex; justify-content:space-between; align-items:center; padding: 12px 0; border-bottom: 1px solid var(--ip-border); }
.hint, .accounts { color: var(--ip-text-secondary); font-size: 13px; line-height: 1.6; }
.accounts { padding-left: 18px; }
.log { display: flex; flex-direction: column; padding: 8px 0; border-bottom: 1px dashed var(--ip-border); }
.log span, .log em { color: var(--ip-text-secondary); font-size: 12px; font-style: normal; }
</style>
