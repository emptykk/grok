<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '72px' : 'var(--ip-aside-w)'" class="aside">
      <div class="logo" @click="$router.push('/dashboard')">
        <div class="mark">采</div>
        <div v-if="!collapsed" class="logo-text">
          <div class="t1">智能采购</div>
          <div class="t2">Procurement AI</div>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="active"
          :collapse="collapsed"
          router
          background-color="transparent"
          text-color="var(--ip-sidebar-text)"
          active-text-color="#fff"
        >
          <template v-for="g in menuGroups" :key="g.title">
            <div v-if="!collapsed && g.items.length" class="group-title">{{ g.title }}</div>
            <el-menu-item v-for="m in g.items" :key="m.path" :index="m.path">
              <el-icon><component :is="m.icon" /></el-icon>
              <template #title>{{ m.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="left">
          <el-button text @click="collapsed = !collapsed">
            <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <div class="crumb">{{ title }}</div>
        </div>
        <div class="right">
          <button type="button" class="cmdk" @click="palette?.open()">
            <span>搜索</span><kbd>Ctrl K</kbd>
          </button>
          <el-button text @click="help?.open()">?</el-button>
          <el-button text class="hide-narrow" @click="tour?.start(true)">答辩导览</el-button>
          <DataScopeChip />
          <el-tooltip content="暗色工业风 / 浅色">
            <el-button text @click="theme.toggle()">
              <el-icon><Moon v-if="!theme.dark" /><Sunny v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <el-badge :value="notify.unread" :hidden="!notify.unread" class="badge">
            <el-button text @click="openBell">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown>
            <span class="user">
              <span class="avatar">{{ (user.realName || 'U').slice(0,1) }}</span>
              <span v-if="!collapsedUser">{{ user.realName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/ai')">AI 助手</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <el-drawer v-model="bellOpen" title="最近消息" size="380px">
      <div class="bell-actions">
        <el-button size="small" :disabled="!notify.unread" @click="readAllBell">全部已读</el-button>
        <el-button size="small" type="primary" @click="goInbox">打开消息中心</el-button>
      </div>
      <button v-for="n in bellList" :key="n.id" type="button" class="bell-item" :class="{ unread: !n.read }" @click="openBellItem(n)">
        <b>{{ n.title }}</b>
        <span>{{ n.content }}</span>
        <em>{{ n.time }} · {{ n.read ? '已读' : '未读' }}</em>
      </button>
      <EmptyState v-if="!bellList.length" title="没有新消息" description="审批、库存和合同事件会出现在这里。" />
    </el-drawer>
    <ShortcutHelp ref="help" />
    <CommandPalette ref="palette" />
    <DefenseTour ref="tour" />
  </el-container>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useNotifyStore } from '@/stores/notify'
import { visibleGroups } from '@/config/nav'
import CommandPalette from '@/components/CommandPalette.vue'
import ShortcutHelp from '@/components/ShortcutHelp.vue'
import DefenseTour from '@/components/DefenseTour.vue'
import DataScopeChip from '@/components/DataScopeChip.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/extended'
import { burstConfetti } from '@/utils/confetti'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const theme = useThemeStore()
const notify = useNotifyStore()
const palette = ref(null)
const help = ref(null)
const tour = ref(null)
const bellOpen = ref(false)
const bellList = ref([])
const user = computed(() => store.user || {})
const title = computed(() => route.meta.title || '')
const active = computed(() => {
  if (route.path.startsWith('/inquiry')) return '/orders'
  if (route.path.startsWith('/trace')) return '/trace/1'
  if (route.path.startsWith('/suppliers/compare')) return '/suppliers/compare'
  if (route.path.startsWith('/suppliers/') && route.path.includes('profile')) return '/suppliers'
  return route.path
})
const narrow = useMediaQuery('(max-width: 900px)')
const collapsed = ref(false)
watch(narrow, (v) => { if (v) collapsed.value = true }, { immediate: true })
const collapsedUser = ref(false)
const menuGroups = computed(() => visibleGroups(store.roles))

function logout() {
  store.logout()
  router.push('/login')
}
async function openBell() {
  bellOpen.value = true
  bellList.value = await listNotifications()
}
async function readAllBell() {
  await markAllNotificationsRead()
  bellList.value.forEach(n => { n.read = true })
  notify.refresh()
  burstConfetti()
}
async function openBellItem(n) {
  if (!n.read) {
    await markNotificationRead(n.id)
    n.read = true
    notify.refresh()
  }
  bellOpen.value = false
  if (n.link && n.link !== '/notifications') router.push(n.link)
}
function goInbox() {
  bellOpen.value = false
  router.push('/notifications')
}

onMounted(async () => {
  theme.apply()
  try { await store.fetchMe() } catch { /* keep cached user */ }
  notify.start()
  setTimeout(() => tour.value?.start(false), 700)
})
onBeforeUnmount(() => notify.stop())
</script>
<style scoped>
.layout { height: 100%; background: var(--ip-bg); }
.aside {
  background: var(--ip-sidebar);
  border-right: 1px solid rgba(255,255,255,0.04);
  transition: width 200ms ease;
  display: flex;
  flex-direction: column;
}
.logo {
  height: var(--ip-header-h);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.mark {
  width: 34px; height: 34px; border-radius: 10px; flex: none;
  display:flex; align-items:center; justify-content:center;
  font-weight: 800; color: #fff;
  background: linear-gradient(135deg, #0d9488, #14b8a6);
}
.logo-text .t1 { color: #fff; font-weight: 750; font-size: 14px; line-height: 1.1; }
.logo-text .t2 { color: #64748b; font-size: 11px; }
.menu-scroll { flex: 1; padding-bottom: 16px; }
.group-title {
  color: #64748b;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 16px 20px 6px;
}
:deep(.el-menu) { border-right: none; }
:deep(.el-menu-item) {
  margin: 2px 8px;
  border-radius: 10px;
  height: 42px;
}
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(13,148,136,.35), rgba(13,148,136,.08)) !important;
  color: #fff !important;
}
.header {
  height: auto;
  min-height: var(--ip-header-h);
  background: color-mix(in srgb, var(--ip-surface) 88%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--ip-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.left, .right { display:flex; align-items:center; gap: 8px; }
.crumb { font-weight: 700; letter-spacing: -0.01em; }
.user { display:flex; align-items:center; gap:8px; cursor:pointer; }
.avatar {
  width: 30px; height: 30px; border-radius: 9px;
  display:flex; align-items:center; justify-content:center;
  background: var(--ip-accent-soft); color: var(--ip-accent); font-weight: 750; font-size: 13px;
}
.main { padding: 18px 20px 28px; background: var(--ip-bg); }
.badge :deep(.el-badge__content) { transform: translateY(4px); }
.cmdk {
  display: flex; align-items: center; gap: 8px;
  border: 1px solid var(--ip-border); background: var(--ip-surface-2); color: var(--ip-text-secondary);
  border-radius: 999px; padding: 4px 10px; cursor: pointer;
}
.cmdk kbd { font-size: 11px; border: 1px solid var(--ip-border); border-radius: 6px; padding: 1px 5px; }
.bell-actions { display: flex; gap: 8px; margin-bottom: 10px; }
.bell-item {
  width: 100%; text-align: left; border: 0; border-bottom: 1px solid var(--ip-border);
  background: transparent; color: inherit; padding: 10px 4px; cursor: pointer; display: flex; flex-direction: column; gap: 2px;
}
.bell-item.unread { background: var(--ip-accent-soft); border-radius: 8px; padding: 10px; }
.bell-item span, .bell-item em { color: var(--ip-text-secondary); font-size: 12px; font-style: normal; }
@media (max-width: 900px) {
  .cmdk span, .hide-narrow { display: none; }
  .right { flex-wrap: wrap; justify-content: flex-end; }
  .main { padding: 12px 12px 24px; }
  .crumb { font-size: 14px; }
}
</style>
