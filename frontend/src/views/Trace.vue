<template>
  <div class="ip-page">
    <PageHeader title="全链路时间机器" sub="选一条种子申请，把提交、审批、询价轮次、下单、入库差异、合同和发票串成一条故事。">
      <el-select v-model="requestId" filterable style="width:320px" @change="load">
        <el-option v-for="r in options" :key="r.id" :label="`${r.requestNo} ${r.title}`" :value="r.id" />
      </el-select>
    </PageHeader>
    <el-skeleton v-if="loading" animated :rows="8" />
    <template v-else-if="data">
      <div class="ip-card pad head">
        <div>
          <div class="no">{{ data.request?.requestNo }}</div>
          <h2>{{ data.request?.title }}</h2>
          <p>{{ data.request?.deptName }} · 预算 ¥{{ Number(data.request?.budgetAmount || 0).toLocaleString() }} · {{ data.request?.status }}</p>
        </div>
        <div class="flags">
          <el-tag :type="data.hasInquiry ? 'success' : 'info'">{{ data.hasInquiry ? '已询价' : '未询价' }}</el-tag>
          <el-tag :type="data.hasOrder ? 'success' : 'info'">{{ data.hasOrder ? '已下单' : '未下单' }}</el-tag>
        </div>
      </div>
      <div class="rail">
        <div v-for="(e, i) in data.events || []" :key="i" class="event" :class="e.tone">
          <div class="phase">{{ e.phase }}</div>
          <div class="body">
            <b>{{ e.title }}</b>
            <p>{{ e.detail }}</p>
            <em>{{ e.time }}</em>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRequestTrace, pageRequests } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const options = ref([])
const requestId = ref(Number(route.params.requestId) || null)
const data = ref(null)
const loading = ref(false)

async function load() {
  if (!requestId.value) return
  if (String(route.params.requestId) !== String(requestId.value)) {
    router.replace('/trace/' + requestId.value)
  }
  loading.value = true
  try { data.value = await getRequestTrace(requestId.value) }
  finally { loading.value = false }
}
onMounted(async () => {
  const page = await pageRequests({ page: 1, size: 50 })
  options.value = page.records || []
  if (!requestId.value && options.value.length) {
    const done = options.value.find(r => r.status === 'COMPLETED') || options.value[0]
    requestId.value = done.id
  }
  load()
})
watch(() => route.params.requestId, (id) => {
  if (id && Number(id) !== Number(requestId.value)) {
    requestId.value = Number(id)
    load()
  }
})
</script>
<style scoped>
.pad { padding: 16px; }
.head { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.no { color: var(--ip-accent); font-weight: 700; letter-spacing: .08em; font-size: 12px; }
h2 { margin: 4px 0; }
.head p { margin: 0; color: var(--ip-text-secondary); }
.flags { display: flex; gap: 8px; align-items: flex-start; }
.rail { display: flex; flex-direction: column; gap: 10px; }
.event {
  display: grid; grid-template-columns: 72px 1fr; gap: 12px;
  background: var(--ip-surface); border: 1px solid var(--ip-border); border-radius: 14px; padding: 12px 14px;
}
.phase { font-weight: 800; color: var(--ip-accent); }
.event.warn { border-color: rgba(245, 158, 11, .6); }
.event.reject { border-color: rgba(239, 68, 68, .55); }
.body b { display: block; }
.body p { margin: 4px 0; color: var(--ip-text-secondary); line-height: 1.55; }
.body em { font-style: normal; font-size: 12px; color: var(--ip-text-secondary); }
</style>
