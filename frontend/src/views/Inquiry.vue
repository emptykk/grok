<template>
  <div class="ip-page">
    <PageHeader :title="`询价比价 · 申请 #${requestId}`" sub="并排报价对比 + 智能比价结论 + AI 推荐">
      <el-button @click="$router.back()">返回</el-button>
      <el-button type="primary" :loading="loading" @click="ensureInquiry">创建/打开询价单</el-button>
      <el-button type="warning" :disabled="!inquiry" :loading="recLoading" @click="onRecommend">AI 推荐</el-button>
      <el-button type="danger" plain :disabled="!inquiry || !quotes.length" :loading="verdictLoading" @click="onVerdict">生成比价结论</el-button>
      <el-button type="success" :disabled="!inquiry || !selectedSupplierId" @click="onSelect">确认选定</el-button>
      <el-button type="primary" plain :disabled="!inquiry || !selectedSupplierId" @click="onCreateOrder">生成订单</el-button>
      <el-button type="success" plain size="large" :disabled="!inquiry" @click="exportCompare">导出比价 Excel</el-button>
      <el-button type="primary" plain size="large" :disabled="!inquiry" @click="openPrint">打印预览</el-button>
    </PageHeader>

    <el-row :gutter="14">
      <el-col :xs="24" :lg="14">
        <div class="ip-card" style="padding:16px;margin-bottom:14px">
          <div class="sec">录入报价</div>
          <el-form inline>
            <el-form-item label="供应商">
              <el-select v-model="quote.supplierId" style="width:220px" filterable>
                <el-option v-for="s in suppliers" :key="s.id" :label="`${s.name}（${s.rating}星）`" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="总价"><el-input-number v-model="quote.totalPrice" :min="0" /></el-form-item>
            <el-form-item label="交期(天)"><el-input-number v-model="quote.deliveryDays" :min="1" /></el-form-item>
            <el-button @click="onAddQuote">添加</el-button>
          </el-form>
        </div>

        <div class="quote-grid" v-auto-animate>
          <div
            v-for="q in quotes"
            :key="q.id || q.supplierId"
            class="quote-card"
            :class="{ selected: selectedSupplierId === q.supplierId, 'ai-best': isAiBest(q.supplierId), lowest: isLowest(q) }"
            @click="selectedSupplierId = q.supplierId"
          >
            <div class="q-name">
              {{ supplierName(q.supplierId) }}
              <el-tag v-if="isLowest(q)" size="small" type="success" effect="dark">最低价</el-tag>
            </div>
            <div class="q-price">¥ {{ Number(q.totalPrice || 0).toLocaleString() }}</div>
            <el-progress :percentage="pricePct(q)" :show-text="false" :stroke-width="6" :color="isLowest(q) ? '#10b981' : '#0d9488'" />
            <div class="q-meta">交期 {{ q.deliveryDays }} 天 · {{ q.remark || '无备注' }}</div>
            <el-button link type="primary" @click.stop="$router.push(`/suppliers/${q.supplierId}/profile`)">供应商画像</el-button>
          </div>
          <EmptyState v-if="!quotes.length" title="还没有报价" description="先创建询价单并录入至少两家报价" />
        </div>
      </el-col>

      <el-col :xs="24" :lg="10">
        <div class="ip-card" style="padding:16px;margin-bottom:14px">
          <div class="sec">报价对比</div>
          <ChartBox :option="barOption" height="220px" />
          <el-table :data="rankedQuotes" size="small" style="margin-top:10px" :row-class-name="rowClass">
            <el-table-column label="供应商">
              <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
            </el-table-column>
            <el-table-column label="总价" width="110">
              <template #default="{ row }">¥ {{ Number(row.totalPrice || 0).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="deliveryDays" label="交期" width="70" />
            <el-table-column label="标记" width="80">
              <template #default="{ row }">
                <el-tag v-if="isLowest(row)" type="success" size="small">最低</el-tag>
                <span v-else class="gap">+{{ priceGap(row) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ip-card verdict" style="padding:16px;margin-bottom:14px">
          <div class="sec" style="display:flex;justify-content:space-between;align-items:center">
            <span>智能比价结论</span>
            <el-tag v-if="verdict" size="small">{{ verdict.source === 'llm' ? '大模型' : '规则' }}</el-tag>
          </div>
          <template v-if="verdict">
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="最低价">
                {{ verdict.lowestPrice?.supplierName }} · ¥{{ verdict.lowestPrice?.totalPrice }}
              </el-descriptions-item>
              <el-descriptions-item label="综合最优">
                {{ verdict.compositeBest?.supplierName }} · 分 {{ verdict.compositeBest?.compositeScore }}
              </el-descriptions-item>
              <el-descriptions-item label="风险">
                {{ (verdict.risks || []).join('；') || '无明显风险' }}
              </el-descriptions-item>
            </el-descriptions>
            <div class="report">{{ verdict.reportText }}</div>
            <div style="margin-top:10px;display:flex;gap:8px;flex-wrap:wrap">
              <el-button size="small" type="success" @click="adoptVerdict">采纳综合最优并选定</el-button>
              <el-button size="small" @click="copyText(verdict.reportText)">复制结论</el-button>
            </div>
          </template>
          <EmptyState v-else title="尚未生成比价结论" description="录入报价后点击「生成比价结论」，结论会写入询价单备查" />
        </div>

        <div class="ip-card" style="padding:16px">
          <div class="sec">AI / 规则推荐</div>
          <template v-if="recommend">
            <div class="ai-bubble" style="white-space:pre-wrap;font-size:13px;margin-bottom:12px">
              {{ recommend.aiRaw || recommend.aiRecommendJson || '已生成候选供应商排序' }}
            </div>
            <el-table :data="recommend.candidates || []" size="small">
              <el-table-column prop="name" label="候选" />
              <el-table-column prop="category" label="品类" />
              <el-table-column prop="rating" label="评级" width="70" />
            </el-table>
          </template>
          <EmptyState v-else title="尚未运行 AI 推荐" description="点击右上角「AI 推荐」生成理由与排序" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createInquiry, inquiryByRequest, addQuote, selectSupplier, recommendSuppliers, createOrder, aiInquiryVerdict } from '@/api'
import { ElMessage } from 'element-plus'
import { downloadFile } from '@/utils/download'
import PageHeader from '@/components/PageHeader.vue'
import ChartBox from '@/components/ChartBox.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute(); const router = useRouter()
const requestId = Number(route.params.requestId)
const loading = ref(false); const recLoading = ref(false); const verdictLoading = ref(false)
const inquiry = ref(null); const quotes = ref([]); const suppliers = ref([])
const selectedSupplierId = ref(null)
const recommend = ref(null)
const verdict = ref(null)
const quote = reactive({ supplierId: null, totalPrice: 0, deliveryDays: 7, remark: '' })
const aiBestId = ref(null)

function supplierName(id) { return suppliers.value.find(s => s.id === id)?.name || id }
const rankedQuotes = computed(() => [...quotes.value].sort((a, b) => Number(a.totalPrice || 0) - Number(b.totalPrice || 0)))
const lowestPrice = computed(() => rankedQuotes.value.length ? Number(rankedQuotes.value[0].totalPrice || 0) : null)
function isLowest(q) { return lowestPrice.value != null && Number(q.totalPrice) === lowestPrice.value }
function pricePct(q) {
  const max = Math.max(...quotes.value.map(x => Number(x.totalPrice) || 0), 1)
  return Math.round((Number(q.totalPrice || 0) / max) * 100)
}
function priceGap(q) {
  if (lowestPrice.value == null) return '—'
  return '¥' + (Number(q.totalPrice || 0) - lowestPrice.value).toLocaleString()
}
function rowClass({ row }) { return isLowest(row) ? 'low-row' : '' }
function isAiBest(id) {
  const v = verdict.value?.recommendedSupplierId || aiBestId.value
  return v && Number(v) === Number(id)
}
function copyText(t) {
  navigator.clipboard?.writeText(t || '')
  ElMessage.success('已复制')
}

const barOption = computed(() => ({
  color: ['#0d9488', '#3b82f6'],
  tooltip: { trigger: 'axis' },
  legend: { data: ['总价', '交期'] },
  grid: { left: 40, right: 40, top: 40, bottom: 28 },
  xAxis: { type: 'category', data: quotes.value.map(q => supplierName(q.supplierId)) },
  yAxis: [{ type: 'value', name: '元' }, { type: 'value', name: '天' }],
  series: [
    { name: '总价', type: 'bar', data: quotes.value.map(q => q.totalPrice), barWidth: 28, itemStyle: { borderRadius: [6,6,0,0] } },
    { name: '交期', type: 'line', yAxisIndex: 1, data: quotes.value.map(q => q.deliveryDays), smooth: true }
  ]
}))

async function refresh() {
  const data = await inquiryByRequest(requestId)
  inquiry.value = data.inquiry
  quotes.value = data.quotes || []
  suppliers.value = data.suppliers || []
  if (data.inquiry?.selectedSupplierId) selectedSupplierId.value = data.inquiry.selectedSupplierId
  if (data.inquiry?.aiRecommendJson) {
    try {
      const parsed = typeof data.inquiry.aiRecommendJson === 'string'
        ? JSON.parse(data.inquiry.aiRecommendJson) : data.inquiry.aiRecommendJson
      if (parsed?.verdict) verdict.value = parsed.verdict
      recommend.value = { aiRecommendJson: data.inquiry.aiRecommendJson, candidates: [] }
    } catch {
      recommend.value = { aiRecommendJson: data.inquiry.aiRecommendJson, candidates: [] }
    }
  }
}
async function ensureInquiry() {
  loading.value = true
  try { await createInquiry(requestId); await refresh(); ElMessage.success('询价单已就绪') }
  finally { loading.value = false }
}
async function onAddQuote() {
  if (!inquiry.value) return ElMessage.warning('请先创建询价单')
  await addQuote(inquiry.value.id, quote)
  ElMessage.success('已添加报价'); refresh()
}
async function onRecommend() {
  recLoading.value = true
  try {
    recommend.value = await recommendSuppliers(inquiry.value.id)
    const c = recommend.value?.candidates?.[0]
    if (c?.id) aiBestId.value = c.id
    else if (c?.supplierId) aiBestId.value = c.supplierId
    ElMessage.success('推荐完成')
  } finally { recLoading.value = false }
}
async function onVerdict() {
  if (!inquiry.value) return
  verdictLoading.value = true
  try {
    verdict.value = await aiInquiryVerdict(inquiry.value.id, true)
    if (verdict.value?.recommendedSupplierId) {
      selectedSupplierId.value = verdict.value.recommendedSupplierId
      aiBestId.value = verdict.value.recommendedSupplierId
    }
    ElMessage.success('比价结论已生成并写入报告')
  } catch (e) {
    ElMessage.error(e?.message || '生成失败')
  } finally { verdictLoading.value = false }
}
async function adoptVerdict() {
  if (!verdict.value?.recommendedSupplierId) return
  selectedSupplierId.value = verdict.value.recommendedSupplierId
  await onSelect()
}
async function onSelect() {
  await selectSupplier(inquiry.value.id, selectedSupplierId.value)
  ElMessage.success('已选定供应商'); refresh()
}
function exportCompare() {
  if (!inquiry.value) return
  downloadFile(`/api/inquiries/${inquiry.value.id}/compare-report/export`, `比价表-${inquiry.value.id}.xlsx`)
}
function openPrint() { router.push('/print/inquiry/' + requestId) }
async function onCreateOrder() {
  const id = await createOrder({ inquiryId: inquiry.value.id, expectDate: new Date().toISOString().slice(0,10) })
  ElMessage.success('订单已生成 #' + id)
  router.push('/orders')
}
onMounted(refresh)
</script>
<style scoped>
.sec { font-weight: 700; margin-bottom: 12px; }
.quote-grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.q-name { font-weight: 650; }
.q-price { font-size: 22px; font-weight: 800; margin: 8px 0 4px; letter-spacing: -0.02em; color: var(--ip-accent); }
.q-meta { font-size: 12px; color: var(--ip-text-secondary); margin-bottom: 8px; }
.verdict { border-color: rgba(13, 148, 136, .45); box-shadow: 0 0 0 1px rgba(13, 148, 136, .12); }
.report { margin-top: 10px; white-space: pre-wrap; font-size: 13px; line-height: 1.55; background: var(--el-fill-color-light); padding: 10px; border-radius: 8px; }
.gap { color: var(--ip-text-secondary); font-size: 12px; }
:deep(.low-row) { background: rgba(16, 185, 129, .12) !important; }
.quote-card.lowest { border-color: #10b981; }
</style>
