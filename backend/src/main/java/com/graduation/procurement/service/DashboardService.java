package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.RequestStatuses;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<String> FLOW = List.of(
            RequestStatuses.DRAFT,
            RequestStatuses.PENDING_L1,
            RequestStatuses.PENDING_L2,
            RequestStatuses.PENDING_L3,
            RequestStatuses.PENDING_INQUIRY,
            RequestStatuses.PENDING_ORDER,
            RequestStatuses.PENDING_RECEIVE,
            RequestStatuses.COMPLETED,
            RequestStatuses.REJECTED,
            RequestStatuses.CANCELLED
    );
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry(RequestStatuses.DRAFT, "草稿"),
            Map.entry(RequestStatuses.PENDING_L1, "一级审批"),
            Map.entry(RequestStatuses.PENDING_L2, "二级审批"),
            Map.entry(RequestStatuses.PENDING_L3, "三级审批"),
            Map.entry(RequestStatuses.PENDING_INQUIRY, "询价比价"),
            Map.entry(RequestStatuses.PENDING_ORDER, "待下单"),
            Map.entry(RequestStatuses.PENDING_RECEIVE, "入库验收"),
            Map.entry(RequestStatuses.COMPLETED, "已完成"),
            Map.entry(RequestStatuses.REJECTED, "已驳回"),
            Map.entry(RequestStatuses.CANCELLED, "已取消")
    );

    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestItemMapper requestItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final SupplierMapper supplierMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;
    private final BudgetSubjectMapper budgetSubjectMapper;
    private final ContractInfoMapper contractInfoMapper;

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<PurchaseRequest> requests = requestMapper.selectList(new LambdaQueryWrapper<PurchaseRequest>().orderByDesc(PurchaseRequest::getUpdatedAt));
        List<PurchaseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
        m.put("pendingL1", countStatus(requests, RequestStatuses.PENDING_L1));
        m.put("pendingL2", countStatus(requests, RequestStatuses.PENDING_L2));
        m.put("pendingL3", countStatus(requests, RequestStatuses.PENDING_L3));
        m.put("pendingInquiry", countStatus(requests, RequestStatuses.PENDING_INQUIRY));
        m.put("pendingReceive", orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>().in(PurchaseOrder::getStatus, RequestStatuses.PENDING_RECEIVE, "PARTIAL")));
        BigDecimal amount = orders.stream().map(o -> nz(o.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("orderAmountTotal", amount);
        m.put("orderCount", orders.size());
        m.put("requestCount", requests.size());
        m.put("supplierCount", supplierMapper.selectCount(new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 1)));

        Map<Long, BigDecimal> bySp = new HashMap<>();
        for (PurchaseOrder o : orders) {
            if (o.getSupplierId() == null) continue;
            bySp.merge(o.getSupplierId(), nz(o.getTotalAmount()), BigDecimal::add);
        }
        List<Map<String, Object>> top = bySp.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> {
                    Supplier s = supplierMapper.selectById(e.getKey());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("supplierId", e.getKey());
                    row.put("name", s == null ? "-" : s.getName());
                    row.put("amount", e.getValue());
                    return row;
                }).collect(Collectors.toList());
        m.put("supplierTop", top);
        m.put("inventoryAlerts", inventoryAlerts());

        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (String key : FLOW) statusDist.put(key, countStatus(requests, key));
        m.put("requestStatusDist", statusDist);
        m.put("monthlyTrend", monthlyTrend(orders, requests));
        m.put("categoryShare", categoryShare());
        m.put("recentFlow", recentFlow(requests));
        m.put("budgetPressure", budgetPressure());
        long expiring = contractInfoMapper.selectCount(new LambdaQueryWrapper<ContractInfo>()
                .eq(ContractInfo::getStatus, "ACTIVE")
                .le(ContractInfo::getEndDate, LocalDate.now().plusDays(30))
                .ge(ContractInfo::getEndDate, LocalDate.now()));
        m.put("contractsExpiring", expiring);
        return m;
    }

    public Map<String, Object> processBoard() {
        List<PurchaseRequest> requests = requestMapper.selectList(new LambdaQueryWrapper<PurchaseRequest>()
                .orderByDesc(PurchaseRequest::getUpdatedAt));
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (String key : FLOW) {
            List<PurchaseRequest> matched = requests.stream().filter(r -> key.equals(r.getStatus())).toList();
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("key", key);
            node.put("label", LABELS.getOrDefault(key, key));
            node.put("count", matched.size());
            node.put("samples", matched.stream().limit(6).map(this::sample).toList());
            nodes.add(node);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nodes", nodes);
        m.put("total", requests.size());
        return m;
    }

    private Map<String, Object> sample(PurchaseRequest r) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getId());
        row.put("requestNo", r.getRequestNo());
        row.put("title", r.getTitle());
        row.put("deptName", r.getDeptName());
        row.put("budgetAmount", r.getBudgetAmount());
        row.put("status", r.getStatus());
        row.put("updatedAt", r.getUpdatedAt());
        return row;
    }

    private long countStatus(List<PurchaseRequest> requests, String status) {
        return requests.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    private List<Map<String, Object>> inventoryAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Inventory inv : inventoryMapper.selectList(new LambdaQueryWrapper<>())) {
            Material mat = materialMapper.selectById(inv.getMaterialId());
            if (mat != null && mat.getSafeStock() != null && inv.getQuantity() != null && inv.getQuantity().compareTo(mat.getSafeStock()) < 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("materialId", mat.getId());
                row.put("name", mat.getName());
                row.put("quantity", inv.getQuantity());
                row.put("safeStock", mat.getSafeStock());
                alerts.add(row);
            }
        }
        return alerts;
    }

    private List<Map<String, Object>> monthlyTrend(List<PurchaseOrder> orders, List<PurchaseRequest> requests) {
        YearMonth now = YearMonth.now();
        Map<String, BigDecimal> orderAmt = new LinkedHashMap<>();
        Map<String, BigDecimal> requestAmt = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            String key = now.minusMonths(i).format(MONTH);
            orderAmt.put(key, BigDecimal.ZERO);
            requestAmt.put(key, BigDecimal.ZERO);
        }
        for (PurchaseOrder o : orders) {
            if (o.getCreatedAt() == null) continue;
            String key = o.getCreatedAt().format(MONTH);
            if (orderAmt.containsKey(key)) orderAmt.put(key, orderAmt.get(key).add(nz(o.getTotalAmount())));
        }
        for (PurchaseRequest r : requests) {
            if (r.getCreatedAt() == null) continue;
            String key = r.getCreatedAt().format(MONTH);
            if (requestAmt.containsKey(key)) requestAmt.put(key, requestAmt.get(key).add(nz(r.getBudgetAmount())));
        }
        boolean ordersEmpty = orderAmt.values().stream().allMatch(v -> v.signum() == 0);
        List<Map<String, Object>> rows = new ArrayList<>();
        orderAmt.forEach((month, amt) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("amount", ordersEmpty ? requestAmt.get(month) : amt);
            row.put("requestAmount", requestAmt.get(month));
            row.put("source", ordersEmpty ? "request" : "order");
            rows.add(row);
        });
        return rows;
    }

    private List<Map<String, Object>> categoryShare() {
        Map<String, BigDecimal> byCat = new LinkedHashMap<>();
        List<PurchaseRequestItem> items = requestItemMapper.selectList(new LambdaQueryWrapper<>());
        for (PurchaseRequestItem item : items) {
            String cat = "未分类";
            if (item.getMaterialId() != null) {
                Material mat = materialMapper.selectById(item.getMaterialId());
                if (mat != null && mat.getCategory() != null && !mat.getCategory().isBlank()) cat = mat.getCategory();
            }
            byCat.merge(cat, nz(item.getAmount()), BigDecimal::add);
        }
        return byCat.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", e.getKey());
                    row.put("value", e.getValue());
                    return row;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> recentFlow(List<PurchaseRequest> requests) {
        return requests.stream().limit(6).map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("step", r.getTitle());
            row.put("actor", (r.getDeptName() == null ? "" : r.getDeptName()) + " · " + r.getRequestNo());
            row.put("time", r.getUpdatedAt() == null ? "" : r.getUpdatedAt().toString().replace('T', ' '));
            row.put("comment", LABELS.getOrDefault(r.getStatus(), r.getStatus()));
            row.put("status", terminal(r.getStatus()));
            row.put("requestId", r.getId());
            return row;
        }).toList();
    }

    private String terminal(String status) {
        if (RequestStatuses.REJECTED.equals(status) || RequestStatuses.CANCELLED.equals(status)) return "reject";
        if (RequestStatuses.COMPLETED.equals(status)) return "done";
        if (RequestStatuses.DRAFT.equals(status)) return "pending";
        return "active";
    }

    private List<Map<String, Object>> budgetPressure() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BudgetSubject b : budgetSubjectMapper.selectList(new LambdaQueryWrapper<BudgetSubject>().eq(BudgetSubject::getStatus, 1))) {
            BigDecimal total = nz(b.getTotalAmount());
            BigDecimal used = nz(b.getUsedAmount());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", b.getName());
            row.put("deptName", b.getDeptName());
            row.put("total", total);
            row.put("used", used);
            row.put("ratio", total.signum() == 0 ? 0 : used.multiply(BigDecimal.valueOf(100)).divide(total, 1, java.math.RoundingMode.HALF_UP));
            rows.add(row);
        }
        rows.sort((a, b) -> ((BigDecimal) b.get("ratio")).compareTo((BigDecimal) a.get("ratio")));
        return rows;
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
