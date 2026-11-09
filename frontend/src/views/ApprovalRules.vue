<template>
  <div class="ip-page">
    <PageHeader title="审批规则" sub="按金额命中级数。老师可在这里看到规则是配置出来的，不是写死在页面上。">
      <el-tag type="info">只读展示</el-tag>
    </PageHeader>
    <el-skeleton v-if="loading" animated :rows="6" />
    <template v-else>
      <div class="ladder">
        <div v-for="(r, i) in rules" :key="r.id" class="step" :style="{ animationDelay: (i * 80) + 'ms' }">
          <div class="lv">{{ r.levels }} 级</div>
          <div class="bar" :style="{ height: (36 + r.levels * 28) + 'px' }" />
          <div class="name">{{ r.name }}</div>
          <div class="range">¥{{ money(r.minAmount) }} – {{ cap(r.maxAmount) }}</div>
          <div class="remark">{{ r.remark || (r.enabled === 0 ? '已停用' : '启用') }}</div>
        </div>
      </div>
      <el-row :gutter="14">
        <el-col :xs="24" :lg="15">
          <div class="ip-card" style="padding:8px">
            <el-table :data="rules" size="small">
              <el-table-column prop="name" label="规则" />
              <el-table-column label="下限" width="120"><template #default="{ row }">{{ money(row.minAmount) }}</template></el-table-column>
              <el-table-column label="上限" width="140"><template #default="{ row }">{{ cap(row.maxAmount) }}</template></el-table-column>
              <el-table-column prop="levels" label="级数" width="70" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }"><el-tag :type="row.enabled === 0 ? 'info' : 'success'" size="small">{{ row.enabled === 0 ? '停用' : '启用' }}</el-tag></template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :lg="9">
          <div class="ip-card pad">
            <div class="sec">试算金额</div>
            <el-input-number v-model="amount" :min="0" :step="1000" style="width:100%" />
            <el-button type="primary" style="margin-top:10px" :loading="resolving" @click="resolve">看会走几级</el-button>
            <div v-if="hit" class="hit">
              <b>{{ hit.levels }} 级审批</b>
              <p>{{ hit.rule?.name || '未命中规则，默认两级' }}</p>
              <p class="muted">{{ hit.rule?.remark }}</p>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { listApprovalRules, resolveApprovalRule } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const rules = ref([])
const loading = ref(true)
const amount = ref(68000)
const resolving = ref(false)
const hit = ref(null)
const money = (v) => Number(v || 0).toLocaleString()
function cap(v) {
  const n = Number(v || 0)
  return n >= 9999999 ? '不封顶' : ('¥' + n.toLocaleString())
}
async function resolve() {
  resolving.value = true
  try { hit.value = await resolveApprovalRule(amount.value) }
  finally { resolving.value = false }
}
onMounted(async () => {
  try { rules.value = await listApprovalRules() || [] }
  finally { loading.value = false }
  resolve()
})
</script>
<style scoped>
.ladder { display: flex; gap: 12px; align-items: flex-end; margin-bottom: 16px; overflow-x: auto; padding-bottom: 6px; }
.step {
  flex: 1; min-width: 160px; background: var(--ip-surface); border: 1px solid var(--ip-border);
  border-radius: 14px; padding: 12px; animation: rise .45s ease both;
}
.lv { font-weight: 800; color: var(--ip-accent); }
.bar { margin: 8px 0; border-radius: 8px 8px 2px 2px; background: linear-gradient(180deg, #5eead4, #0d9488); }
.name { font-weight: 700; font-size: 13px; }
.range, .remark, .muted { color: var(--ip-text-secondary); font-size: 12px; margin-top: 4px; }
.pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 10px; }
.hit { margin-top: 14px; padding: 12px; border-radius: 12px; background: var(--ip-accent-soft); }
.hit b { font-size: 22px; }
@keyframes rise { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }
</style>
