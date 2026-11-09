import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const http = axios.create({ baseURL: '/', timeout: 60000 })

http.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) config.headers.Authorization = `Bearer ${store.token}`
  return config
})

http.interceptors.response.use(
  (resp) => {
    // blob export
    if (resp.config.responseType === 'blob') return resp.data
    const data = resp.data
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data.data
  },
  (err) => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    if (err.response?.status === 401 || err.response?.status === 403) {
      ElMessage.error('登录已失效，请重新登录')
      const store = useUserStore()
      store.logout()
      router.push('/login')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default http
