<template>
  <teleport to="body">
    <div v-if="open" class="mask" @click.self="close">
      <div class="panel" role="dialog" aria-label="命令面板">
        <div class="search">
          <el-icon><Search /></el-icon>
          <input
            ref="inputRef"
            v-model="q"
            placeholder="搜索菜单、跳转页面或执行操作"
            @keydown.down.prevent="move(1)"
            @keydown.up.prevent="move(-1)"
            @keydown.enter.prevent="runActive"
            @keydown.esc.prevent="close"
          />
          <kbd>ESC</kbd>
        </div>
        <div class="list">
          <button
            v-for="(item, idx) in filtered"
            :key="item.id"
            type="button"
            class="row"
            :class="{ active: idx === cursor }"
            @mouseenter="cursor = idx"
            @click="run(item)"
          >
            <el-icon><component :is="item.icon || 'Right'" /></el-icon>
            <span class="main">
              <b>{{ item.title }}</b>
              <small>{{ item.hint }}</small>
            </span>
            <em>{{ item.group }}</em>
          </button>
          <div v-if="!filtered.length" class="empty">没有匹配的命令</div>
        </div>
        <div class="foot">↑↓ 选择 · Enter 执行 · Ctrl/⌘ K 开关</div>
      </div>
    </div>
  </teleport>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { flatMenus } from '@/config/nav'

const router = useRouter()
const user = useUserStore()
const theme = useThemeStore()
const open = ref(false)
const q = ref('')
const cursor = ref(0)
const inputRef = ref(null)

const commands = computed(() => {
  const nav = flatMenus(user.roles).map(m => ({
    id: 'nav-' + m.path,
    title: m.title,
    hint: m.hint,
    group: m.group,
    icon: m.icon,
    run: () => router.push(m.path)
  }))
  const extra = [
    { id: 'new-req', title: '新建采购申请', hint: '进入申请编辑页', group: '操作', icon: 'Plus', roles: ['ADMIN', 'APPLICANT'], run: () => router.push('/requests/edit') },
    { id: 'theme', title: theme.dark ? '切换为浅色' : '切换为暗色工业风', hint: '立即切换主题', group: '操作', icon: theme.dark ? 'Sunny' : 'Moon', roles: ['ADMIN', 'APPLICANT', 'DEPT_MANAGER', 'PURCHASE_MANAGER', 'WAREHOUSE', 'FINANCE'], run: () => theme.toggle() },
    { id: 'ops', title: '打开运行中心', hint: '任务、健康、审计', group: '操作', icon: 'Monitor', roles: ['ADMIN'], run: () => router.push('/ops') },
    { id: 'supplier', title: '查看华信供应商画像', hint: '雷达图与风险标签', group: '操作', icon: 'PieChart', roles: ['ADMIN', 'PURCHASE_MANAGER'], run: () => router.push('/suppliers/1/profile') }
  ].filter(c => c.roles.some(r => user.roles.includes(r)))
  return [...nav, ...extra]
})

const filtered = computed(() => {
  const key = q.value.trim().toLowerCase()
  const list = !key ? commands.value : commands.value.filter(c => (c.title + c.hint + c.group).toLowerCase().includes(key))
  return list.slice(0, 12)
})

function openPalette() {
  open.value = true
  q.value = ''
  cursor.value = 0
  nextTick(() => inputRef.value?.focus())
}
function close() { open.value = false }
function move(step) {
  if (!filtered.value.length) return
  cursor.value = (cursor.value + step + filtered.value.length) % filtered.value.length
}
function runActive() {
  const item = filtered.value[cursor.value]
  if (item) run(item)
}
function run(item) {
  close()
  item.run()
}
function onKey(e) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    open.value ? close() : openPalette()
  }
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
defineExpose({ open: openPalette })
</script>
<style scoped>
.mask {
  position: fixed; inset: 0; z-index: 3000;
  background: rgba(2, 6, 23, 0.55);
  display: flex; justify-content: center; padding-top: 12vh;
  backdrop-filter: blur(6px);
}
.panel {
  width: min(640px, 92vw);
  background: var(--ip-surface);
  border: 1px solid var(--ip-border);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0,0,0,.35);
  overflow: hidden;
}
.search {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 16px; border-bottom: 1px solid var(--ip-border);
}
.search input {
  flex: 1; border: 0; outline: none; background: transparent;
  color: var(--ip-text); font-size: 16px;
}
kbd, .foot { color: var(--ip-text-secondary); font-size: 12px; }
.list { max-height: 380px; overflow: auto; padding: 8px; }
.row {
  width: 100%; display: flex; align-items: center; gap: 10px;
  border: 0; background: transparent; color: var(--ip-text);
  text-align: left; border-radius: 10px; padding: 10px 12px; cursor: pointer;
}
.row.active, .row:hover { background: var(--ip-accent-soft); }
.main { flex: 1; display: flex; flex-direction: column; }
.main small, em { color: var(--ip-text-secondary); font-style: normal; font-size: 12px; }
.empty { padding: 28px; text-align: center; color: var(--ip-text-secondary); }
.foot { padding: 8px 16px 12px; }
</style>
