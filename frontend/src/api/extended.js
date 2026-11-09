import http from '@/utils/request'
import { useThemeStore } from '@/stores/theme'
import {
  mockNotifications, mockBudgets, mockContracts, mockReturns,
  mockInvoices, mockSupplierAudits, mockTemplates, mockAnomalies,
  mockSupplierProfile
} from '@/mock/data'

/** Prefer real API; only fall back to mock when theme.useMock is true. Default useMock=false. */
async function tryApi(fn, fallback) {
  try {
    return await fn()
  } catch (e) {
    let useMock = false
    try {
      useMock = !!useThemeStore().useMock
    } catch (_) {
      useMock = localStorage.getItem('ip-use-mock') === '1'
    }
    if (!useMock) throw e
    console.warn('[extended] API unavailable, using mock', e?.message || e)
    return typeof fallback === 'function' ? fallback() : fallback
  }
}

function asPage(data, mapRow) {
  if (Array.isArray(data)) {
    const records = mapRow ? data.map(mapRow) : data
    return { records, total: records.length }
  }
  const records = (data?.records || []).map(mapRow || (x => x))
  return { records, total: data?.total ?? records.length }
}

function money(v) {
  if (v == null || v === '') return 0
  return Number(v)
}

function messageLink(m) {
  const t = String(m.bizType || '').toUpperCase()
  if (t.includes('APPROVAL') || t.includes('REQUEST')) return '/approvals'
  if (t.includes('INVENTORY') || t.includes('STOCK') || t.includes('ALERT')) return '/inventories'
  if (t.includes('CONTRACT')) return '/contracts'
  if (t.includes('ORDER') || t.includes('PAYMENT') || t.includes('INVOICE')) return '/orders'
  if (t.includes('RETURN')) return '/returns'
  return '/notifications'
}

function mapBudget(b) {
  const total = money(b.totalAmount)
  const used = money(b.usedAmount)
  const remain = Math.max(0, total - used)
  const ratio = total > 0 ? used / total : 0
  let status = '正常'
  if (ratio >= 0.98) status = '超限风险'
  else if (ratio >= 0.8) status = '偏紧'
  return {
    ...b,
    dept: b.deptName,
    year: b.budgetYear,
    total,
    used,
    remain,
    status: b.status === 0 ? '停用' : status
  }
}

function mapContract(c) {
  return {
    ...c,
    no: c.contractNo,
    supplier: c.supplierName || (c.supplierId != null ? `供应商#${c.supplierId}` : '-'),
    end: c.endDate,
    start: c.startDate,
    amount: money(c.amount)
  }
}

function mapReturn(r) {
  return {
    ...r,
    no: r.returnNo,
    orderNo: r.orderNo || (r.orderId != null ? `订单#${r.orderId}` : '-'),
    material: r.materialName || r.reason || '-',
    qty: r.quantity ?? r.qty ?? '-',
    type: r.type === 'EXCHANGE' ? '换货' : (r.type === 'RETURN' ? '退货' : r.type),
    createdAt: r.createdAt ? String(r.createdAt).slice(0, 10) : ''
  }
}

function mapInvoice(inv) {
  return {
    ...inv,
    no: inv.invoiceNo,
    orderNo: inv.orderNo || (inv.orderId != null ? `订单#${inv.orderId}` : '-'),
    supplier: inv.supplierName || (inv.supplierId != null ? `供应商#${inv.supplierId}` : '-'),
    amount: money(inv.amount),
    tax: money(inv.taxAmount),
    payStatus: inv.payStatus || '-'
  }
}

function mapTemplate(t) {
  const parts = []
  if (t.materialName) parts.push(`${t.materialName}×${t.defaultQty ?? 1}`)
  return {
    ...t,
    items: t.items || parts.join(', ') || t.spec || '-',
    usage: t.useCount ?? 0
  }
}

function mapSupplierAudit(s) {
  const score = money(s.admissionScore ?? s.rating * 20)
  let level = 'C'
  if (score >= 90) level = 'A'
  else if (score >= 80) level = 'B'
  else if (score >= 60) level = 'C'
  else level = 'D'
  return {
    id: s.id,
    name: s.name,
    score,
    level,
    blacklist: s.blacklisted === 1 || s.blacklisted === true ? '是' : '否',
    lastAudit: s.lastReviewDate || '-',
    nextAudit: s.nextReviewDate || '-',
    note: s.blacklistReason || s.remark || ''
  }
}

function mapMessage(m) {
  return {
    id: m.id,
    title: m.title,
    content: m.content,
    type: m.bizType || '系统',
    read: m.readFlag === 1 || m.readFlag === true,
    time: m.createdAt ? String(m.createdAt).replace('T', ' ').slice(0, 16) : '',
    link: messageLink(m)
  }
}

export const listNotifications = () => tryApi(async () => {
  const data = await http.get('/api/messages', { params: { page: 1, size: 50 } })
  const page = asPage(data, mapMessage)
  return page.records
}, mockNotifications)

export const markNotificationRead = (id) => tryApi(
  () => http.post(`/api/messages/${id}/read`),
  { ok: true }
)

export const markAllNotificationsRead = () => tryApi(
  () => http.post('/api/messages/read-all'),
  { ok: true }
)

export const unreadCount = () => tryApi(
  () => http.get('/api/messages/unread-count'),
  { count: 0 }
)

export const pageBudgets = (params) => tryApi(async () => {
  const data = await http.get('/api/budgets', { params })
  return asPage(data, mapBudget)
}, { records: mockBudgets, total: mockBudgets.length })

export const pageContracts = (params) => tryApi(async () => {
  const data = await http.get('/api/contracts', { params })
  return asPage(data, mapContract)
}, { records: mockContracts, total: mockContracts.length })

export const summarizeContract = (id) => tryApi(async () => {
  const data = await http.post(`/api/contracts/${id}/risk-summary`)
  const summary = data?.summary || data?.riskSummary || (typeof data === 'string' ? data : JSON.stringify(data))
  return { summary, risks: data?.risks || [] }
}, { summary: '合同主体权利义务清晰；建议关注交期与违约责任上限条款。', risks: ['交期弹性过大', '缺少质量验收细则'] })

export const pageReturns = (params) => tryApi(async () => {
  const data = await http.get('/api/returns', { params })
  return asPage(data, mapReturn)
}, { records: mockReturns, total: mockReturns.length })

export const pageInvoices = (params) => tryApi(async () => {
  const data = await http.get('/api/invoices', { params })
  const page = asPage(data, mapInvoice)
  // enrich payStatus from payments when available
  try {
    const pays = await http.get('/api/payments')
    const list = Array.isArray(pays) ? pays : (pays?.records || [])
    const byInvoice = {}
    for (const p of list) {
      if (p.invoiceId != null) byInvoice[p.invoiceId] = p.status || p.payStatus || '申请中'
    }
    page.records = page.records.map(r => ({
      ...r,
      payStatus: byInvoice[r.id] || r.payStatus || '-'
    }))
  } catch (_) { /* optional */ }
  return page
}, { records: mockInvoices, total: mockInvoices.length })

export const pageSupplierAudits = (params) => tryApi(async () => {
  const data = await http.get('/api/suppliers', { params: { page: params?.page || 1, size: params?.size || 50, keyword: params?.keyword } })
  return asPage(data, mapSupplierAudit)
}, { records: mockSupplierAudits, total: mockSupplierAudits.length })

export const getSupplierProfile = (id) => tryApi(async () => {
  const data = await http.get('/api/suppliers', { params: { page: 1, size: 200 } })
  const records = data?.records || (Array.isArray(data) ? data : [])
  const s = records.find(x => String(x.id) === String(id))
  if (!s) throw new Error('supplier not found')
  let scores = []
  try { scores = await http.get(`/api/suppliers/${id}/scores`) || [] } catch (_) { scores = [] }
  const scoreVal = Math.min(100, money(s.admissionScore ?? (s.rating || 3) * 18))
  return {
    id: s.id,
    name: s.name,
    rating: s.rating,
    category: s.category,
    contact: `${s.contact || '-'} / ${s.phone || '-'}`,
    address: s.remark || '-',
    cooperationYears: 1,
    orderCount: Array.isArray(scores) ? scores.length : 0,
    totalAmount: 0,
    onTimeRate: 0.92,
    qualifyRate: 0.95,
    admissionScore: s.admissionScore,
    blacklisted: s.blacklisted,
    lastReviewDate: s.lastReviewDate,
    nextReviewDate: s.nextReviewDate,
    radar: [
      { name: '价格', value: Math.max(40, scoreVal - 8) },
      { name: '交期', value: Math.max(40, scoreVal - 4) },
      { name: '质量', value: scoreVal },
      { name: '服务', value: Math.max(40, scoreVal - 6) },
      { name: '合规', value: s.blacklisted ? 20 : Math.max(50, scoreVal) }
    ],
    recentOrders: [],
    scores
  }
}, () => ({ ...mockSupplierProfile, id: Number(id) || 1 }))

export const pageTemplates = (params) => tryApi(async () => {
  const data = await http.get('/api/material-templates', { params })
  return asPage(data, mapTemplate)
}, { records: mockTemplates, total: mockTemplates.length })

/** Contract: POST /api/ai/anomaly-analysis → adapt to card list for UI */
export const listAnomalies = () => tryApi(async () => {
  const data = await http.post('/api/ai/anomaly-analysis', { months: 3 })
  const analysis = data?.analysis || ''
  const stats = data?.stats || ''
  const total = data?.orderTotal
  const cards = [
    {
      id: 'summary',
      title: `近 ${data?.months || 3} 个月采购异常解读`,
      level: '中',
      metric: total != null ? `¥${Number(total).toLocaleString()}` : '—',
      desc: stats || '基于订单与供应商分布的规则+大模型解读。',
      advice: analysis || '暂无异常结论。',
      raw: data
    }
  ]
  const map = data?.supplierAmountMap || {}
  const entries = Object.entries(map)
  if (entries.length) {
    const sum = entries.reduce((a, [, v]) => a + Number(v || 0), 0) || 1
    const top = entries.sort((a, b) => Number(b[1]) - Number(a[1]))[0]
    const pct = Math.round((Number(top[1]) / sum) * 100)
    cards.push({
      id: 'concentration',
      title: '供应商集中度',
      level: pct >= 60 ? '高' : '中',
      metric: `${pct}%`,
      desc: `供应商 #${top[0]} 近窗金额占比约 ${pct}%。`,
      advice: pct >= 60 ? '建议引入备选供应商并完成比价。' : '集中度尚可，持续观察。',
      raw: data
    })
  }
  return cards
}, mockAnomalies)

export const interpretAnomaly = (id) => tryApi(async () => {
  const data = await http.post('/api/ai/anomaly-analysis', { months: 3 })
  return { text: data?.analysis || data?.stats || JSON.stringify(data), id }
}, { text: '结合历史成交与部门预算占用，建议对本月相关申请追加说明并由采购负责人复核。' })
