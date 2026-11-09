# V2 扩展 API 契约（前后端对齐）

统一前缀 `/api`，`Authorization: Bearer <token>`，响应 `{code,message,data}`，`code=0` 成功。

> 本文件供前端子代理并行对接；后端以实现本契约为准。

## 1. 预算科目 Budget
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/budgets | 分页 `page,size,year,deptName,keyword` |
| GET | /api/budgets/{id} | 详情 |
| POST | /api/budgets | 新建 `{code,name,deptName,budgetYear,totalAmount,remark}` |
| PUT | /api/budgets/{id} | 更新 |
| DELETE | /api/budgets/{id} | 删除（无占用时可删） |
| GET | /api/budgets/available | 当前用户部门可用预算列表 |

申请提交时若带 `budgetSubjectId`，校验 `usedAmount + request.budgetAmount <= totalAmount`，超预算返回 `code!=0` 且 message 含「超预算」。

## 2. 合同 Contract
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/contracts | 分页 |
| GET | /api/contracts/{id} | 详情 |
| POST | /api/contracts | `{orderId,title,amount,startDate,endDate,contentText,remark}` 由订单生成 |
| PUT | /api/contracts/{id} | 更新草稿/生效前 |
| POST | /api/contracts/{id}/activate | 生效 |
| POST | /api/contracts/{id}/risk-summary | AI 风险摘要（DeepSeek，可降级） |
| GET | /api/contracts/expiring | 即将到期（默认 30 天内） |

## 3. 多轮询价 / 议价 / 比价报告
在原有 `/api/inquiries` 上扩展：
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/inquiries/{id}/rounds | 开启下一轮议价 `{remark}` |
| GET | /api/inquiries/{id}/rounds | 轮次与各轮报价 |
| POST | /api/inquiries/{id}/quotes | body 增加 `roundNo,negotiateNote` |
| GET | /api/inquiries/{id}/compare-report | 比价汇总 JSON |
| GET | /api/inquiries/{id}/compare-report/export | Excel 导出 |

## 4. 到货差异 / 退货换货
入库 DTO 增加 `diffFlag,diffRemark`（短装/超装记录）。
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/returns | 列表 |
| GET | /api/returns/{id} | 详情 |
| POST | /api/returns | `{orderId,type:RETURN\|EXCHANGE,reason,items:[{orderItemId,quantity,remark}]}` |
| POST | /api/returns/{id}/approve | `{result:PASS\|REJECT,comment}` |
| POST | /api/returns/{id}/complete | 完成（退货扣库存 / 换货标记） |

## 5. 发票与付款
| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | /api/invoices | 列表/登记 `{orderId,invoiceNo,amount,taxAmount,invoiceDate,remark}` |
| PUT | /api/invoices/{id}/match | 对账匹配 `{status:MATCHED\|REJECTED,remark}` |
| GET/POST | /api/payments | 付款申请列表/新建 `{orderId,invoiceId,amount,remark}` |
| POST | /api/payments/{id}/submit | 提交 |
| POST | /api/payments/{id}/approve | `{result,comment}` |
| POST | /api/payments/{id}/mark-paid | 标记已付（模拟，不接真实支付） |

## 6. 供应商准入 / 黑名单 / 年审
供应商实体扩展字段；API：
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/suppliers/{id}/score | `{score,dimension,remark}` 准入/过程评分 |
| GET | /api/suppliers/{id}/scores | 评分历史 |
| POST | /api/suppliers/{id}/blacklist | `{blacklisted:true\|false,reason}` |
| POST | /api/suppliers/{id}/review | `{result:PASS\|FAIL,nextReviewDate,remark}` 年审 |
| GET | /api/suppliers/review-due | 待年审列表 |

黑名单供应商不可被询价选定。

## 7. 数据权限
角色字段 `dataScope`: `SELF` | `DEPT` | `ALL`。  
列表接口按当前用户最大 dataScope 过滤（申请人默认 SELF，主管 DEPT，采购/仓管/管理员 ALL）。  
`GET /api/roles` 返回含 dataScope；`PUT /api/roles/{id}/data-scope` 管理员可改。

## 8. 审批规则（按金额）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/approval-rules | 列表 |
| POST/PUT/DELETE | /api/approval-rules[/{id}] | 维护 `{name,minAmount,maxAmount,levels,enabled}` |
| GET | /api/approval-rules/resolve?amount= | 解析当前金额需几级 |

`levels=1|2|3`：L1 主管 / L2 采购负责人 / L3 财务(FINANCE)或 ADMIN。  
申请提交后按规则进入 `PENDING_L1`；末级通过 → `PENDING_INQUIRY`。状态增加 `PENDING_L3`。

## 9. 消息通知 + 邮件模拟
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/messages | 我的消息 `page,size,onlyUnread` |
| GET | /api/messages/unread-count | 未读数 |
| POST | /api/messages/{id}/read | 标已读 |
| POST | /api/messages/read-all | 全部已读 |
| GET | /api/email-logs | 邮件发送日志（ADMIN） |

业务事件（审批待办、通过/驳回、库存预警、合同到期）写 `sys_message`，并写 `email_send_log`（模拟发送成功）。

## 10. 导入导出 / 审计 / 限流
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/materials/import | Excel 导入 |
| GET | /api/materials/export | Excel 导出 |
| GET | /api/suppliers/export | 导出 |
| GET | /api/orders/export | 导出 |
| GET | /api/logs | 审计日志增强字段：method,requestUri,costMs,success |

全局限流：默认每用户每分钟 120 次，超限 `code=429`。

## 11. 定时任务（无独立 API，可手动触发演示）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/jobs/inventory-alert | 手动触发库存预警通知 |
| POST | /api/jobs/contract-expiry | 手动触发合同到期扫描 |

## 12. AI 扩展
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/ai/anomaly-analysis | `{months?:3}` 异常采购解读 |
| POST | /api/ai/generate-request | 原有；成功后可写入物资模板记忆 |
| GET | /api/material-templates | 我的常用物资模板 |
| POST | /api/material-templates | 保存模板 |
| POST | /api/material-templates/{id}/use | 使用（useCount+1） |
| DELETE | /api/material-templates/{id} | 删除 |

## 13. RAG 增强
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/knowledge/ask | 返回增加 `highlights:[{refIndex,keyword,snippet}]`，并落库历史 |
| GET | /api/knowledge/qa-history | 历史问答 `page,size` |
| GET | /api/knowledge/qa-history/{id} | 单条 |

## 采购申请字段扩展
`RequestSaveDTO` 增加：`budgetSubjectId`（可选）。  
详情返回占用预算信息。
