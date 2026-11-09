const map = {
  DRAFT: '草稿',
  PENDING_L1: '待一级审批',
  PENDING_L2: '待二级审批',
  PENDING_L3: '待三级审批',
  PENDING_INQUIRY: '待询价',
  PENDING_ORDER: '待下单',
  PENDING_RECEIVE: '待收货',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
  PARTIAL: '部分入库',
  OPEN: '询价中',
  SELECTED: '已选定',
  ORDERED: '已下单'
}
export function statusLabel(s) { return map[s] || s }
