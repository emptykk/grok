package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.procurement.ai.DeepSeekClient;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {
    private final DeepSeekClient deepSeekClient;
    private final MaterialMapper materialMapper;
    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestItemMapper requestItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final SupplierMapper supplierMapper;
    private final BudgetSubjectMapper budgetSubjectMapper;
    private final InquiryMapper inquiryMapper;
    private final InquiryQuoteMapper quoteMapper;
    private final InventoryMapper inventoryMapper;
    private final ContractInfoMapper contractInfoMapper;
    private final MaterialTemplateService templateService;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> generateRequestItems(String prompt) {
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<>());
        String catalog = materials.stream()
                .map(m -> m.getCode() + "|" + m.getName() + "|" + m.getSpec() + "|" + m.getUnit() + "|" + m.getCategory())
                .collect(Collectors.joining("\n"));
        String system = "你是企业采购助理。根据用户需求生成采购明细。优先匹配物资目录中的名称。"
                + "只输出 JSON 数组，不要 markdown。字段：materialName,spec,unit,quantity,estimatePrice,remark。"
                + "quantity/estimatePrice 为数字。";
        String user = "物资目录:\n" + catalog + "\n\n用户需求:\n" + prompt;
        String content;
        try {
            content = deepSeekClient.chat(system, user);
        } catch (BizException e) {
            List<Map<String, Object>> fallback = new ArrayList<>();
            for (Material m : materials) {
                if (prompt != null && m.getName() != null && prompt.contains(m.getName().substring(0, Math.min(2, m.getName().length())))) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialId", m.getId());
                    row.put("materialName", m.getName());
                    row.put("spec", m.getSpec());
                    row.put("unit", m.getUnit());
                    row.put("quantity", 1);
                    row.put("estimatePrice", 0);
                    row.put("remark", "规则降级生成（未配置大模型）");
                    fallback.add(row);
                }
            }
            if (fallback.isEmpty()) throw e;
            remember(fallback);
            return fallback;
        }
        List<Map<String, Object>> items = parseItems(content);
        remember(items);
        return items;
    }

    private void remember(List<Map<String, Object>> items) {
        try {
            List<MaterialTemplateService.MapLike> list = new ArrayList<>();
            for (Map<String, Object> it : items) {
                MaterialTemplateService.MapLike ml = new MaterialTemplateService.MapLike();
                Object mid = it.get("materialId");
                if (mid instanceof Number) ml.materialId = ((Number) mid).longValue();
                ml.materialName = String.valueOf(it.getOrDefault("materialName", ""));
                ml.spec = it.get("spec") == null ? null : String.valueOf(it.get("spec"));
                ml.unit = it.get("unit") == null ? null : String.valueOf(it.get("unit"));
                Object q = it.get("quantity");
                Object p = it.get("estimatePrice");
                if (q != null) ml.quantity = new BigDecimal(q.toString());
                if (p != null) ml.estimatePrice = new BigDecimal(p.toString());
                list.add(ml);
            }
            templateService.rememberFromItems(SecurityUtils.currentUser().getId(), list);
        } catch (Exception ignored) {}
    }

    public Map<String, Object> anomalyAnalysis(Integer months) {
        int m = months == null || months <= 0 ? 3 : months;
        LocalDateTime since = LocalDateTime.now().minusMonths(m);
        List<PurchaseRequest> reqs = requestMapper.selectList(new LambdaQueryWrapper<PurchaseRequest>().ge(PurchaseRequest::getCreatedAt, since));
        List<PurchaseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>().ge(PurchaseOrder::getCreatedAt, since));
        List<Supplier> suppliers = supplierMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal total = orders.stream().map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, BigDecimal> bySupplier = new HashMap<>();
        for (PurchaseOrder o : orders) {
            bySupplier.merge(o.getSupplierId(), o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount(), BigDecimal::add);
        }
        String stats = "近" + m + "个月申请数=" + reqs.size() + ", 订单数=" + orders.size() + ", 订单总额=" + total
                + ", 供应商金额分布=" + bySupplier + ", 供应商数=" + suppliers.size();
        List<Map<String, Object>> findings = buildAnomalyFindings(m, orders, bySupplier, suppliers, total);
        String source = "llm";
        String analysis;
        try {
            analysis = deepSeekClient.chat(
                    "你是采购风控分析师。根据统计数据指出异常点：金额突增、供应商集中度过高、品类异常等，给出中文解读与建议，300字内。",
                    stats + "\n规则发现=" + findings);
        } catch (BizException e) {
            source = "rule";
            analysis = "（大模型未配置，已按规则降级）" + findings.stream()
                    .map(f -> String.valueOf(f.get("title")) + "：" + f.get("advice"))
                    .collect(Collectors.joining("；"))
                    + "。原始统计：" + stats;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", m);
        result.put("stats", stats);
        result.put("analysis", analysis);
        result.put("source", source);
        result.put("orderTotal", total);
        result.put("requestCount", reqs.size());
        result.put("orderCount", orders.size());
        result.put("supplierAmountMap", bySupplier);
        result.put("findings", findings);
        return result;
    }

    private List<Map<String, Object>> buildAnomalyFindings(int months, List<PurchaseOrder> orders,
                                                           Map<Long, BigDecimal> bySupplier, List<Supplier> suppliers, BigDecimal total) {
        List<Map<String, Object>> findings = new ArrayList<>();
        if (bySupplier.size() >= 1 && total.signum() > 0) {
            Map.Entry<Long, BigDecimal> top = bySupplier.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
            if (top != null) {
                int pct = top.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
                Supplier sp = suppliers.stream().filter(s -> s.getId().equals(top.getKey())).findFirst().orElse(null);
                String name = sp == null ? ("供应商#" + top.getKey()) : sp.getName();
                findings.add(finding("concentration", "供应商集中度", pct >= 50 ? "高" : "中", pct + "%",
                        name + " 近 " + months + " 个月成交占订单总额 " + pct + "%。",
                        pct >= 50 ? "建议引入备选供应商并保留比价记录，避免单一来源。" : "集中度尚可，持续观察头部供应商交期与质量。"));
            }
        }
        for (Map.Entry<Long, BigDecimal> e : bySupplier.entrySet()) {
            Supplier sp = suppliers.stream().filter(s -> s.getId().equals(e.getKey())).findFirst().orElse(null);
            if (sp != null && sp.getBlacklisted() != null && sp.getBlacklisted() == 1 && e.getValue().signum() > 0) {
                findings.add(finding("blacklist", "黑名单供应商仍有成交", "高", "¥" + e.getValue(),
                        sp.getName() + " 已拉黑，窗口内仍有订单金额。", "立即冻结后续下单，并复核历史审批是否绕过准入。"));
            }
        }
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        for (PurchaseOrder o : orders) {
            if (o.getCreatedAt() == null) continue;
            String key = o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            byMonth.merge(key, o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount(), BigDecimal::add);
        }
        List<BigDecimal> seq = new ArrayList<>(byMonth.values());
        if (seq.size() >= 2) {
            BigDecimal prev = seq.get(seq.size() - 2);
            BigDecimal last = seq.get(seq.size() - 1);
            if (prev.signum() > 0 && last.compareTo(prev.multiply(new BigDecimal("1.8"))) > 0) {
                findings.add(finding("spike", "近期金额突增", "高", "¥" + last,
                        "最近一个有订单的月份金额约为上一月的 " + last.divide(prev, 1, RoundingMode.HALF_UP) + " 倍。",
                        "核对是否集中补货或拆单规避，必要时升级审批。"));
            }
        }
        for (BudgetSubject b : budgetSubjectMapper.selectList(new LambdaQueryWrapper<BudgetSubject>().eq(BudgetSubject::getStatus, 1))) {
            if (b.getTotalAmount() == null || b.getTotalAmount().signum() == 0 || b.getUsedAmount() == null) continue;
            BigDecimal ratio = b.getUsedAmount().divide(b.getTotalAmount(), 3, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.8")) >= 0) {
                findings.add(finding("budget-" + b.getId(), "预算占用偏高", ratio.compareTo(new BigDecimal("0.95")) >= 0 ? "高" : "中",
                        ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "%",
                        b.getDeptName() + "「" + b.getName() + "」已用 " + b.getUsedAmount() + " / " + b.getTotalAmount() + "。",
                        "新申请提交前确认剩余额度，超预算会被拦截。"));
            }
        }
        if (findings.isEmpty()) {
            findings.add(finding("calm", "窗口内未见显著异常", "低", orders.size() + " 单",
                    "近 " + months + " 个月订单 " + orders.size() + " 笔，总额 ¥" + total + "。",
                    "保持比价与入库抽检即可。"));
        }
        return findings;
    }

    private Map<String, Object> finding(String id, String title, String level, String metric, String desc, String advice) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", id);
        f.put("title", title);
        f.put("level", level);
        f.put("metric", metric);
        f.put("desc", desc);
        f.put("advice", advice);
        return f;
    }

    /** 智能填单助手：根据场景与当前表单上下文给出可一键采纳的字段建议 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> formAssist(Map<String, Object> body) {
        String scene = str(body.get("scene"), "REQUEST").toUpperCase(Locale.ROOT);
        Map<String, Object> form = body.get("form") instanceof Map ? (Map<String, Object>) body.get("form") : new LinkedHashMap<>();
        String hint = str(body.get("hint"), "");
        List<Map<String, Object>> suggestions = new ArrayList<>();
        List<Map<String, Object>> itemSuggestions = new ArrayList<>();
        String advice;
        String source = "rule";

        if ("REQUEST".equals(scene)) {
            Map<String, Object> rule = ruleFormRequest(form, hint);
            suggestions = (List<Map<String, Object>>) rule.get("suggestions");
            itemSuggestions = (List<Map<String, Object>>) rule.get("itemSuggestions");
            advice = String.valueOf(rule.get("advice"));
            try {
                String system = "你是企业采购填单助手。根据当前表单与用户提示，给出可直接写入表单的建议。"
                        + "只输出 JSON：{\"advice\":\"...\",\"suggestions\":[{\"field\":\"title|budgetAmount|remark\",\"label\":\"\",\"value\":\"\",\"reason\":\"\"}],"
                        + "\"itemSuggestions\":[{\"materialName\":\"\",\"spec\":\"\",\"unit\":\"\",\"quantity\":1,\"estimatePrice\":0,\"remark\":\"\"}]}";
                String user = "当前表单:" + objectMapper.writeValueAsString(form) + "\n用户提示:" + hint
                        + "\n物资目录摘要:" + materialMapper.selectList(new LambdaQueryWrapper<Material>().last("LIMIT 30")).stream()
                        .map(m -> m.getName() + "/" + m.getSpec() + "/" + m.getUnit()).collect(Collectors.joining("; "));
                String content = deepSeekClient.chat(system, user);
                Map<String, Object> parsed = parseObject(content);
                if (parsed.get("advice") != null) advice = String.valueOf(parsed.get("advice"));
                if (parsed.get("suggestions") instanceof List) suggestions = (List<Map<String, Object>>) parsed.get("suggestions");
                if (parsed.get("itemSuggestions") instanceof List) itemSuggestions = (List<Map<String, Object>>) parsed.get("itemSuggestions");
                source = "llm";
            } catch (Exception ignored) { /* keep rule */ }
        } else if ("CONTRACT".equals(scene)) {
            Map<String, Object> rule = ruleFormContract(form, hint);
            suggestions = (List<Map<String, Object>>) rule.get("suggestions");
            advice = String.valueOf(rule.get("advice"));
            try {
                String system = "你是采购合同起草助手。根据表单给出字段建议。只输出 JSON："
                        + "{\"advice\":\"...\",\"suggestions\":[{\"field\":\"title|amount|remark|contentText\",\"label\":\"\",\"value\":\"\",\"reason\":\"\"}]}";
                String content = deepSeekClient.chat(system, "表单:" + objectMapper.writeValueAsString(form) + "\n提示:" + hint);
                Map<String, Object> parsed = parseObject(content);
                if (parsed.get("advice") != null) advice = String.valueOf(parsed.get("advice"));
                if (parsed.get("suggestions") instanceof List) suggestions = (List<Map<String, Object>>) parsed.get("suggestions");
                source = "llm";
            } catch (Exception ignored) {}
        } else if ("RETURN".equals(scene)) {
            Map<String, Object> rule = ruleFormReturn(form, hint);
            suggestions = (List<Map<String, Object>>) rule.get("suggestions");
            advice = String.valueOf(rule.get("advice"));
            try {
                String system = "你是退换货填单助手。根据差异描述生成规范原因与类型建议。只输出 JSON："
                        + "{\"advice\":\"...\",\"suggestions\":[{\"field\":\"type|reason\",\"label\":\"\",\"value\":\"\",\"reason\":\"\"}]}。"
                        + "type 只能是 RETURN 或 EXCHANGE。";
                String content = deepSeekClient.chat(system, "表单:" + objectMapper.writeValueAsString(form) + "\n提示:" + hint);
                Map<String, Object> parsed = parseObject(content);
                if (parsed.get("advice") != null) advice = String.valueOf(parsed.get("advice"));
                if (parsed.get("suggestions") instanceof List) suggestions = (List<Map<String, Object>>) parsed.get("suggestions");
                source = "llm";
            } catch (Exception ignored) {}
        } else {
            throw new BizException("未知 scene，支持 REQUEST/CONTRACT/RETURN");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene", scene);
        result.put("source", source);
        result.put("advice", advice);
        result.put("suggestions", suggestions);
        result.put("itemSuggestions", itemSuggestions);
        return result;
    }

    private Map<String, Object> ruleFormRequest(Map<String, Object> form, String hint) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        List<Map<String, Object>> items = new ArrayList<>();
        String title = str(form.get("title"), "");
        if (title.isBlank() && !hint.isBlank()) {
            suggestions.add(sug("title", "标题", hint.length() > 40 ? hint.substring(0, 40) : hint, "根据需求描述生成标题"));
        }
        Object budget = form.get("budgetAmount");
        if (budget == null || toBd(budget).compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal est = BigDecimal.ZERO;
            if (form.get("items") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        est = est.add(toBd(m.get("quantity")).multiply(toBd(m.get("estimatePrice"))));
                    }
                }
            }
            if (est.compareTo(BigDecimal.ZERO) > 0) {
                suggestions.add(sug("budgetAmount", "预算金额", est.setScale(2, RoundingMode.HALF_UP).toPlainString(), "按明细估价汇总"));
            } else if (!hint.isBlank()) {
                Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*万").matcher(hint);
                if (m.find()) {
                    suggestions.add(sug("budgetAmount", "预算金额",
                            new BigDecimal(m.group(1)).multiply(BigDecimal.valueOf(10000)).toPlainString(), "从描述中识别预算"));
                }
            }
        }
        if (str(form.get("remark"), "").isBlank() && !hint.isBlank()) {
            suggestions.add(sug("remark", "备注", "需求说明：" + hint, "补充申请备注便于审批"));
        }
        // 明细：匹配物资名
        if (!(form.get("items") instanceof List) || ((List<?>) form.get("items")).isEmpty()) {
            List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<>());
            String text = hint + " " + title;
            for (Material mat : materials) {
                if (mat.getName() != null && text.contains(mat.getName().substring(0, Math.min(2, mat.getName().length())))) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialName", mat.getName());
                    row.put("spec", mat.getSpec());
                    row.put("unit", mat.getUnit());
                    row.put("quantity", 1);
                    row.put("estimatePrice", 0);
                    row.put("remark", "规则匹配物资目录");
                    items.add(row);
                }
            }
            if (items.isEmpty() && !hint.isBlank()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("materialName", hint.length() > 20 ? hint.substring(0, 20) : hint);
                row.put("spec", "");
                row.put("unit", "台");
                row.put("quantity", 1);
                row.put("estimatePrice", 0);
                row.put("remark", "请完善规格与估价");
                items.add(row);
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("advice", "已根据表单上下文给出规则建议（未配置大模型时可用）。可逐项一键采纳。");
        r.put("suggestions", suggestions);
        r.put("itemSuggestions", items);
        return r;
    }

    private Map<String, Object> ruleFormContract(Map<String, Object> form, String hint) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        if (str(form.get("title"), "").isBlank()) {
            suggestions.add(sug("title", "合同标题",
                    "采购合同-" + LocalDate.now(), "默认合同标题"));
        }
        if (form.get("amount") == null || toBd(form.get("amount")).compareTo(BigDecimal.ZERO) <= 0) {
            suggestions.add(sug("amount", "合同金额", "0", "请按订单金额填写"));
        }
        if (str(form.get("contentText"), "").isBlank()) {
            String draft = "甲方（采购方）与乙方（供应商）就采购事项达成协议。\n"
                    + "一、标的与数量以订单为准。\n二、交期与验收按双方约定。\n三、付款方式：验收合格后支付。\n"
                    + (hint.isBlank() ? "" : "四、补充说明：" + hint + "\n");
            suggestions.add(sug("contentText", "合同正文", draft, "生成标准合同草稿骨架"));
        }
        if (str(form.get("remark"), "").isBlank() && !hint.isBlank()) {
            suggestions.add(sug("remark", "备注", hint, "写入备注"));
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("advice", "已生成合同字段建议，请核对供应商、金额与有效期后保存。");
        r.put("suggestions", suggestions);
        r.put("itemSuggestions", List.of());
        return r;
    }

    private Map<String, Object> ruleFormReturn(Map<String, Object> form, String hint) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        String text = (hint + " " + str(form.get("reason"), "")).toLowerCase(Locale.ROOT);
        String type = str(form.get("type"), "");
        if (type.isBlank()) {
            boolean exchange = text.contains("换") || text.contains("exchange");
            suggestions.add(sug("type", "类型", exchange ? "EXCHANGE" : "RETURN",
                    exchange ? "描述偏向换货" : "默认按退货处理"));
        }
        if (str(form.get("reason"), "").isBlank()) {
            String reason = !hint.isBlank() ? hint : "到货数量/质量与订单不符，申请退换货处理。";
            suggestions.add(sug("reason", "原因说明", reason, "规范退换货原因"));
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("advice", "已根据差异描述给出退换货填单建议。");
        r.put("suggestions", suggestions);
        r.put("itemSuggestions", List.of());
        return r;
    }

    private Map<String, Object> sug(String field, String label, Object value, String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("label", label);
        m.put("value", value);
        m.put("reason", reason);
        return m;
    }

    /** 审批助手：同意/驳回倾向 + 依据 + 建议意见 */
    public Map<String, Object> approvalAssist(Long requestId) {
        if (requestId == null) throw new BizException("requestId 必填");
        PurchaseRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("申请不存在");
        List<PurchaseRequestItem> items = requestItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, requestId));

        List<String> reasons = new ArrayList<>();
        Map<String, Object> checks = new LinkedHashMap<>();

        // 预算检查
        Map<String, Object> budgetCheck = new LinkedHashMap<>();
        BigDecimal budgetAmt = req.getBudgetAmount() == null ? BigDecimal.ZERO : req.getBudgetAmount();
        BudgetSubject subject = null;
        if (req.getBudgetSubjectId() != null) {
            subject = budgetSubjectMapper.selectById(req.getBudgetSubjectId());
        }
        if (subject == null && req.getDeptName() != null) {
            subject = budgetSubjectMapper.selectOne(new LambdaQueryWrapper<BudgetSubject>()
                    .eq(BudgetSubject::getDeptName, req.getDeptName())
                    .eq(BudgetSubject::getStatus, 1)
                    .orderByDesc(BudgetSubject::getBudgetYear)
                    .last("LIMIT 1"));
        }
        if (subject != null) {
            BigDecimal total = nz(subject.getTotalAmount());
            BigDecimal used = nz(subject.getUsedAmount());
            BigDecimal remain = total.subtract(used);
            budgetCheck.put("subject", subject.getName());
            budgetCheck.put("total", total);
            budgetCheck.put("used", used);
            budgetCheck.put("remain", remain);
            budgetCheck.put("requestBudget", budgetAmt);
            boolean ok = budgetAmt.compareTo(remain) <= 0;
            budgetCheck.put("ok", ok);
            reasons.add(ok
                    ? "预算科目「" + subject.getName() + "」剩余 " + remain + "，本次 " + budgetAmt + "，预算充足"
                    : "预算科目「" + subject.getName() + "」剩余 " + remain + "，本次申请 " + budgetAmt + "，存在超预算风险");
        } else {
            budgetCheck.put("ok", true);
            budgetCheck.put("notice", "未关联预算科目，跳过占用校验");
            reasons.add("未绑定预算科目，建议审批时人工确认部门预算");
        }
        checks.put("budget", budgetCheck);

        // 历史价
        Map<String, Object> priceCheck = new LinkedHashMap<>();
        List<Map<String, Object>> priceRows = new ArrayList<>();
        boolean priceOk = true;
        for (PurchaseRequestItem it : items) {
            List<PurchaseOrder> recent = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                    .orderByDesc(PurchaseOrder::getCreatedAt).last("LIMIT 50"));
            // 简化：用同名物资的估价与目录对照
            Material mat = it.getMaterialId() != null ? materialMapper.selectById(it.getMaterialId())
                    : materialMapper.selectOne(new LambdaQueryWrapper<Material>().eq(Material::getName, it.getMaterialName()).last("LIMIT 1"));
            BigDecimal est = nz(it.getEstimatePrice());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialName", it.getMaterialName());
            row.put("estimatePrice", est);
            row.put("quantity", it.getQuantity());
            if (mat != null) {
                row.put("catalogSpec", mat.getSpec());
            }
            if (est.compareTo(BigDecimal.ZERO) <= 0) {
                row.put("flag", "估价缺失");
                priceOk = false;
            } else if (est.compareTo(BigDecimal.valueOf(50000)) > 0) {
                row.put("flag", "单价偏高需复核");
                priceOk = false;
            } else {
                row.put("flag", "正常");
            }
            row.put("recentOrderCount", recent.size());
            priceRows.add(row);
        }
        priceCheck.put("ok", priceOk);
        priceCheck.put("rows", priceRows);
        reasons.add(priceOk ? "明细估价未见明显异常" : "部分明细估价缺失或偏高，建议驳回补充或要求说明");
        checks.put("historyPrice", priceCheck);

        // 供应商风险（申请阶段看黑名单供应商占比倾向：用品类相关供应商）
        Map<String, Object> supplierCheck = new LinkedHashMap<>();
        long black = supplierMapper.selectCount(new LambdaQueryWrapper<Supplier>().eq(Supplier::getBlacklisted, 1));
        long active = supplierMapper.selectCount(new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 1).eq(Supplier::getBlacklisted, 0));
        supplierCheck.put("blacklistedCount", black);
        supplierCheck.put("activeCount", active);
        supplierCheck.put("ok", black == 0 || active > 0);
        reasons.add(black > 0
                ? "系统存在 " + black + " 家黑名单供应商，后续询价请注意过滤"
                : "供应商主数据未见黑名单风险");
        checks.put("supplierRisk", supplierCheck);

        String tendency = "PASS";
        if (Boolean.FALSE.equals(budgetCheck.get("ok"))) tendency = "REJECT";
        else if (!priceOk) tendency = "REVIEW";

        String suggestedComment;
        if ("PASS".equals(tendency)) {
            suggestedComment = "经核对预算与明细估价，未见明显风险，同意进入下一环节。";
        } else if ("REJECT".equals(tendency)) {
            suggestedComment = "存在超预算风险，请调整金额或更换预算科目后重新提交。";
        } else {
            suggestedComment = "建议补充高单价物资的市场询价依据后再审。";
        }

        String source = "rule";
        try {
            String system = "你是采购审批助手。根据检查结果给出倾向 PASS/REJECT/REVIEW、中文建议意见与简要理由。"
                    + "只输出 JSON：{\"tendency\":\"PASS|REJECT|REVIEW\",\"suggestedComment\":\"...\",\"extraReasons\":[\"...\"]}";
            String user = "申请:" + req.getTitle() + " 预算:" + budgetAmt + " 部门:" + req.getDeptName()
                    + "\n检查:" + objectMapper.writeValueAsString(checks) + "\n已有理由:" + reasons;
            String content = deepSeekClient.chat(system, user);
            Map<String, Object> parsed = parseObject(content);
            if (parsed.get("tendency") != null) tendency = String.valueOf(parsed.get("tendency")).toUpperCase(Locale.ROOT);
            if (parsed.get("suggestedComment") != null) suggestedComment = String.valueOf(parsed.get("suggestedComment"));
            if (parsed.get("extraReasons") instanceof List<?> er) {
                for (Object o : er) reasons.add(String.valueOf(o));
            }
            source = "llm";
        } catch (Exception ignored) {}

        double confidence = "PASS".equals(tendency) ? 0.78 : ("REJECT".equals(tendency) ? 0.82 : 0.65);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", requestId);
        result.put("title", req.getTitle());
        result.put("tendency", tendency);
        result.put("confidence", confidence);
        result.put("reasons", reasons);
        result.put("suggestedComment", suggestedComment);
        result.put("checks", checks);
        result.put("source", source);
        return result;
    }

    /** 比价智能结论 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> inquiryVerdict(Long inquiryId, boolean persist) {
        if (inquiryId == null) throw new BizException("inquiryId 必填");
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null) throw new BizException("询价单不存在");
        List<InquiryQuote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<InquiryQuote>().eq(InquiryQuote::getInquiryId, inquiryId));
        if (quotes.isEmpty()) throw new BizException("尚无报价，无法生成结论");

        InquiryQuote lowest = quotes.stream()
                .filter(q -> q.getTotalPrice() != null)
                .min(Comparator.comparing(InquiryQuote::getTotalPrice))
                .orElse(quotes.get(0));

        // 综合分：价格 50% + 交期 30% + 评级 20%（归一化）
        BigDecimal maxPrice = quotes.stream().map(q -> nz(q.getTotalPrice())).max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        int maxDays = quotes.stream().map(q -> q.getDeliveryDays() == null ? 30 : q.getDeliveryDays()).max(Integer::compareTo).orElse(30);
        List<Map<String, Object>> scored = new ArrayList<>();
        for (InquiryQuote q : quotes) {
            Supplier s = supplierMapper.selectById(q.getSupplierId());
            double priceScore = maxPrice.compareTo(BigDecimal.ZERO) == 0 ? 1 :
                    1.0 - nz(q.getTotalPrice()).doubleValue() / Math.max(maxPrice.doubleValue(), 1);
            int days = q.getDeliveryDays() == null ? maxDays : q.getDeliveryDays();
            double dayScore = 1.0 - (double) days / Math.max(maxDays, 1);
            int rating = s == null || s.getRating() == null ? 3 : s.getRating();
            double ratingScore = rating / 5.0;
            double composite = priceScore * 0.5 + dayScore * 0.3 + ratingScore * 0.2;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("supplierId", q.getSupplierId());
            row.put("supplierName", s == null ? String.valueOf(q.getSupplierId()) : s.getName());
            row.put("totalPrice", q.getTotalPrice());
            row.put("deliveryDays", q.getDeliveryDays());
            row.put("rating", rating);
            row.put("blacklisted", s != null && s.getBlacklisted() != null && s.getBlacklisted() == 1);
            row.put("compositeScore", BigDecimal.valueOf(composite).setScale(4, RoundingMode.HALF_UP));
            scored.add(row);
        }
        scored.sort((a, b) -> ((BigDecimal) b.get("compositeScore")).compareTo((BigDecimal) a.get("compositeScore")));
        Map<String, Object> compositeBest = scored.get(0);

        List<String> risks = new ArrayList<>();
        for (Map<String, Object> row : scored) {
            if (Boolean.TRUE.equals(row.get("blacklisted"))) {
                risks.add(row.get("supplierName") + " 位于黑名单，禁止选用");
            }
        }
        if (scored.size() < 2) risks.add("报价家数不足 2 家，比价说服力弱");
        BigDecimal low = nz(lowest.getTotalPrice());
        BigDecimal high = quotes.stream().map(q -> nz(q.getTotalPrice())).max(BigDecimal::compareTo).orElse(low);
        if (high.subtract(low).compareTo(low.multiply(BigDecimal.valueOf(0.3))) > 0) {
            risks.add("最高价与最低价相差超过 30%，请核实规格是否一致");
        }

        Supplier lowSup = supplierMapper.selectById(lowest.getSupplierId());
        Map<String, Object> lowestMap = new LinkedHashMap<>();
        lowestMap.put("supplierId", lowest.getSupplierId());
        lowestMap.put("supplierName", lowSup == null ? String.valueOf(lowest.getSupplierId()) : lowSup.getName());
        lowestMap.put("totalPrice", lowest.getTotalPrice());
        lowestMap.put("deliveryDays", lowest.getDeliveryDays());

        String reportText = "【比价结论】\n"
                + "最低价：" + lowestMap.get("supplierName") + " ¥" + lowestMap.get("totalPrice") + "\n"
                + "综合最优：" + compositeBest.get("supplierName") + "（综合分 " + compositeBest.get("compositeScore") + "）\n"
                + "风险提示：" + (risks.isEmpty() ? "无明显风险" : String.join("；", risks)) + "\n"
                + "建议：优先选择综合最优供应商，并保留比价记录备查。";

        String source = "rule";
        try {
            String system = "你是采购比价分析师。根据报价与评分输出中文比价报告段落（200字内），并给出 recommendedSupplierId。"
                    + "只输出 JSON：{\"reportText\":\"...\",\"recommendedSupplierId\":1,\"risks\":[\"...\"]}";
            String content = deepSeekClient.chat(system, "报价评分:" + objectMapper.writeValueAsString(scored));
            Map<String, Object> parsed = parseObject(content);
            if (parsed.get("reportText") != null) reportText = String.valueOf(parsed.get("reportText"));
            if (parsed.get("risks") instanceof List<?> lr) {
                risks = lr.stream().map(String::valueOf).collect(Collectors.toList());
            }
            source = "llm";
        } catch (Exception ignored) {}

        Map<String, Object> verdict = new LinkedHashMap<>();
        verdict.put("inquiryId", inquiryId);
        verdict.put("lowestPrice", lowestMap);
        verdict.put("compositeBest", compositeBest);
        verdict.put("scores", scored);
        verdict.put("risks", risks);
        verdict.put("reportText", reportText);
        verdict.put("recommendedSupplierId", compositeBest.get("supplierId"));
        verdict.put("source", source);
        verdict.put("generatedAt", LocalDateTime.now().toString());

        if (persist) {
            try {
                Map<String, Object> store = new LinkedHashMap<>();
                if (inq.getAiRecommendJson() != null && !inq.getAiRecommendJson().isBlank()) {
                    try {
                        store = objectMapper.readValue(inq.getAiRecommendJson(), new TypeReference<>() {});
                    } catch (Exception ignored) {
                        store.put("previous", inq.getAiRecommendJson());
                    }
                }
                store.put("verdict", verdict);
                inq.setAiRecommendJson(objectMapper.writeValueAsString(store));
                inq.setUpdatedAt(LocalDateTime.now());
                inquiryMapper.updateById(inq);
                verdict.put("persisted", true);
            } catch (Exception e) {
                verdict.put("persisted", false);
                verdict.put("persistError", e.getMessage());
            }
        }
        return verdict;
    }

    /** 管理周报 */
    public Map<String, Object> weeklyReport(Integer days) {
        int d = days == null || days <= 0 ? 7 : days;
        LocalDateTime since = LocalDateTime.now().minusDays(d);
        Map<String, Object> dash = dashboardService.summary();
        List<PurchaseRequest> reqs = requestMapper.selectList(new LambdaQueryWrapper<PurchaseRequest>().ge(PurchaseRequest::getCreatedAt, since));
        List<PurchaseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>().ge(PurchaseOrder::getCreatedAt, since));
        BigDecimal orderAmt = orders.stream().map(o -> nz(o.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long alerts = 0;
        Object alertObj = dash.get("inventoryAlerts");
        if (alertObj instanceof List<?> list) alerts = list.size();
        List<ContractInfo> expiring = contractInfoMapper.selectList(new LambdaQueryWrapper<ContractInfo>()
                .isNotNull(ContractInfo::getEndDate)
                .le(ContractInfo::getEndDate, LocalDate.now().plusDays(30))
                .ge(ContractInfo::getEndDate, LocalDate.now()));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("days", d);
        metrics.put("newRequests", reqs.size());
        metrics.put("newOrders", orders.size());
        metrics.put("orderAmount", orderAmt);
        metrics.put("pendingL1", dash.get("pendingL1"));
        metrics.put("pendingL2", dash.get("pendingL2"));
        metrics.put("pendingInquiry", dash.get("pendingInquiry"));
        metrics.put("pendingReceive", dash.get("pendingReceive"));
        metrics.put("inventoryAlerts", alerts);
        metrics.put("contractsExpiring30d", expiring.size());
        metrics.put("supplierTop", dash.get("supplierTop"));

        String report = "【采购管理周报】" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "\n"
                + "统计窗口：近 " + d + " 天\n"
                + "1. 新增申请 " + reqs.size() + " 单，新增订单 " + orders.size() + " 单，订单金额合计 ¥" + orderAmt + "。\n"
                + "2. 待办：一级审批 " + dash.get("pendingL1") + "，二级审批 " + dash.get("pendingL2")
                + "，待询价 " + dash.get("pendingInquiry") + "，待收货 " + dash.get("pendingReceive") + "。\n"
                + "3. 库存预警 " + alerts + " 项；30 日内到期合同 " + expiring.size() + " 份。\n"
                + "4. 建议：优先清理超期审批与库存预警，对到期合同启动续签评估。";

        String source = "rule";
        try {
            String system = "你是企业管理层助理。根据采购指标写一份简洁中文周报（含亮点、风险、下周建议），400字内。直接输出正文，不要 JSON。";
            report = deepSeekClient.chat(system, "指标:" + objectMapper.writeValueAsString(metrics));
            source = "llm";
        } catch (Exception ignored) {}

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metrics", metrics);
        result.put("reportText", report);
        result.put("source", source);
        result.put("generatedAt", LocalDateTime.now().toString());
        return result;
    }

    private List<Map<String, Object>> parseItems(String content) {
        String json = content.trim();
        Matcher m = Pattern.compile("\\[.*\\]", Pattern.DOTALL).matcher(json);
        if (m.find()) json = m.group();
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
            for (Map<String, Object> row : list) {
                normalizeNumber(row, "quantity");
                normalizeNumber(row, "estimatePrice");
            }
            return list;
        } catch (Exception e) {
            throw new BizException("AI 返回无法解析为明细 JSON: " + content);
        }
    }

    private Map<String, Object> parseObject(String content) throws Exception {
        String json = content.trim();
        Matcher m = Pattern.compile("\\{.*\\}", Pattern.DOTALL).matcher(json);
        if (m.find()) json = m.group();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private void normalizeNumber(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) { row.put(key, 0); return; }
        if (v instanceof Number) return;
        try { row.put(key, new BigDecimal(v.toString())); } catch (Exception e) { row.put(key, 0); }
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
    private String str(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? def : s;
    }
}
