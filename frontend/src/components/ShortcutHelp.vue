<template>
  <el-dialog v-model="open" title="快捷键与常用跳转" width="520px">
    <div class="row"><kbd>?</kbd><span>打开本面板</span></div>
    <div class="row"><kbd>Ctrl</kbd><span>+</span><kbd>K</kbd><span>命令面板：搜菜单并跳转</span></div>
    <div class="row"><kbd>Esc</kbd><span>关闭命令面板</span></div>
    <el-divider />
    <div class="links">
      <el-button v-for="l in links" :key="l.path" @click="go(l.path)">{{ l.label }}</el-button>
    </div>
    <p class="hint">答辩导览在顶栏。看完或跳过后不会自动再弹，按钮可以重播。</p>
  </el-dialog>
</template>
<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const open = ref(false)
const links = [
  { label: '演示脚本', path: '/demo-script' },
  { label: '全链路时间机器', path: '/trace/1' },
  { label: '供应商对比', path: '/suppliers/compare' },
  { label: '预算仪表', path: '/budgets' },
  { label: '审批规则', path: '/approval-rules' },
  { label: '工作台', path: '/dashboard' },
  { label: '答辩一页纸', path: '/defense-brief' }
]
function typingTarget(el) {
  if (!el) return false
  const tag = el.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || el.isContentEditable
}
function onKey(e) {
  if (e.key !== '?' || e.ctrlKey || e.metaKey || e.altKey) return
  if (typingTarget(e.target)) return
  e.preventDefault()
  open.value = !open.value
}
function go(path) {
  open.value = false
  router.push(path)
}
function onOpen() { open.value = true }
onMounted(() => {
  window.addEventListener('keydown', onKey)
  window.addEventListener('ip-shortcuts', onOpen)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('ip-shortcuts', onOpen)
})
defineExpose({ open: () => { open.value = true } })
</script>
<style scoped>
.row { display: flex; align-items: center; gap: 8px; margin: 8px 0; }
kbd { border: 1px solid var(--ip-border); border-radius: 6px; padding: 2px 6px; font-size: 12px; background: var(--ip-surface-2); }
.links { display: flex; flex-wrap: wrap; gap: 8px; }
.hint { color: var(--ip-text-secondary); font-size: 12px; }
</style>
