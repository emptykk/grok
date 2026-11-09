# 模块与 API 清单

统一前缀 `/api`，除登录外需 `Authorization: Bearer <token>`。响应：`{ code, message, data }`，`code=0` 成功。

## 认证
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/login | 登录，返回 token 与用户信息 |
| GET | /api/auth/me | 当前用户 |
| PUT | /api/auth/password | 修改密码 |

## 系统管理（ADMIN）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/PUT | /api/users | 用户 CRUD |
| GET | /api/roles | 角色列表 |
| GET | /api/logs | 操作日志分页 |

## 主数据
| 方法 | 路径 | 说明 |
|---|---|---|
| CRUD | /api/materials | 物资 |
| CRUD | /api/suppliers | 供应商 |
| GET | /api/inventories | 库存列表/预警 |
| GET | /api/inventories/alerts | 低于安全库存 |

## 采购申请
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/requests | 分页（按角色过滤） |
| GET | /api/requests/{id} | 详情含明细与审批 |
| POST | /api/requests | 新建草稿 |
| PUT | /api/requests/{id} | 更新草稿 |
| POST | /api/requests/{id}/submit | 提交 |
| POST | /api/requests/{id}/withdraw | 撤回 |
| DELETE | /api/requests/{id} | 删草稿 |
| GET | /api/requests/export | Excel 导出 |

## 审批
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/approvals/todo | 我的待办 |
| POST | /api/approvals/{requestId} | body: {result, comment} |

## 询价与订单、入库
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/inquiries | 按申请创建询价 |
| POST | /api/inquiries/{id}/quotes | 录入报价 |
| POST | /api/inquiries/{id}/select | 选定供应商 |
| POST | /api/inquiries/{id}/recommend | AI 推荐 |
| POST | /api/orders | 由询价生成订单 |
| GET | /api/orders | 订单列表 |
| POST | /api/inbounds | 入库 |
| GET | /api/inbounds | 入库记录 |

## 智能
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/ai/generate-request | 自然语言→申请明细 |
| POST | /api/ai/recommend-suppliers | 供应商推荐 |
| POST | /api/knowledge/upload | 上传制度 |
| GET | /api/knowledge/docs | 文档列表 |
| POST | /api/knowledge/ask | RAG 问答 |
| DELETE | /api/knowledge/docs/{id} | 删除文档 |

## 看板与附件
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/dashboard/summary | 统计汇总 |
| POST | /api/attachments/upload | 上传 |
| GET | /api/attachments | 按业务查询 |

---

## V2 扩展
详见 **`07-v2扩展API契约.md`**（预算/合同/多轮询价/退换货/发票付款/供应商准入/数据权限/审批规则/消息/导入导出/定时任务/AI异常/RAG历史/物资模板）。
新增角色：`FINANCE`（caiwu / 123456）。


## 嵌入式 AI 与配置（本轮新增）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/ai/form-assist | 智能填单助手（scene=REQUEST/CONTRACT/RETURN） |
| POST | /api/ai/approval-assist | 审批助手（requestId） |
| POST | /api/ai/inquiry-verdict | 智能比价结论（inquiryId, persist） |
| POST | /api/ai/weekly-report | 管理周报（days） |
| GET | /api/ai/config | 大模型配置（密钥脱敏） |
| PUT | /api/ai/config | 管理员保存 BaseURL/Model/API Key |
