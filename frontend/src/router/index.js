import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const allRoles = ['ADMIN','APPLICANT','DEPT_MANAGER','PURCHASE_MANAGER','WAREHOUSE','FINANCE']

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/print/requests/:id', component: () => import('@/views/RequestPrint.vue'), meta: { title: '申请单打印' } },
  { path: '/print/inquiry/:requestId', component: () => import('@/views/InquiryPrint.vue'), meta: { title: '比价表打印' } },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台', roles: allRoles } },
      { path: 'demo-script', component: () => import('@/views/DemoScript.vue'), meta: { title: '演示脚本', roles: allRoles } },
      { path: 'defense-brief', component: () => import('@/views/DefenseBrief.vue'), meta: { title: '答辩一页纸', roles: allRoles } },
      { path: 'approval-rules', component: () => import('@/views/ApprovalRules.vue'), meta: { title: '审批规则', roles: allRoles } },
      { path: 'process', component: () => import('@/views/ProcessOverview.vue'), meta: { title: '流程总览', roles: allRoles } },
      { path: 'trace/:requestId', component: () => import('@/views/Trace.vue'), meta: { title: '全链路时间机器', roles: allRoles } },
      { path: 'suppliers/compare', component: () => import('@/views/SupplierCompare.vue'), meta: { title: '供应商对比', roles: ['ADMIN','PURCHASE_MANAGER'] } },
      { path: 'notifications', component: () => import('@/views/Notifications.vue'), meta: { title: '消息中心', roles: allRoles } },
      { path: 'ai', component: () => import('@/views/AiAssistant.vue'), meta: { title: 'AI 助手', roles: allRoles } },
      { path: 'profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心', roles: allRoles } },

      { path: 'requests', component: () => import('@/views/Requests.vue'), meta: { title: '采购申请', roles: ['ADMIN','APPLICANT','DEPT_MANAGER','PURCHASE_MANAGER'] } },
      { path: 'requests/edit/:id?', component: () => import('@/views/RequestEdit.vue'), meta: { title: '编辑申请', roles: ['ADMIN','APPLICANT'] } },
      { path: 'approvals', component: () => import('@/views/Approvals.vue'), meta: { title: '我的审批', roles: ['ADMIN','DEPT_MANAGER','PURCHASE_MANAGER','FINANCE'] } },
      { path: 'inquiry/:requestId', component: () => import('@/views/Inquiry.vue'), meta: { title: '询价比价', roles: ['ADMIN','PURCHASE_MANAGER'] } },
      { path: 'orders', component: () => import('@/views/Orders.vue'), meta: { title: '采购订单', roles: ['ADMIN','PURCHASE_MANAGER','WAREHOUSE'] } },
      { path: 'inbound/:orderId?', component: () => import('@/views/Inbound.vue'), meta: { title: '入库验收', roles: ['ADMIN','WAREHOUSE'] } },
      { path: 'returns', component: () => import('@/views/Returns.vue'), meta: { title: '退货换货', roles: ['ADMIN','WAREHOUSE','PURCHASE_MANAGER'] } },

      { path: 'budgets', component: () => import('@/views/Budget.vue'), meta: { title: '预算管理', roles: ['ADMIN','DEPT_MANAGER','PURCHASE_MANAGER','FINANCE'] } },
      { path: 'contracts', component: () => import('@/views/Contracts.vue'), meta: { title: '合同管理', roles: ['ADMIN','PURCHASE_MANAGER','FINANCE'] } },
      { path: 'invoices', component: () => import('@/views/Invoices.vue'), meta: { title: '发票付款', roles: ['ADMIN','PURCHASE_MANAGER','FINANCE'] } },

      { path: 'materials', component: () => import('@/views/Materials.vue'), meta: { title: '物资档案', roles: ['ADMIN','PURCHASE_MANAGER'] } },
      { path: 'suppliers', component: () => import('@/views/Suppliers.vue'), meta: { title: '供应商', roles: ['ADMIN','PURCHASE_MANAGER'] } },
      { path: 'suppliers/:id/profile', component: () => import('@/views/SupplierProfile.vue'), meta: { title: '供应商画像', roles: ['ADMIN','PURCHASE_MANAGER'] } },
      { path: 'supplier-audits', component: () => import('@/views/SupplierAudit.vue'), meta: { title: '准入年审', roles: ['ADMIN','PURCHASE_MANAGER'] } },
      { path: 'inventories', component: () => import('@/views/Inventories.vue'), meta: { title: '库存预警', roles: ['ADMIN','WAREHOUSE','PURCHASE_MANAGER'] } },
      { path: 'templates', component: () => import('@/views/Templates.vue'), meta: { title: '填单模板', roles: ['ADMIN','APPLICANT','PURCHASE_MANAGER'] } },
      { path: 'knowledge', component: () => import('@/views/Knowledge.vue'), meta: { title: '制度问答', roles: allRoles } },
      { path: 'anomaly', component: () => import('@/views/AnomalyInsight.vue'), meta: { title: '异常解读', roles: ['ADMIN','PURCHASE_MANAGER','DEPT_MANAGER'] } },

      { path: 'ops', component: () => import('@/views/Ops.vue'), meta: { title: '运行中心', roles: ['ADMIN'] } },
      { path: 'users', component: () => import('@/views/Users.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'logs', component: () => import('@/views/Logs.vue'), meta: { title: '操作日志', roles: ['ADMIN'] } },
      { path: 'ai-settings', component: () => import('@/views/AiSettings.vue'), meta: { title: '大模型配置', roles: ['ADMIN'] } },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  if (to.meta.public) return next()
  const store = useUserStore()
  if (!store.token) return next('/login')
  if (to.meta.roles && to.meta.roles.length) {
    const ok = to.meta.roles.some(r => store.roles.includes(r))
    if (!ok) return next('/dashboard')
  }
  next()
})

export default router
