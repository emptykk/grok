package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.InboundOrder;
import com.graduation.procurement.entity.PurchaseOrder;
import com.graduation.procurement.entity.ReturnOrder;
import com.graduation.procurement.entity.Supplier;
import com.graduation.procurement.entity.SupplierScoreRecord;
import com.graduation.procurement.mapper.InboundOrderMapper;
import com.graduation.procurement.mapper.PurchaseOrderMapper;
import com.graduation.procurement.mapper.ReturnOrderMapper;
import com.graduation.procurement.mapper.SupplierMapper;
import com.graduation.procurement.mapper.SupplierScoreRecordMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierExtService {
    private static final String[] RADAR = {"价格", "交期", "质量", "服务", "合规"};

    private final SupplierMapper supplierMapper;
    private final SupplierScoreRecordMapper scoreMapper;
    private final PurchaseOrderMapper orderMapper;
    private final InboundOrderMapper inboundMapper;
    private final ReturnOrderMapper returnMapper;
    private final OperLogService operLogService;

    @Transactional
    public void score(Long id, BigDecimal score, String dimension, String remark) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BizException("供应商不存在");
        SupplierScoreRecord r = new SupplierScoreRecord();
        r.setSupplierId(id); r.setScore(score); r.setDimension(dimension); r.setRemark(remark);
        r.setReviewerId(SecurityUtils.currentUser().getId()); r.setCreatedAt(LocalDateTime.now());
        scoreMapper.insert(r);
        // 更新准入分：最近记录均值简化为直接用本次分
        s.setAdmissionScore(score);
        s.setUpdatedAt(LocalDateTime.now());
        supplierMapper.updateById(s);
        operLogService.log("供应商", "评分", s.getName() + "=" + score);
    }

    public List<SupplierScoreRecord> scores(Long id) {
        return scoreMapper.selectList(new LambdaQueryWrapper<SupplierScoreRecord>().eq(SupplierScoreRecord::getSupplierId, id).orderByDesc(SupplierScoreRecord::getCreatedAt));
    }

    @Transactional
    public void blacklist(Long id, boolean blacklisted, String reason) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BizException("供应商不存在");
        s.setBlacklisted(blacklisted ? 1 : 0);
        s.setBlacklistReason(reason);
        if (blacklisted) s.setStatus(0);
        s.setUpdatedAt(LocalDateTime.now());
        supplierMapper.updateById(s);
        operLogService.log("供应商", blacklisted ? "拉黑" : "解除黑名单", s.getName());
    }

    @Transactional
    public void review(Long id, String result, LocalDate nextReviewDate, String remark) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BizException("供应商不存在");
        s.setLastReviewDate(LocalDate.now());
        s.setNextReviewDate(nextReviewDate);
        if ("FAIL".equalsIgnoreCase(result)) {
            s.setStatus(0);
            s.setRemark((s.getRemark() == null ? "" : s.getRemark() + ";") + "年审未通过:" + (remark == null ? "" : remark));
        } else {
            s.setStatus(1);
        }
        s.setUpdatedAt(LocalDateTime.now());
        supplierMapper.updateById(s);
        operLogService.log("供应商", "年审", s.getName() + ":" + result);
    }

    public Map<String, Object> profile(Long id) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new BizException("供应商不存在");
        List<PurchaseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getSupplierId, id).orderByDesc(PurchaseOrder::getCreatedAt));
        BigDecimal total = orders.stream().map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SupplierScoreRecord> scores = scores(id);
        List<Map<String, Object>> radar = radar(s, scores);
        int checked = 0, onTime = 0;
        for (PurchaseOrder o : orders) {
            if (o.getExpectDate() == null) continue;
            InboundOrder inbound = inboundMapper.selectOne(new LambdaQueryWrapper<InboundOrder>()
                    .eq(InboundOrder::getOrderId, o.getId()).orderByDesc(InboundOrder::getCreatedAt).last("LIMIT 1"));
            if (inbound == null || inbound.getCreatedAt() == null) continue;
            checked++;
            if (!inbound.getCreatedAt().toLocalDate().isAfter(o.getExpectDate())) onTime++;
        }
        long returns = 0;
        for (PurchaseOrder o : orders) {
            returns += returnMapper.selectCount(new LambdaQueryWrapper<ReturnOrder>().eq(ReturnOrder::getOrderId, o.getId()));
        }
        BigDecimal allAmount = orderMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int share = allAmount.signum() == 0 ? 0 : total.multiply(BigDecimal.valueOf(100)).divide(allAmount, 0, RoundingMode.HALF_UP).intValue();
        LocalDate first = orders.stream().map(PurchaseOrder::getCreatedAt).filter(t -> t != null)
                .map(LocalDateTime::toLocalDate).min(LocalDate::compareTo).orElse(null);
        int years = first == null ? 0 : Math.max(1, (int) ChronoUnit.YEARS.between(first, LocalDate.now()) + (first.getYear() == LocalDate.now().getYear() && orders.size() > 0 ? 1 : 0));
        if (first != null && ChronoUnit.DAYS.between(first, LocalDate.now()) < 400) years = 1;

        List<Map<String, Object>> tags = new ArrayList<>();
        if (s.getBlacklisted() != null && s.getBlacklisted() == 1) tags.add(tag("BLACKLIST", "黑名单", "high"));
        if (s.getStatus() != null && s.getStatus() == 0) tags.add(tag("DISABLED", "已停用", "high"));
        if (s.getAdmissionScore() != null && s.getAdmissionScore().compareTo(BigDecimal.valueOf(70)) < 0) tags.add(tag("LOW_SCORE", "准入分偏低", "mid"));
        if (s.getNextReviewDate() != null && !s.getNextReviewDate().isAfter(LocalDate.now().plusDays(30))) tags.add(tag("REVIEW_DUE", "年审临期", "mid"));
        if (share >= 45 && orders.size() >= 2) tags.add(tag("CONCENTRATION", "采购额集中", "mid"));
        if (returns > 0) tags.add(tag("RETURN", "存在退换货", "mid"));
        if (tags.isEmpty()) tags.add(tag("STABLE", "合作稳定", "ok"));

        List<Map<String, Object>> recent = new ArrayList<>();
        for (PurchaseOrder o : orders.stream().limit(8).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", o.getId());
            row.put("no", o.getOrderNo());
            row.put("amount", o.getTotalAmount());
            row.put("date", o.getCreatedAt() == null ? "" : o.getCreatedAt().toLocalDate().toString());
            row.put("status", o.getStatus());
            recent.add(row);
        }
        String summary = s.getName() + " 当前准入分 " + (s.getAdmissionScore() == null ? "未评" : s.getAdmissionScore().stripTrailingZeros().toPlainString())
                + "，历史订单 " + orders.size() + " 笔，累计成交 ¥" + total.stripTrailingZeros().toPlainString()
                + (share > 0 ? "，占全部订单金额约 " + share + "%" : "")
                + (returns > 0 ? "，关联退换货 " + returns + " 单。" : "。")
                + (s.getBlacklisted() != null && s.getBlacklisted() == 1 ? "该供应商已列入黑名单，禁止新采购。" : "可结合雷达维度决定是否进入比价短名单。");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("rating", s.getRating());
        m.put("category", s.getCategory());
        m.put("contact", (s.getContact() == null ? "-" : s.getContact()) + " / " + (s.getPhone() == null ? "-" : s.getPhone()));
        m.put("address", s.getRemark() == null ? "-" : s.getRemark());
        m.put("status", s.getStatus());
        m.put("admissionScore", s.getAdmissionScore());
        m.put("blacklisted", s.getBlacklisted());
        m.put("blacklistReason", s.getBlacklistReason());
        m.put("lastReviewDate", s.getLastReviewDate());
        m.put("nextReviewDate", s.getNextReviewDate());
        m.put("cooperationYears", orders.isEmpty() ? 0 : years);
        m.put("orderCount", orders.size());
        m.put("totalAmount", total);
        m.put("amountShare", share);
        m.put("onTimeRate", checked == 0 ? null : BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(checked), 2, RoundingMode.HALF_UP));
        m.put("onTimeSample", checked);
        m.put("qualifyRate", orders.isEmpty() ? null : BigDecimal.ONE.subtract(BigDecimal.valueOf(Math.min(returns, orders.size()))
                .divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP)));
        m.put("returnCount", returns);
        m.put("radar", radar);
        m.put("riskTags", tags);
        m.put("historySummary", summary);
        m.put("recentOrders", recent);
        m.put("scores", scores.stream().limit(8).toList());
        return m;
    }

    private List<Map<String, Object>> radar(Supplier s, List<SupplierScoreRecord> scores) {
        Map<String, List<BigDecimal>> bucket = new LinkedHashMap<>();
        for (String d : RADAR) bucket.put(d, new ArrayList<>());
        for (SupplierScoreRecord r : scores) {
            String dim = normalizeDim(r.getDimension());
            if (dim != null && r.getScore() != null) bucket.get(dim).add(r.getScore());
        }
        BigDecimal base = s.getAdmissionScore() == null
                ? BigDecimal.valueOf((s.getRating() == null ? 3 : s.getRating()) * 18L)
                : s.getAdmissionScore();
        int[] drift = {-6, -2, 2, -4, s.getBlacklisted() != null && s.getBlacklisted() == 1 ? -40 : 0};
        List<Map<String, Object>> radar = new ArrayList<>();
        for (int i = 0; i < RADAR.length; i++) {
            List<BigDecimal> vals = bucket.get(RADAR[i]);
            BigDecimal value;
            boolean measured = vals != null && !vals.isEmpty();
            if (measured) {
                value = vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(vals.size()), 1, RoundingMode.HALF_UP);
            } else {
                value = base.add(BigDecimal.valueOf(drift[i]));
            }
            if (value.compareTo(BigDecimal.valueOf(100)) > 0) value = BigDecimal.valueOf(100);
            if (value.compareTo(BigDecimal.ZERO) < 0) value = BigDecimal.ZERO;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", RADAR[i]);
            row.put("value", value);
            row.put("measured", measured);
            radar.add(row);
        }
        return radar;
    }

    private String normalizeDim(String raw) {
        if (raw == null) return null;
        String d = raw.trim().toLowerCase();
        if (d.contains("价") || d.contains("price")) return "价格";
        if (d.contains("交") || d.contains("deliver")) return "交期";
        if (d.contains("质") || d.contains("quality")) return "质量";
        if (d.contains("服") || d.contains("service")) return "服务";
        if (d.contains("合") || d.contains("compl")) return "合规";
        for (String name : RADAR) if (name.equals(raw.trim())) return name;
        return null;
    }

    private Map<String, Object> tag(String code, String label, String level) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("label", label);
        m.put("level", level);
        return m;
    }

    public List<Supplier> reviewDue() {
        return supplierMapper.selectList(new LambdaQueryWrapper<Supplier>()
                .isNotNull(Supplier::getNextReviewDate)
                .le(Supplier::getNextReviewDate, LocalDate.now().plusDays(30))
                .orderByAsc(Supplier::getNextReviewDate));
    }
}
