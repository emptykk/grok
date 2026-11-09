<template>
  <div class="ip-page">
    <PageHeader title="大模型配置" sub="管理员可在此填写 DeepSeek Base URL / API Key / Model，优先生效于环境变量" />
    <div class="ip-card" style="padding:20px;max-width:720px">
      <el-alert
        :type="form.configured ? 'success' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom:16px"
        :title="form.configured ? `已配置（来源：${form.source}，脱敏密钥 ${form.apiKeyMasked || '****'}）` : '尚未配置 API Key，智能能力将走规则降级'"
      />
      <el-form label-width="120px">
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com" />
        </el-form-item>
        <el-form-item label="Model">
          <el-input v-model="form.model" placeholder="deepseek-chat" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="留空或带 **** 则不改动已有密钥" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
          <el-button @click="load">刷新</el-button>
          <el-button link type="primary" @click="$router.push('/ai')">去 AI 助手页</el-button>
        </el-form-item>
      </el-form>
      <el-divider />
      <div style="font-size:13px;color:var(--el-text-color-secondary);line-height:1.6">
        说明：配置写入后端 <code>sys_config</code> 表。密钥接口返回脱敏值；保存时若输入含 <code>*</code> 或为空则保留原密钥。
        嵌入式能力包括：智能填单助手、审批助手、比价结论、管理周报。
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAiConfig, saveAiConfig } from '@/api'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'

const saving = ref(false)
const form = reactive({
  baseUrl: 'https://api.deepseek.com',
  model: 'deepseek-chat',
  apiKey: '',
  configured: false,
  apiKeyMasked: '',
  source: ''
})

async function load() {
  const data = await getAiConfig()
  form.baseUrl = data.baseUrl || 'https://api.deepseek.com'
  form.model = data.model || 'deepseek-chat'
  form.configured = !!data.configured
  form.apiKeyMasked = data.apiKeyMasked || ''
  form.source = data.source || ''
  form.apiKey = ''
}
async function onSave() {
  saving.value = true
  try {
    const data = await saveAiConfig({
      baseUrl: form.baseUrl,
      model: form.model,
      apiKey: form.apiKey
    })
    Object.assign(form, {
      baseUrl: data.baseUrl,
      model: data.model,
      configured: data.configured,
      apiKeyMasked: data.apiKeyMasked,
      source: data.source,
      apiKey: ''
    })
    ElMessage.success('已保存大模型配置')
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally { saving.value = false }
}
onMounted(load)
</script>
