<template>
  <el-tooltip :content="hint" placement="bottom">
    <span class="scope" :class="scope.code">数据范围 · {{ scope.label }}</span>
  </el-tooltip>
</template>
<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { resolveScope } from '@/config/nav'

const user = useUserStore()
const scope = computed(() => resolveScope(user.user))
const hint = computed(() => {
  if (scope.value.code === 'ALL') return '当前角色可查看全部业务数据（ALL）'
  if (scope.value.code === 'DEPT') return '当前角色仅查看本部门数据（DEPT）'
  return '当前角色仅查看本人单据（SELF）'
})
</script>
<style scoped>
.scope {
  font-size: 12px; font-weight: 700;
  padding: 4px 10px; border-radius: 999px;
  border: 1px solid var(--ip-border);
  color: var(--ip-accent);
  background: var(--ip-accent-soft);
  letter-spacing: .02em;
}
.scope.SELF { color: #b45309; background: rgba(245, 158, 11, .15); }
.scope.DEPT { color: #1d4ed8; background: rgba(59, 130, 246, .14); }
html.dark .scope.SELF { color: #fbbf24; }
html.dark .scope.DEPT { color: #93c5fd; }
</style>
