<template>
  <div class="ip-page ai-page">
    <PageHeader title="AI 助手" sub="生成采购申请 · 制度问答 · 异常解读">
      <el-button @click="clearChat">清空对话</el-button>
    </PageHeader>
    <div class="layout">
      <div class="chat ip-card" v-auto-animate>
        <div v-for="(m, i) in messages" :key="i" class="row" :class="m.role">
          <div class="who">{{ m.role === 'user' ? '我' : '助手' }}</div>
          <div class="ai-bubble" :class="{ user: m.role === 'user' }">
            <div style="white-space:pre-wrap">{{ m.content }}</div>
            <div v-if="m.refs?.length" class="refs">
              <div class="ref-title">依据片段</div>
              <div v-for="(r, ri) in m.refs" :key="ri" class="ref">{{ r }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="side">
        <div class="ip-card pad">
          <div class="sec">快捷能力</div>
          <el-space wrap>
            <el-button @click="preset('帮我生成：市场部采购 10 台办公笔记本，预算 5 万')">生成申请</el-button>
            <el-button @click="preset('超过 1 万元的采购需要几级审批？')">问制度</el-button>
            <el-button @click="$router.push('/anomaly')">异常解读</el-button>
            <el-button @click="$router.push('/knowledge')">制度文档</el-button>
          </el-space>
        </div>
        <div class="ip-card pad" style="margin-top:14px">
          <div class="sec">模式</div>
          <el-radio-group v-model="mode">
            <el-radio-button value="request">写申请</el-radio-button>
            <el-radio-button value="knowledge">制度问答</el-radio-button>
          </el-radio-group>
          <p class="hint">写申请调用 /api/ai/generate-request；制度问答调用 /api/knowledge/ask</p>
        </div>
      </div>
    </div>
    <div class="composer ip-card">
      <el-input v-model="input" type="textarea" :rows="2" placeholder="输入自然语言需求或制度问题…" @keydown.ctrl.enter="send" />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { aiGenerateRequest, askKnowledge } from '@/api'
import { mockAiChats } from '@/mock/data'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'

const messages = ref([...mockAiChats])
const input = ref('')
const loading = ref(false)
const mode = ref('request')

function preset(t) { input.value = t }
function clearChat() { messages.value = [...mockAiChats] }

async function send() {
  const q = input.value.trim()
  if (!q) return
  messages.value.push({ role: 'user', content: q })
  input.value = ''
  loading.value = true
  try {
    if (mode.value === 'request') {
      const data = await aiGenerateRequest(q)
      const text = typeof data === 'string' ? data : JSON.stringify(data, null, 2)
      messages.value.push({ role: 'assistant', content: '已生成申请明细（可复制到新建申请页）：\n' + text, refs: [] })
    } else {
      const data = await askKnowledge(q)
      messages.value.push({
        role: 'assistant',
        content: data.answer || data.content || JSON.stringify(data),
        refs: data.refs || data.segments || data.sources || []
      })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '调用失败：' + (e?.message || '请检查后端与 DeepSeek Key'), refs: [] })
    ElMessage.error('AI 调用失败')
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.ai-page { display:flex; flex-direction:column; min-height: calc(100vh - 120px); }
.layout { display:grid; grid-template-columns: 1fr 300px; gap:14px; flex:1; }
@media (max-width: 960px) { .layout { grid-template-columns: 1fr; } }
.chat { padding: 16px; min-height: 420px; max-height: 58vh; overflow:auto; }
.row { display:flex; gap:10px; margin-bottom:14px; }
.row.user { flex-direction: row-reverse; }
.who {
  width: 36px; height: 36px; border-radius: 10px; flex:none;
  display:flex; align-items:center; justify-content:center;
  background: var(--ip-accent-soft); color: var(--ip-accent); font-size: 12px; font-weight: 700;
}
.row.user .who { background: #dbeafe; color: #2563eb; }
.refs { margin-top: 10px; padding-top: 8px; border-top: 1px dashed var(--ip-border); }
.ref-title { font-size: 12px; color: var(--ip-text-secondary); margin-bottom: 6px; }
.ref { font-size: 12px; background: var(--ip-surface); border: 1px solid var(--ip-border); border-radius: 8px; padding: 8px; margin-bottom: 6px; }
.side .pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 10px; }
.hint { font-size: 12px; color: var(--ip-text-secondary); line-height: 1.5; margin-top: 10px; }
.composer { margin-top: 14px; padding: 12px; display:flex; gap:10px; align-items:flex-end; }
</style>
