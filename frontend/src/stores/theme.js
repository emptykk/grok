import { defineStore } from 'pinia'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    dark: localStorage.getItem('ip-theme') === 'dark',
    // Prefer real API by default; mock only when explicitly enabled
    useMock: localStorage.getItem('ip-use-mock') === '1'
  }),
  actions: {
    apply() {
      document.documentElement.classList.toggle('dark', this.dark)
    },
    toggle() {
      this.dark = !this.dark
      localStorage.setItem('ip-theme', this.dark ? 'dark' : 'light')
      this.apply()
    },
    setMock(v) {
      this.useMock = !!v
      localStorage.setItem('ip-use-mock', v ? '1' : '0')
    }
  }
})
