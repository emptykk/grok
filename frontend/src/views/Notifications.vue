<template>
  <div class="ip-page">
    <PageHeader title="消息中心" sub="未读会同步到顶栏角标。打开一条即标记已读。">
      <el-radio-group v-model="filter" size="small">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="unread">未读 {{ unreadLocal }}</el-radio-button>
      </el-radio-group>
      <el-button :disabled="!unreadLocal" @click="readAll">全部已读</el-button>
      <el-button @click="load">刷新</el-button>
    </PageHeader>
    <div class="ip-card" v-auto-animate>
      <div v-for="n in shown" :key="n.id" class="item" :class="{ unread: !n.read }" @click="open(n)">
        <div class="dot" />
        <div class="left">
          <el-tag size="small" effect="plain">{{ n.type }}</el-tag>
          <div class="title">{{ n.title }}</div>
          <div class="content">{{ n.content }}</div>
          <div class="time">{{ n.time }}</div>
        </div>
        <el-tag v-if="!n.read" type="danger" size="small">未读</el-tag>
        <el-tag v-else type="info" size="small">已读</el-tag>
      </div>
      <EmptyState v-if="!shown.length" title="没有消息" description="审批、库存预警和合同到期会写到这里" />
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listNotifications, markNotificationRead, markAllNotificationsRead } from '@/api/extended'
import { useNotifyStore } from '@/stores/notify'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { burstConfetti } from '@/utils/confetti'

const list = ref([])
const filter = ref('all')
const router = useRouter()
const notify = useNotifyStore()
const unreadLocal = computed(() => list.value.filter(n => !n.read).length)
const shown = computed(() => filter.value === 'unread' ? list.value.filter(n => !n.read) : list.value)

async function load() {
  list.value = await listNotifications()
  await notify.refresh()
}
async function open(n) {
  if (!n.read) {
    await markNotificationRead(n.id)
    n.read = true
    notify.refresh()
  }
  if (n.link && n.link !== '/notifications') router.push(n.link)
}
async function readAll() {
  await markAllNotificationsRead()
  list.value.forEach(n => { n.read = true })
  notify.refresh()
  burstConfetti()
}
onMounted(load)
</script>
<style scoped>
.item {
  display:flex; align-items:flex-start; gap:12px;
  padding: 14px 16px; border-bottom: 1px solid var(--ip-border);
  cursor: pointer; transition: background 150ms ease;
}
.item:hover { background: var(--ip-surface-2); }
.item.unread { background: var(--ip-accent-soft); }
.dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 8px; background: transparent; flex: none; }
.item.unread .dot { background: var(--ip-danger); }
.left { flex: 1; }
.title { font-weight: 700; margin: 6px 0 2px; }
.content { font-size: 13px; color: var(--ip-text-secondary); line-height: 1.5; }
.time { font-size: 12px; color: var(--ip-text-secondary); margin-top: 4px; }
</style>
