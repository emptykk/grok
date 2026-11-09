import http from '@/utils/request'
export const login = (data) => http.post('/api/auth/login', data)
export const me = () => http.get('/api/auth/me')
export const authBootstrap = () => http.get('/api/auth/bootstrap')
