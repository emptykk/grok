<template>
  <div class="ip-page">
    <PageHeader title="制度问答" sub="上传制度、检索片段、引用高亮，并保留提问历史">
      <span class="badge-mock">无 Key 时返回检索片段</span>
    </PageHeader>
    <el-row :gutter="14">
      <el-col :xs="24" :lg="8">
        <div class="ip-card pad">
          <div class="sec">制度文档</div>
          <el-upload :http-request="upload" :show-file-list="false" accept=".pdf,.docx,.txt,.md">
            <el-button type="primary">上传 PDF / Word / TXT</el-button>
          </el-upload>
          <p class="hint">演示库已预置《采购管理制度（节选）》，可直接提问。</p>
          <div v-for="d in docs" :key="d.id" class="doc">
            <div>
              <b>{{ d.title }}</b>
              <div class="hint">{{ d.chunkCount }} 段 · {{ d.status }}</div>
            </div>
            <el-button link type="danger" @click="onDel(d)">删除</el-button>
          </div>
          <EmptyState v-if="!docs.length" title="还没有制度文档" />
        </div>
        <div class="ip-card pad" style="margin-top:14px">
          <div class="sec">提问历史</div>
          <button v-for="h in history" :key="h.id" type="button" class="hist" @click="openHistory(h)">
            <b>{{ h.question }}</b>
            <span>{{ h.createdAt }}</span>
          </button>
          <EmptyState v-if="!history.length" title="还没有提问" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="16">
        <div class="ip-card pad">
          <div class="sec">向制度提问</div>
          <el-input v-model="question" type="textarea" :rows="3" placeholder="例如：超过1万元的采购需要几级审批？" />
          <div class="ask-row">
            <el-button @click="question = '超过1万元的采购需要几级审批？'">填入示例</el-button>
            <el-button type="primary" :loading="asking" @click="ask">提问</el-button>
          </div>
          <template v-if="answer">
            <h3>回答</h3>
            <div class="ans" v-html="renderAnswer(answer.answer)" />
            <h3>依据片段</h3>
            <div class="ref-grid">
              <article v-for="r in answer.references || []" :key="r.index" class="cite-card">
                <div class="cite-no">[{{ r.index }}]</div>
                <div class="cite-meta">
                  <b>{{ r.docTitle }}</b>
                  <span>片段 #{{ r.chunkIndex }} · 相关度 {{ Number(r.score || 0).toFixed(2) }}</span>
                </div>
                <div class="snippet" v-html="highlight(r.content || r.snippet, keywords)" />
              </article>
            </div>
            <div v-if="(answer.highlights || []).length" class="chips">
              <span>命中词</span>
              <el-tag v-for="h in answer.highlights" :key="h.refIndex + h.keyword" size="small">{{ h.keyword }}</el-tag>
            </div>
          </template>
          <EmptyState v-else title="从左侧历史或上方输入开始" description="回答会标出 [1][2] 引用，片段里的命中词会高亮" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { listKnowledgeDocs, askKnowledge, deleteKnowledgeDoc, knowledgeHistory, knowledgeQa } from '@/api'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const docs = ref([])
const history = ref([])
const question = ref('超过1万元的采购需要几级审批？')
const asking = ref(false)
const answer = ref(null)
const store = useUserStore()
const keywords = computed(() => (answer.value?.highlights || []).map(h => h.keyword).filter(Boolean))

function escapeHtml(s) {
  return String(s || '').replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]))
}
function highlight(text, words) {
  let html = escapeHtml(text)
  const uniq = [...new Set(words || [])].filter(w => w && w.length >= 2)
  for (const w of uniq) {
    const esc = w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    html = html.replace(new RegExp(esc, 'g'), `<mark>${escapeHtml(w)}</mark>`)
  }
  return html
}
function renderAnswer(text) {
  return highlight(text, keywords.value).replace(/\[(\d+)\]/g, '<sup class="cite">[$1]</sup>')
}
async function load() {
  docs.value = await listKnowledgeDocs()
  const page = await knowledgeHistory({ page: 1, size: 8 })
  history.value = page.records || []
}
async function upload(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  fd.append('title', opt.file.name)
  await axios.post('/api/knowledge/upload', fd, { headers: { Authorization: 'Bearer ' + store.token } })
  ElMessage.success('上传并分段完成')
  load()
}
async function ask() {
  if (!question.value.trim()) return
  asking.value = true
  try {
    answer.value = await askKnowledge(question.value)
    await load()
  } finally { asking.value = false }
}
async function onDel(row) { await deleteKnowledgeDoc(row.id); load() }
async function openHistory(h) {
  const detail = await knowledgeQa(h.id)
  let references = []
  let highlights = []
  try { references = JSON.parse(detail.referencesJson || '[]') } catch { references = [] }
  try { highlights = JSON.parse(detail.highlightsJson || '[]') } catch { highlights = [] }
  question.value = detail.question
  answer.value = { answer: detail.answer, references, highlights }
}
onMounted(load)
</script>
<style scoped>
.pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 10px; }
.hint { color: var(--ip-text-secondary); font-size: 12px; margin: 8px 0; }
.doc, .hist { display: flex; justify-content: space-between; gap: 8px; width: 100%; text-align: left; border: 0; background: transparent; color: inherit; padding: 10px 0; border-bottom: 1px solid var(--ip-border); cursor: pointer; }
.hist { flex-direction: column; }
.ask-row { display: flex; gap: 8px; margin: 10px 0 16px; }
.ans, .snippet { white-space: pre-wrap; line-height: 1.65; background: var(--ip-surface-2); padding: 12px; border-radius: 10px; }
.ref-grid { display: flex; flex-direction: column; gap: 10px; }
.cite-card {
  display: grid; grid-template-columns: 52px 1fr; gap: 8px 12px;
  border: 1px solid rgba(13,148,136,.35);
  background: linear-gradient(180deg, var(--ip-accent-soft), transparent 42%), var(--ip-surface);
  border-radius: 14px; padding: 12px;
  animation: rise .35s ease both;
}
.cite-no { grid-row: span 2; font-size: 20px; font-weight: 800; color: var(--ip-accent); }
.cite-meta { display: flex; flex-direction: column; }
.cite-meta span { font-size: 12px; color: var(--ip-text-secondary); }
.cite-card .snippet { grid-column: 2; margin: 0; }
@keyframes rise { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.chips { display: flex; gap: 6px; align-items: center; margin-top: 8px; flex-wrap: wrap; }
:deep(mark) { background: rgba(250, 204, 21, .45); color: inherit; padding: 0 2px; border-radius: 3px; }
:deep(.cite) { color: var(--ip-accent); font-weight: 700; }
</style>
