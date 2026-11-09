import http from '@/utils/request'

export const getDashboard = () => http.get('/api/dashboard/summary')
export const getProcessBoard = () => http.get('/api/dashboard/process')

export const pageMaterials = (params) => http.get('/api/materials', { params })
export const saveMaterial = (data) => data.id ? http.put(`/api/materials/${data.id}`, data) : http.post('/api/materials', data)
export const deleteMaterial = (id) => http.delete(`/api/materials/${id}`)

export const pageSuppliers = (params) => http.get('/api/suppliers', { params })
export const saveSupplier = (data) => data.id ? http.put(`/api/suppliers/${data.id}`, data) : http.post('/api/suppliers', data)
export const deleteSupplier = (id) => http.delete(`/api/suppliers/${id}`)

export const listInventories = () => http.get('/api/inventories')

export const pageRequests = (params) => http.get('/api/requests', { params })
export const getRequest = (id) => http.get(`/api/requests/${id}`)
export const getRequestTrace = (id) => http.get(`/api/requests/${id}/trace`)
export const saveRequest = (data) => data.id ? http.put(`/api/requests/${data.id}`, data) : http.post('/api/requests', data)
export const submitRequest = (id) => http.post(`/api/requests/${id}/submit`)
export const withdrawRequest = (id) => http.post(`/api/requests/${id}/withdraw`)
export const deleteRequest = (id) => http.delete(`/api/requests/${id}`)

export const approvalTodo = () => http.get('/api/approvals/todo')
export const getApprovalTimeline = (requestId) => http.get(`/api/approvals/${requestId}/timeline`)
export const doApprove = (requestId, data) => http.post(`/api/approvals/${requestId}`, data)

export const createInquiry = (requestId) => http.post('/api/inquiries', { requestId })
export const inquiryByRequest = (requestId) => http.get(`/api/inquiries/by-request/${requestId}`)
export const addQuote = (id, data) => http.post(`/api/inquiries/${id}/quotes`, data)
export const selectSupplier = (id, supplierId) => http.post(`/api/inquiries/${id}/select`, { supplierId })
export const recommendSuppliers = (id) => http.post(`/api/inquiries/${id}/recommend`)

export const pageOrders = (params) => http.get('/api/orders', { params })
export const getOrder = (id) => http.get(`/api/orders/${id}`)
export const createOrder = (data) => http.post('/api/orders', data)

export const listInbounds = () => http.get('/api/inbounds')
export const createInbound = (data) => http.post('/api/inbounds', data)

export const aiGenerateRequest = (prompt) => http.post('/api/ai/generate-request', { prompt })
export const listKnowledgeDocs = () => http.get('/api/knowledge/docs')
export const askKnowledge = (question) => http.post('/api/knowledge/ask', { question })
export const deleteKnowledgeDoc = (id) => http.delete(`/api/knowledge/docs/${id}`)
export const knowledgeHistory = (params) => http.get('/api/knowledge/qa-history', { params })
export const knowledgeQa = (id) => http.get(`/api/knowledge/qa-history/${id}`)

export const pageUsers = (params) => http.get('/api/users', { params })
export const listRoles = () => http.get('/api/roles')
export const pageLogs = (params) => http.get('/api/logs', { params })
export const getLogDetail = (id) => http.get(`/api/logs/${id}`)

export const aiFormAssist = (data) => http.post('/api/ai/form-assist', data)
export const aiApprovalAssist = (requestId) => http.post('/api/ai/approval-assist', { requestId })
export const aiInquiryVerdict = (inquiryId, persist = true) => http.post('/api/ai/inquiry-verdict', { inquiryId, persist })
export const aiWeeklyReport = (days = 7) => http.post('/api/ai/weekly-report', { days })
export const getAiConfig = () => http.get('/api/ai/config')
export const saveAiConfig = (data) => http.put('/api/ai/config', data)
export const aiAnomaly = (months = 3) => http.post('/api/ai/anomaly-analysis', { months })
export const getOpsOverview = () => http.get('/api/ops/overview')
export const listApprovalRules = () => http.get('/api/approval-rules')
export const resolveApprovalRule = (amount) => http.get('/api/approval-rules/resolve', { params: { amount } })
export const runInventoryJob = () => http.post('/api/jobs/inventory-alert')
export const runContractJob = () => http.post('/api/jobs/contract-expiry')
export const supplierProfile = (id) => http.get(`/api/suppliers/${id}/profile`)
