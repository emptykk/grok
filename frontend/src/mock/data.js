export const mockNotifications = [
  { id: 1, title: '申请 PR-2026-0018 待您一级审批', type: '审批', read: false, time: '2026-11-06 09:12', link: '/approvals' },
  { id: 2, title: '库存预警：A4 打印纸低于安全库存', type: '预警', read: false, time: '2026-11-06 08:40', link: '/inventories' },
  { id: 3, title: '合同 CT-2026-004 将于 15 天后到期', type: '合同', read: true, time: '2026-11-05 16:20', link: '/contracts' },
  { id: 4, title: 'AI 异常解读：本月办公品类金额环比 +42%', type: '智能', read: false, time: '2026-11-05 11:05', link: '/anomaly' },
  { id: 5, title: '订单 PO-2026-009 已部分入库', type: '订单', read: true, time: '2026-11-04 17:33', link: '/orders' }
]

export const mockBudgets = [
  { id: 1, dept: '市场部', year: 2026, total: 500000, used: 186400, remain: 313600, status: '正常' },
  { id: 2, dept: '研发部', year: 2026, total: 800000, used: 612000, remain: 188000, status: '偏紧' },
  { id: 3, dept: '行政部', year: 2026, total: 200000, used: 198500, remain: 1500, status: '超限风险' },
  { id: 4, dept: '财务部', year: 2026, total: 120000, used: 45300, remain: 74700, status: '正常' }
]

export const mockContracts = [
  { id: 1, no: 'CT-2026-001', title: '办公设备年度框架协议', supplier: '华联办公科技', amount: 280000, start: '2026-01-01', end: '2026-12-31', status: '履行中', risk: '交期条款偏宽' },
  { id: 2, no: 'CT-2026-004', title: '服务器维保合同', supplier: '云启科技', amount: 96000, start: '2026-03-01', end: '2026-11-21', status: '即将到期', risk: '无违约金上限' },
  { id: 3, no: 'CT-2026-007', title: '劳保用品采购合同', supplier: '安盾实业', amount: 45000, start: '2026-06-01', end: '2027-05-31', status: '履行中', risk: '低' }
]

export const mockReturns = [
  { id: 1, no: 'RT-2026-003', orderNo: 'PO-2026-009', material: '人体工学椅', qty: 2, reason: '外观破损', type: '退货', status: '处理中', createdAt: '2026-11-03' },
  { id: 2, no: 'RT-2026-004', orderNo: 'PO-2026-011', material: '激光打印机硒鼓', qty: 5, reason: '型号不符', type: '换货', status: '已完成', createdAt: '2026-10-28' }
]

export const mockInvoices = [
  { id: 1, no: 'INV-2026-021', orderNo: 'PO-2026-008', amount: 36800, tax: 4784, status: '待付款', payStatus: '未申请', supplier: '华联办公科技' },
  { id: 2, no: 'INV-2026-019', orderNo: 'PO-2026-006', amount: 15200, tax: 1976, status: '已核对', payStatus: '审批中', supplier: '安盾实业' },
  { id: 3, no: 'INV-2026-015', orderNo: 'PO-2026-003', amount: 8900, tax: 1157, status: '已核对', payStatus: '已付款', supplier: '云启科技' }
]

export const mockSupplierAudits = [
  { id: 1, name: '华联办公科技', score: 92, level: 'A', blacklist: false, lastAudit: '2026-09-12', nextAudit: '2027-09-12', note: '交付稳定' },
  { id: 2, name: '迅达耗材', score: 61, level: 'C', blacklist: false, lastAudit: '2026-08-01', nextAudit: '2026-12-01', note: '交期波动大，需年审关注' },
  { id: 3, name: '灰鲸贸易', score: 40, level: 'D', blacklist: true, lastAudit: '2026-07-15', nextAudit: '-', note: '质量投诉，已拉黑' }
]

export const mockTemplates = [
  { id: 1, name: '市场部常规办公物资', items: '笔记本×10, 签字笔×50, A4纸×20', usage: 28 },
  { id: 2, name: '新员工入职套装', items: '显示器×1, 键鼠套装×1, 耳机×1', usage: 14 },
  { id: 3, name: '会议室改造清单', items: '投影机×1, 幕布×1, 音响×2', usage: 3 }
]

export const mockAnomalies = [
  { id: 1, title: '办公品类金额环比异常', level: '高', metric: '+42%', desc: '11 月办公耗材采购额显著高于近 6 个月均值，集中在市场部与行政部。', advice: '核查是否存在集中囤货或重复申请；建议对单笔超 2 万走二级复核。' },
  { id: 2, title: '供应商集中度偏高', level: '中', metric: '68%', desc: '近季度 68% 订单落在华联办公科技，存在供应中断风险。', advice: '对同类物资引入至少 1 家备选供应商并完成询价。' },
  { id: 3, title: '单价偏离历史成交', level: '中', metric: '+18%', desc: '人体工学椅本次报价高于近一年均价 18%。', advice: '结合 AI 比价页复核交期与质保差异后再下单。' }
]

export const mockSupplierProfile = {
  id: 1,
  name: '华联办公科技',
  rating: 4.6,
  category: '办公设备/耗材',
  contact: '王经理 / 13800001111',
  address: '上海市浦东新区张江路 88 号',
  cooperationYears: 3,
  orderCount: 46,
  totalAmount: 1286000,
  onTimeRate: 0.94,
  qualifyRate: 0.98,
  radar: [
    { name: '价格', value: 78 },
    { name: '交期', value: 92 },
    { name: '质量', value: 96 },
    { name: '服务', value: 88 },
    { name: '合规', value: 90 }
  ],
  recentOrders: [
    { no: 'PO-2026-011', amount: 23600, date: '2026-10-21', status: '已完成' },
    { no: 'PO-2026-009', amount: 41200, date: '2026-10-08', status: '部分入库' },
    { no: 'PO-2026-006', amount: 15800, date: '2026-09-19', status: '已完成' }
  ]
}

export const mockProcessSteps = [
  { key: 'draft', label: '草稿' },
  { key: 'l1', label: '一级审批' },
  { key: 'l2', label: '二级审批' },
  { key: 'inquiry', label: '询价比价' },
  { key: 'order', label: '下单' },
  { key: 'inbound', label: '入库' },
  { key: 'done', label: '完成' }
]

export const mockTimeline = [
  { step: '提交申请', status: 'done', actor: '张三', time: '2026-11-01 10:12', comment: 'AI 生成明细后微调数量' },
  { step: '一级审批', status: 'done', actor: '李四', time: '2026-11-01 14:20', comment: '同意，注意控制预算' },
  { step: '二级审批', status: 'done', actor: '王五', time: '2026-11-02 09:05', comment: '通过，进入询价' },
  { step: '询价比价', status: 'active', actor: '王五', time: '2026-11-02 15:40', comment: '已收 3 家报价，待选定' },
  { step: '生成订单', status: 'pending', actor: '-', time: '-', comment: '' },
  { step: '入库验收', status: 'pending', actor: '-', time: '-', comment: '' }
]

export const mockTrend = {
  months: ['6月','7月','8月','9月','10月','11月'],
  amounts: [126000, 98000, 142000, 118000, 156000, 189000],
  categories: [
    { name: '办公设备', value: 38 },
    { name: '耗材', value: 24 },
    { name: 'IT 硬件', value: 18 },
    { name: '劳保', value: 12 },
    { name: '其他', value: 8 }
  ]
}

export const mockAiChats = [
  { role: 'assistant', content: '你好，我是采购智能助手。可以帮你：生成采购申请明细、解读制度、分析异常采购。', refs: [] }
]
