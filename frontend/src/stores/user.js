import { defineStore } from 'pinia'
import { login as loginApi, me } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    roles: (s) => s.user?.roles || [],
    realName: (s) => s.user?.realName || '',
    hasRole: (s) => (role) => (s.user?.roles || []).includes(role),
    hasAnyRole: (s) => (...roles) => roles.some(r => (s.user?.roles || []).includes(r))
  },
  actions: {
    async login(username, password) {
      const data = await loginApi({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    async fetchMe() {
      const data = await me()
      this.user = data
      localStorage.setItem('user', JSON.stringify(data))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
