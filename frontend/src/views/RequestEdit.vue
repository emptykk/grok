<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap">
        <span>{{ id ? '编辑申请' : '新建申请' }}</span>
        <div style="display:flex;gap:8px">
          <el-button type="warning" plain @click="assistOpen = true">智能填单助手</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </div>
    </template>

    <el-alert type="info" show-icon :closable="false" title="可用自然语言生成明细，或打开右侧「智能填单助手」按上下文一键采纳字段" style="margin-bottom:12px" />

    <el-form label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="预算"><el-input-number v-model="form.budgetAmount" :min="0" :step="100" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      <el-form-item label="AI 描述">
        <div style="display:flex;gap:8px;width:100%">
          <el-input v-model="aiPrompt" type="textarea" :rows="2" placeholder="例：为市场部采购10台办公笔记本和2台激光打印机，预算6万" />
          <el-button type="warning" :loading="aiLoading" @click="onAi">生成明细</el-button>
        </div>
      </el-form-item>
    </el-form>

    <div style="margin:8px 0">
      <el-button size="small" @click="addRow">添加明细</el-button>
    </div>
    <el-table :data="form.items" border>
      <el-table-column label="物资名称"><template #default="{row}"><el-input v-model="row.materialName" /></template></el-table-column>
      <el-table-column label="规格" width="140"><template #default="{row}"><el-input v-model="row.spec" /></template></el-table-column>
      <el-table-column label="单位" width="80"><template #default="{row}"><el-input v-model="row.unit" /></template></el-table-column>
      <el-table-column label="数量" width="110"><template #default="{row}"><el-input-number v-model="row.quantity" :min="0" :controls="false" /></template></el-table-column>
      <el-table-column label="估价" width="110"><template #default="{row}"><el-input-number v-model="row.estimatePrice" :min="0" :controls="false" /></template></el-table-column>
      <el-table-column label="操作" width="80"><template #default="{$index}"><el-button link type="danger" @click="form.items.splice($index,1)">删</el-button></template></el-table-column>
    </el-table>

    <div style="margin-top:16px">
      <el-button type="primary" :loading="saving" @click="onSave(false)">保存草稿</el-button>
      <el-button type="success" :loading="saving" @click="onSave(true)">保存并提交</el-button>
    </div>

    <FormAssistDrawer
      v-model="assistOpen"
      scene="REQUEST"
      :form="assistForm"
      @apply-field="onApplyField"
      @apply-item="onApplyItem"
      @apply-items="onApplyItems"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRequest, saveRequest, submitRequest, aiGenerateRequest } from '@/api'
import { ElMessage } from 'element-plus'
import FormAssistDrawer from '@/components/FormAssistDrawer.vue'

const route = useRoute(); const router = useRouter()
const id = route.params.id
const saving = ref(false); const aiLoading = ref(false)
const aiPrompt = ref('')
const assistOpen = ref(false)
const form = reactive({ id: null, title: '', budgetAmount: 0, remark: '', aiPrompt: '', items: [] })
const assistForm = computed(() => ({
  title: form.title,
  budgetAmount: form.budgetAmount,
  remark: form.remark,
  items: form.items,
  hint: aiPrompt.value
}))

function addRow() { form.items.push({ materialName: '', spec: '', unit: '台', quantity: 1, estimatePrice: 0, remark: '' }) }

function onApplyField(s) {
  if (!s?.field) return
  if (s.field === 'budgetAmount') form.budgetAmount = Number(s.value || 0)
  else if (s.field in form) form[s.field] = s.value
  ElMessage.success('已采纳：' + (s.label || s.field))
}
function onApplyItem(it) {
  form.items.push({
    materialName: it.materialName || '',
    spec: it.spec || '',
    unit: it.unit || '台',
    quantity: Number(it.quantity || 1),
    estimatePrice: Number(it.estimatePrice || 0),
    remark: it.remark || ''
  })
  ElMessage.success('已添加明细行')
}
function onApplyItems(list) {
  form.items = (list || []).map(it => ({
    materialName: it.materialName || '',
    spec: it.spec || '',
    unit: it.unit || '台',
    quantity: Number(it.quantity || 1),
    estimatePrice: Number(it.estimatePrice || 0),
    remark: it.remark || ''
  }))
  ElMessage.success('已写入全部明细')
}

async function onAi() {
  if (!aiPrompt.value) return ElMessage.warning('请先填写需求描述')
  aiLoading.value = true
  try {
    const items = await aiGenerateRequest(aiPrompt.value)
    form.items = items.map(i => ({
      materialName: i.materialName || i.name || '',
      spec: i.spec || '',
      unit: i.unit || '台',
      quantity: Number(i.quantity || 1),
      estimatePrice: Number(i.estimatePrice || 0),
      remark: i.remark || ''
    }))
    form.aiPrompt = aiPrompt.value
    if (!form.title) form.title = aiPrompt.value.slice(0, 40)
    ElMessage.success('已生成明细，请确认后保存')
  } finally { aiLoading.value = false }
}

async function onSave(submit) {
  if (!form.title) return ElMessage.warning('请填写标题')
  if (!form.items.length) return ElMessage.warning('请添加明细')
  saving.value = true
  try {
    const newId = await saveRequest(form)
    form.id = newId
    if (submit) await submitRequest(newId)
    ElMessage.success(submit ? '已提交审批' : '已保存')
    router.push('/requests')
  } finally { saving.value = false }
}

onMounted(async () => {
  if (id) {
    const d = await getRequest(id)
    Object.assign(form, { id: d.request.id, title: d.request.title, budgetAmount: Number(d.request.budgetAmount||0), remark: d.request.remark, aiPrompt: d.request.aiPrompt, items: d.items })
    aiPrompt.value = d.request.aiPrompt || ''
  } else addRow()
})
</script>
