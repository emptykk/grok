import { defineStore } from 'pinia'
import http from '@/utils/request'

export const useNotifyStore = defineStore('notify', {
  state: () => ({ unread: 0, timer: null }),
  actions: {
    async refresh() {
      try {
        const data = await http.get('/api/messages/unread-count')
        this.unread = Number(data?.count || 0)
      } catch {
        this.unread = 0
      }
    },
    start() {
      this.refresh()
      if (this.timer) clearInterval(this.timer)
      this.timer = setInterval(() => this.refresh(), 20000)
    },
    stop() {
      if (this.timer) clearInterval(this.timer)
      this.timer = null
    }
  }
})
