export const ALL_ROLES = ['ADMIN', 'APPLICANT', 'DEPT_MANAGER', 'PURCHASE_MANAGER', 'WAREHOUSE', 'FINANCE']

export const menuGroups = [
  {
    title: '总览',
    items: [
      { path: '/dashboard', title: '工作台', icon: 'Odometer', roles: ALL_ROLES, hint: 'KPI、趋势、快捷入口' },
      { path: '/demo-script', title: '演示脚本', icon: 'Guide', roles: ALL_ROLES, hint: '答辩 checklist' },
      { path: '/defense-brief', title: '答辩一页纸', icon: 'Postcard', roles: ALL_ROLES, hint: '技术栈与工作量' },
      { path: '/process', title: '流程总览', icon: 'Share', roles: ALL_ROLES, hint: '采购状态机与数量' },
      { path: '/trace/1', title: '时间机器', icon: 'Timer', roles: ALL_ROLES, hint: '一条申请讲完整故事' },
      { path: '/approval-rules', title: '审批规则', icon: 'Histogram', roles: ALL_ROLES, hint: '金额阈值与级数' },
      { path: '/notifications', title: '消息中心', icon: 'Bell', roles: ALL_ROLES, hint: '已读未读与角标' },
      { path: '/ai', title: 'AI 助手', icon: 'MagicStick', roles: ALL_ROLES, hint: '生成明细与制度问答入口' }
    ]
  },
  {
    title: '采购业务',
    items: [
      { path: '/requests', title: '采购申请', icon: 'Document', roles: ['ADMIN', 'APPLICANT', 'DEPT_MANAGER', 'PURCHASE_MANAGER'], hint: '申请、导出、打印' },
      { path: '/approvals', title: '我的审批', icon: 'Checked', roles: ['ADMIN', 'DEPT_MANAGER', 'PURCHASE_MANAGER', 'FINANCE'], hint: '待办与审批轨迹' },
      { path: '/orders', title: '采购订单', icon: 'List', roles: ['ADMIN', 'PURCHASE_MANAGER', 'WAREHOUSE'], hint: '订单跟踪' },
      { path: '/inbound', title: '入库验收', icon: 'Box', roles: ['ADMIN', 'WAREHOUSE'], hint: '到货与差异' },
      { path: '/returns', title: '退货换货', icon: 'RefreshLeft', roles: ['ADMIN', 'WAREHOUSE', 'PURCHASE_MANAGER'], hint: '退换货闭环' }
    ]
  },
  {
    title: '财务与合约',
    items: [
      { path: '/budgets', title: '预算管理', icon: 'Wallet', roles: ['ADMIN', 'DEPT_MANAGER', 'PURCHASE_MANAGER', 'FINANCE'], hint: '占用与导出' },
      { path: '/contracts', title: '合同管理', icon: 'Notebook', roles: ['ADMIN', 'PURCHASE_MANAGER', 'FINANCE'], hint: '到期与风险摘要' },
      { path: '/invoices', title: '发票付款', icon: 'Ticket', roles: ['ADMIN', 'PURCHASE_MANAGER', 'FINANCE'], hint: '对账与付款' }
    ]
  },
  {
    title: '主数据 / 智能',
    items: [
      { path: '/materials', title: '物资档案', icon: 'Goods', roles: ['ADMIN', 'PURCHASE_MANAGER'], hint: '物资主数据' },
      { path: '/suppliers', title: '供应商', icon: 'OfficeBuilding', roles: ['ADMIN', 'PURCHASE_MANAGER'], hint: '名录与画像入口' },
      { path: '/suppliers/compare', title: '供应商对比', icon: 'DataLine', roles: ['ADMIN', 'PURCHASE_MANAGER'], hint: '2-3 家雷达并排' },
      { path: '/supplier-audits', title: '准入年审', icon: 'Stamp', roles: ['ADMIN', 'PURCHASE_MANAGER'], hint: '评分与黑名单' },
      { path: '/inventories', title: '库存预警', icon: 'Warning', roles: ['ADMIN', 'WAREHOUSE', 'PURCHASE_MANAGER'], hint: '低于安全库存' },
      { path: '/templates', title: '填单模板', icon: 'Collection', roles: ['ADMIN', 'APPLICANT', 'PURCHASE_MANAGER'], hint: '常用物资模板' },
      { path: '/knowledge', title: '制度问答', icon: 'ChatDotRound', roles: ALL_ROLES, hint: 'RAG 上传、引用与历史' },
      { path: '/anomaly', title: '异常解读', icon: 'DataAnalysis', roles: ['ADMIN', 'PURCHASE_MANAGER', 'DEPT_MANAGER'], hint: '集中度、预算与突增' }
    ]
  },
  {
    title: '系统',
    items: [
      { path: '/ops', title: '运行中心', icon: 'Monitor', roles: ['ADMIN'], hint: '任务、健康、审计' },
      { path: '/users', title: '用户管理', icon: 'User', roles: ['ADMIN'], hint: '账号与数据范围' },
      { path: '/logs', title: '操作日志', icon: 'DocumentCopy', roles: ['ADMIN'], hint: '筛选、详情、导出' },
      { path: '/ai-settings', title: '大模型配置', icon: 'Cpu', roles: ['ADMIN'], hint: 'DeepSeek 脱敏配置' },
      { path: '/profile', title: '个人中心', icon: 'Setting', roles: ALL_ROLES, hint: '主题与数据范围' }
    ]
  }
]

export function visibleGroups(roles = []) {
  return menuGroups
    .map(g => ({ ...g, items: g.items.filter(m => m.roles.some(r => roles.includes(r))) }))
    .filter(g => g.items.length)
}

export function flatMenus(roles = []) {
  return visibleGroups(roles).flatMap(g => g.items.map(item => ({ ...item, group: g.title })))
}

export const SCOPE_FALLBACK = {
  ADMIN: { code: 'ALL', label: '全部数据' },
  PURCHASE_MANAGER: { code: 'ALL', label: '全部数据' },
  WAREHOUSE: { code: 'ALL', label: '全部数据' },
  FINANCE: { code: 'ALL', label: '全部数据' },
  DEPT_MANAGER: { code: 'DEPT', label: '本部门' },
  APPLICANT: { code: 'SELF', label: '仅本人' }
}

export function resolveScope(user) {
  if (user?.dataScope) {
    return { code: user.dataScope, label: user.dataScopeLabel || user.dataScope }
  }
  const roles = user?.roles || []
  if (roles.includes('ADMIN')) return SCOPE_FALLBACK.ADMIN
  if (roles.includes('DEPT_MANAGER') && roles.length === 1) return SCOPE_FALLBACK.DEPT_MANAGER
  if (roles.includes('APPLICANT') && roles.length === 1) return SCOPE_FALLBACK.APPLICANT
  for (const r of roles) if (SCOPE_FALLBACK[r]) return SCOPE_FALLBACK[r]
  return { code: 'SELF', label: '仅本人' }
}
