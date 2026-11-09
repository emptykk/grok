<template>
  <div class="empty-box">
    <svg class="illu" viewBox="0 0 120 80" aria-hidden="true">
      <rect x="18" y="16" width="84" height="52" rx="8" fill="none" stroke="currentColor" stroke-width="1.6" />
      <path d="M30 32h36M30 42h28M30 52h18" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
      <circle cx="86" cy="48" r="10" fill="none" stroke="currentColor" stroke-width="1.6" />
      <path d="M93 55l8 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
    </svg>
    <div class="title">{{ title }}</div>
    <div class="desc">{{ description }}</div>
    <div class="actions">
      <el-button v-if="actionText" type="primary" @click="go">{{ actionText }}</el-button>
      <slot />
    </div>
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
const props = defineProps({
  title: { type: String, default: '这里还是空的' },
  description: { type: String, default: '当前筛选下没有记录。可以新建，或换个条件再查。' },
  actionText: { type: String, default: '' },
  actionTo: { type: String, default: '' }
})
const router = useRouter()
function go() {
  if (props.actionTo) router.push(props.actionTo)
}
</script>
<style scoped>
.empty-box { padding: 36px 20px 28px; }
.illu { width: 120px; height: 80px; color: var(--ip-accent); opacity: .85; margin-bottom: 8px; }
.desc { max-width: 420px; margin: 0 auto; line-height: 1.6; }
.actions { margin-top: 14px; display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
</style>
