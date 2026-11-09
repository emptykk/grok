# AI 与 RAG 设计

## 1. 配置
```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat
```
未配置 key 时：接口返回明确错误提示，业务主流程仍可手工录入。

## 2. AI 生成采购申请
- **输入**：用户自然语言 + 可选物资目录摘要  
- **Prompt**：要求输出严格 JSON 数组：`[{name,spec,unit,quantity,estimatePrice,remark}]`  
- **后处理**：解析 JSON；非法则提示重试；前端可编辑后保存为申请明细  

## 3. 供应商智能推荐
1. **规则层**：按申请品类/名称关键词匹配供应商 `category`；过滤停用；按 `rating`、历史成交均价排序，取 Top N  
2. **模型层**：将候选供应商与申请明细、历史价写入 Prompt，要求输出推荐排序与理由（JSON）  
3. **落库**：推荐结果写入 `inquiry.ai_recommend_json` 便于答辩展示  

## 4. RAG 制度问答（轻量方案）
1. **解析**：PDF（PDFBox）/ Word（POI）提取纯文本  
2. **分段**：按段落/固定长度（约 500 字，重叠 50 字）写入 `knowledge_chunk`  
3. **检索**：无外部向量库；采用 **中文分词粗粒度 + TF 余弦 / 关键词重合打分** 取 Top-K 片段  
4. **生成**：将 Top-K 作为 context 交给 DeepSeek，要求回答并引用片段编号  
5. **响应**：`answer` + `references[{docTitle, chunkIndex, snippet}]`  

说明：该方案满足本科毕设对 RAG 架构的阐述与演示，避免引入 Milvus 等重依赖。若后续有 embedding API，可替换检索层而不改接口。

## 5. 降级策略
- HTTP 超时/401/余额不足：返回 `AI_UNAVAILABLE`  
- RAG 无命中片段：提示「未在制度中检索到相关内容」，不编造  

## V2 增强
- **异常采购解读** `POST /api/ai/anomaly-analysis`：聚合近 N 月申请/订单/供应商金额，DeepSeek 解读；无 Key 时规则降级。
- **合同风险摘要** `POST /api/contracts/{id}/risk-summary`。
- **RAG**：ask 返回 `highlights`（引用片段关键词高亮线索），并写入 `knowledge_qa` 历史。
- **物资模板记忆**：AI 生成明细后写入/累加 `material_template`。


## 6. 嵌入式智能（Embedded AI，非对话框）

系统将大模型能力嵌在业务节点，而不是仅提供聊天页：

| 能力 | 中文名 | API | 页面入口 |
|---|---|---|---|
| 表单内联建议 | 智能填单助手 | `POST /api/ai/form-assist` | 采购申请编辑、合同新建、退换货新建（抽屉） |
| 审批倾向 | 审批助手 | `POST /api/ai/approval-assist` | 我的审批右侧面板 |
| 比价结论 | 智能比价结论 | `POST /api/ai/inquiry-verdict` | 询价比价页结论卡，可写入询价单 |
| 管理周报 | 一键生成管理周报 | `POST /api/ai/weekly-report` | 工作台右上角 |

未配置 Key 时均有**规则降级**，保证答辩可演示。

## 7. 大模型运行时配置

- 管理员页面：`/ai-settings`（大模型配置）
- API：`GET/PUT /api/ai/config`（密钥脱敏返回；PUT 时含 `*` 或空则不覆盖原 Key）
- 存储：`sys_config` 表（`deepseek.api-key` / `deepseek.base-url` / `deepseek.model`），优先于 `application.yml` / 环境变量
