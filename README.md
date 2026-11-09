# 基于大模型的智能采购管理系统

本科毕业设计演示项目：Vue 3 + Element Plus + Spring Boot 3 + MyBatis-Plus + JWT + MySQL/H2 + DeepSeek。

## 文档
见 `docs/`：需求分析、总体架构、库表设计、审批状态机、API 清单、AI/RAG 设计、V2 扩展 API 契约。公网部署见 `docs/RENDER.md`。本地演示口令只用于本机；`PUBLIC_MODE=true` 时必须另设门禁和登录口令，页面不再展示本地账号卡片。

## 环境要求
- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8（可选；默认可用 H2 兼容模式快速演示）
- DeepSeek API Key（可选；无 key 时主流程可手工操作，AI 接口会规则降级）

## 快速启动（H2，推荐本地答辩演示）

### 1. 后端
```bash
cd backend
# 若表结构变更，先清理旧库：
rm -rf data
export SPRING_PROFILES_ACTIVE=h2
# 可选：export DEEPSEEK_API_KEY=你的key
mvn -DskipTests package
java -jar target/intelligent-procurement-1.0.0.jar
```
接口文档：http://127.0.0.1:8080/doc.html

### 2. 前端
```bash
cd frontend
npm install
npm run dev
```
访问：http://127.0.0.1:43123  
Vite 已将 `/api`、`/files` 代理到 `8080`。前端默认优先真实 API；个人中心可打开「mock 回退」。

## MySQL 启动方式
1. 建库：`CREATE DATABASE procurement DEFAULT CHARSET utf8mb4;`
2. 修改 `backend/src/main/resources/application-mysql.yml` 账号密码
3. `export SPRING_PROFILES_ACTIVE=mysql` 后启动 jar  
DDL/种子：`backend/src/main/resources/db/schema.sql`、`data.sql`

## 演示账号（密码均为 `123456`）
| 账号 | 角色 |
|---|---|
| zhangsan | 采购申请人 |
| lisi | 部门主管（一级审批） |
| wangwu | 采购负责人（二级审批 / 询价下单） |
| zhaoliu | 仓管 |
| caiwu | 财务（三级审批 / 付款对账） |
| admin | 系统管理员 |

## 12–15 分钟口播顺序

种子数据已经铺好，不必现场建单。登录页点身份卡片即进入，密码都是 `123456`。系统里菜单「演示脚本」和「答辩一页纸」与下面同序。换账号时说一句「我退出，换某某角色」，回登录页点对应卡片。

| 时间 | 账号 | 口播与页面 |
|---|---|---|
| 0:00–1:30 | admin | 登录页点「管理员」。工作台看 KPI、近六月金额和品类占比，点「一键生成管理周报」。无 Key 时说明走规则汇总。 |
| 1:30–3:00 | admin | `/defense-brief` 答辩一页纸：技术栈、页面数、6 个 AI 嵌入点、SELF/DEPT/ALL。 |
| 3:00–5:00 | admin | `/trace/1` 时间机器。用笔记本补货单讲提交、两级审批、两轮报价、下单、入库、合同、发票、付款。 |
| 5:00–6:30 | admin | `/approval-rules` 试算 68000，说明超过 5 万走财务三级，规则在库里。`/process` 点一个节点看数量。 |
| 6:30–8:30 | **换 wangwu** | `/suppliers/compare` 华信对宏达。`/suppliers/1/profile` 雷达和风险标签。`/inquiry/9` 指出最低价，可开打印预览。 |
| 8:30–10:00 | **换 lisi** | `/approvals` 看待一级审批和右侧真实时间轴。顶栏数据范围是「本部门 / DEPT」。可点审批助手。 |
| 10:00–11:30 | lisi 或任意 | `/knowledge` 问「超过1万元的采购需要几级审批？」，看引用卡片。再打开 `/anomaly`（主管可见）点卡片跳到预算或供应商。 |
| 11:30–13:00 | **换 admin** | `/budgets` 信息中心超 80%，下钻到投影申请。`/ops` 点一次库存预警，看任务结果和 Key 脱敏。铃铛抽屉里有未读。 |
| 13:00–14:30 | admin | 按 `?` 看快捷键，提一句 `Ctrl+K`。若还有时间：申请列表打印预览、`/invoices` 付款进度。收在数据范围：张三仅本人，李四本部门，其余全部。 |

备用换角：caiwu 看三级审批待办；zhaoliu 看库存色阶和待收货。不要在无网环境现场填 DeepSeek Key。

细节清单见 `docs/08-答辩演示增强.md`。

## 答辩演示步骤（主流程）
1. **zhangsan** → 采购申请 → 新建 →「AI生成明细」→ 可选绑定预算科目 → 保存并提交  
2. **lisi** → 我的审批 → 通过  
3. 金额 ≥ 5 万时 **caiwu** 可出现三级审批；常规金额 **wangwu** 二级通过后进入询价  
4. **wangwu** → 申请列表「询价」→ 录入报价 →「AI供应商推荐」→ 选定 → 生成订单；可开多轮议价 / 导出比价报告  
5. **zhaoliu** → 入库验收（可记到货差异）→ 工作台看库存预警  
6. 任意角色 → 制度问答：上传 `sample-data/采购管理制度.txt` → 提问「超过1万元的采购需要几级审批？」

## V2 扩展演示（财务 / 合约 / 智能）
| 页面 | 建议账号 | 说明 |
|---|---|---|
| 预算管理 | admin / caiwu | 种子科目已有；超预算提交会被拦截 |
| 合同管理 | wangwu | 可由订单生成合同；到期扫描 / AI 风险摘要 |
| 退货换货 | zhaoliu / wangwu | 无单时为空态，创建后走审批→完成 |
| 发票付款 | caiwu | 登记发票、对账、付款申请（模拟已付） |
| 消息中心 | 任意 | 种子消息含未读；角标与已读联动 |
| 填单模板 | zhangsan | 种子「常用笔记本」「打印纸补货」 |
| 准入年审 / 画像 | wangwu | 基于供应商主数据与评分 |
| 异常解读 | wangwu | `POST /api/ai/anomaly-analysis`，无 Key 时规则降级 |

手动触发定时任务（管理员/采购/财务）：
- `POST /api/jobs/inventory-alert`
- `POST /api/jobs/contract-expiry`

## 目录结构
```
intelligent-procurement/
  docs/           设计文档（含 07-v2扩展API契约.md）
  backend/        Spring Boot
  frontend/       Vue3
  sample-data/    示例制度文本
  README.md
```

## V2 功能清单
预算与超预算拦截、合同与到期提醒、多轮询价/比价 Excel、到货差异与退换货、发票与付款、供应商评分/黑名单/年审、数据权限 SELF/DEPT/ALL、按金额审批级数、消息中心与邮件模拟、导入导出与审计增强、接口限流、定时库存/合同扫描、AI 异常解读、RAG 高亮与问答历史、物资模板。

## 嵌入式 AI（答辩亮点）
- 智能填单助手：申请/合同/退换货编辑页抽屉，一键采纳字段（`POST /api/ai/form-assist`）
- 审批助手：审批页倾向与依据（`POST /api/ai/approval-assist`）
- 智能比价结论：询价页结论卡并写入报告（`POST /api/ai/inquiry-verdict`）
- 管理周报：工作台一键生成（`POST /api/ai/weekly-report`）
- 大模型配置：管理员菜单「大模型配置」`/ai-settings`（`GET/PUT /api/ai/config`，存 `sys_config`）
