<template>
  <el-drawer v-model="visible" :title="title" size="420px" append-to-body>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
      title="根据当前表单上下文生成可采纳建议；未配置大模型时走规则降级。" />
    <el-input v-model="hint" type="textarea" :rows="3" placeholder="可选：补充一句需求或约束" style="margin-bottom:10px" />
    <el-button type="primary" :loading="loading" style="width:100%;margin-bottom:14px" @click="run">生成 AI 建议</el-button>

    <div v-if="result" class="block">
      <div class="advice">{{ result.advice }}</div>
      <el-tag size="small" type="info" style="margin:8px 0">来源：{{ result.source === 'llm' ? '大模型' : '规则降级' }}</el-tag>

      <div v-if="(result.suggestions || []).length" class="sec">字段建议</div>
      <div v-for="(s, idx) in (result.suggestions || [])" :key="'s'+idx" class="card">
        <div class="lab">{{ s.label || s.field }}</div>
        <div class="val">{{ s.value }}</div>
        <div class="reason">{{ s.reason }}</div>
        <el-button size="small" type="success" @click="$emit('apply-field', s)">一键采纳</el-button>
      </div>

      <div v-if="(result.itemSuggestions || []).length" class="sec">明细建议</div>
      <div v-for="(it, idx) in (result.itemSuggestions || [])" :key="'i'+idx" class="card">
        <div class="lab">{{ it.materialName }} · {{ it.spec || '-' }}</div>
        <div class="val">数量 {{ it.quantity }} {{ it.unit }} / 估价 {{ it.estimatePrice }}</div>
        <div class="reason">{{ it.remark }}</div>
        <el-button size="small" type="success" @click="$emit('apply-item', it)">采纳为明细行</el-button>
      </div>
      <el-button v-if="(result.itemSuggestions || []).length" link type="primary" @click="$emit('apply-items', result.itemSuggestions)">全部写入明细</el-button>
    </div>
  </el-drawer>
</template>
<script setup>
import { ref, watch } from 'vue'
import { aiFormAssist } from '@/api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  scene: { type: String, default: 'REQUEST' },
  form: { type: Object, default: () => ({}) },
  title: { type: String, default: '智能填单助手' }
})
const emit = defineEmits(['update:modelValue', 'apply-field', 'apply-item', 'apply-items'])

const visible = ref(false)
const hint = ref('')
const loading = ref(false)
const result = ref(null)

watch(() => props.modelValue, v => { visible.value = v; if (v) result.value = null })
watch(visible, v => emit('update:modelValue', v))

async function run() {
  loading.value = true
  try {
    result.value = await aiFormAssist({ scene: props.scene, form: props.form, hint: hint.value })
    ElMessage.success('已生成建议')
  } catch (e) {
    ElMessage.error(e?.message || '生成失败')
  } finally { loading.value = false }
}
</script>
<style scoped>
.advice { background: var(--el-fill-color-light); padding: 10px 12px; border-radius: 8px; font-size: 13px; line-height: 1.6; }
.sec { font-weight: 700; margin: 14px 0 8px; }
.card { border: 1px solid var(--el-border-color); border-radius: 10px; padding: 10px 12px; margin-bottom: 10px; }
.lab { font-weight: 650; font-size: 13px; }
.val { margin: 4px 0; font-size: 13px; word-break: break-all; white-space: pre-wrap; }
.reason { color: var(--el-text-color-secondary); font-size: 12px; margin-bottom: 8px; }
</style>
