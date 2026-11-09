package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.procurement.ai.DeepSeekClient;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.common.RequestStatuses;
import com.graduation.procurement.dto.QuoteDTO;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private final InquiryMapper inquiryMapper;
    private final InquiryQuoteMapper quoteMapper;
    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestItemMapper itemMapper;
    private final SupplierMapper supplierMapper;
    private final DeepSeekClient deepSeekClient;
    private final OperLogService operLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Inquiry create(Long requestId) {
        PurchaseRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("申请不存在");
        if (!RequestStatuses.PENDING_INQUIRY.equals(req.getStatus()) && !RequestStatuses.PENDING_ORDER.equals(req.getStatus())) {
            throw new BizException("申请状态不允许询价");
        }
        Inquiry exist = inquiryMapper.selectOne(new LambdaQueryWrapper<Inquiry>().eq(Inquiry::getRequestId, requestId));
        if (exist != null) return exist;
        Inquiry inq = new Inquiry();
        inq.setRequestId(requestId);
        inq.setStatus("OPEN");
        inq.setCurrentRound(1);
        inq.setCreatedAt(LocalDateTime.now());
        inq.setUpdatedAt(LocalDateTime.now());
        inquiryMapper.insert(inq);
        operLogService.log("询价", "创建", String.valueOf(requestId));
        return inq;
    }

    public Map<String, Object> detailByRequest(Long requestId) {
        Inquiry inq = inquiryMapper.selectOne(new LambdaQueryWrapper<Inquiry>().eq(Inquiry::getRequestId, requestId));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("inquiry", inq);
        if (inq != null) {
            List<InquiryQuote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<InquiryQuote>().eq(InquiryQuote::getInquiryId, inq.getId()).orderByAsc(InquiryQuote::getRoundNo));
            m.put("quotes", quotes);
            List<Supplier> suppliers = supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 1).eq(Supplier::getBlacklisted, 0));
            m.put("suppliers", suppliers);
            m.put("rounds", rounds(inq.getId()));
        }
        return m;
    }

    public Map<String, Object> rounds(Long inquiryId) {
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null) throw new BizException("询价单不存在");
        List<InquiryQuote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<InquiryQuote>().eq(InquiryQuote::getInquiryId, inquiryId).orderByAsc(InquiryQuote::getRoundNo));
        Map<Integer, List<InquiryQuote>> byRound = quotes.stream().collect(Collectors.groupingBy(q -> q.getRoundNo() == null ? 1 : q.getRoundNo(), LinkedHashMap::new, Collectors.toList()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currentRound", inq.getCurrentRound());
        m.put("rounds", byRound);
        return m;
    }

    @Transactional
    public void startNextRound(Long inquiryId, String remark) {
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null) throw new BizException("询价单不存在");
        if ("SELECTED".equals(inq.getStatus())) throw new BizException("已选定供应商，不能再开新轮");
        int next = (inq.getCurrentRound() == null ? 1 : inq.getCurrentRound()) + 1;
        inq.setCurrentRound(next);
        inq.setStatus("OPEN");
        inq.setUpdatedAt(LocalDateTime.now());
        inquiryMapper.updateById(inq);
        operLogService.log("询价", "开启第" + next + "轮", remark == null ? "" : remark);
    }

    @Transactional
    public void addQuote(Long inquiryId, QuoteDTO dto) {
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null) throw new BizException("询价单不存在");
        Supplier s = supplierMapper.selectById(dto.getSupplierId());
        if (s == null) throw new BizException("供应商不存在");
        if (s.getBlacklisted() != null && s.getBlacklisted() == 1) throw new BizException("黑名单供应商不可报价");
        InquiryQuote q = new InquiryQuote();
        q.setInquiryId(inquiryId);
        q.setRoundNo(dto.getRoundNo() == null ? (inq.getCurrentRound() == null ? 1 : inq.getCurrentRound()) : dto.getRoundNo());
        q.setSupplierId(dto.getSupplierId());
        q.setUnitPrice(dto.getUnitPrice());
        q.setTotalPrice(dto.getTotalPrice());
        q.setDeliveryDays(dto.getDeliveryDays());
        q.setRemark(dto.getRemark());
        q.setNegotiateNote(dto.getNegotiateNote());
        q.setCreatedAt(LocalDateTime.now());
        quoteMapper.insert(q);
        operLogService.log("询价", "录入报价", "inq=" + inquiryId + " round=" + q.getRoundNo());
    }

    @Transactional
    public void selectSupplier(Long inquiryId, Long supplierId) {
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null) throw new BizException("询价单不存在");
        Supplier s = supplierMapper.selectById(supplierId);
        if (s != null && s.getBlacklisted() != null && s.getBlacklisted() == 1) throw new BizException("黑名单供应商不可选定");
        long cnt = quoteMapper.selectCount(new LambdaQueryWrapper<InquiryQuote>().eq(InquiryQuote::getInquiryId, inquiryId));
        if (cnt < 1) throw new BizException("请先录入报价");
        inq.setSelectedSupplierId(supplierId);
        inq.setStatus("SELECTED");
        inq.setUpdatedAt(LocalDateTime.now());
        inquiryMapper.updateById(inq);
        PurchaseRequest req = requestMapper.selectById(inq.getRequestId());
        req.setStatus(RequestStatuses.PENDING_ORDER);
        req.setUpdatedAt(LocalDateTime.now());
        requestMapper.updateById(req);
        operLogService.log("询价", "选定供应商", String.valueOf(supplierId));
    }

    public Map<String, Object> compareReport(Long inquiryId) {
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null) throw new BizException("询价单不存在");
        List<InquiryQuote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<InquiryQuote>().eq(InquiryQuote::getInquiryId, inquiryId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InquiryQuote q : quotes) {
            Supplier s = supplierMapper.selectById(q.getSupplierId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roundNo", q.getRoundNo());
            row.put("supplierId", q.getSupplierId());
            row.put("supplierName", s == null ? null : s.getName());
            row.put("rating", s == null ? null : s.getRating());
            row.put("unitPrice", q.getUnitPrice());
            row.put("totalPrice", q.getTotalPrice());
            row.put("deliveryDays", q.getDeliveryDays());
            row.put("negotiateNote", q.getNegotiateNote());
            row.put("remark", q.getRemark());
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (Integer) r.getOrDefault("roundNo", 1))
                .thenComparing(r -> r.get("totalPrice") == null ? BigDecimal.valueOf(Long.MAX_VALUE) : (BigDecimal) r.get("totalPrice")));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("inquiryId", inquiryId);
        m.put("currentRound", inq.getCurrentRound());
        m.put("selectedSupplierId", inq.getSelectedSupplierId());
        m.put("rows", rows);
        return m;
    }

    public byte[] exportCompareExcel(Long inquiryId) {
        Map<String, Object> report = compareReport(inquiryId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) report.get("rows");
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("比价报告");
            Row h = sheet.createRow(0);
            String[] headers = {"轮次", "供应商", "评级", "单价", "总价", "交货天数", "议价说明", "备注"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int rIdx = 1;
            for (Map<String, Object> row : rows) {
                Row r = sheet.createRow(rIdx++);
                r.createCell(0).setCellValue(row.get("roundNo") == null ? 1 : ((Number) row.get("roundNo")).doubleValue());
                r.createCell(1).setCellValue(String.valueOf(row.getOrDefault("supplierName", "")));
                r.createCell(2).setCellValue(row.get("rating") == null ? 0 : ((Number) row.get("rating")).doubleValue());
                r.createCell(3).setCellValue(row.get("unitPrice") == null ? 0 : ((Number) row.get("unitPrice")).doubleValue());
                r.createCell(4).setCellValue(row.get("totalPrice") == null ? 0 : ((Number) row.get("totalPrice")).doubleValue());
                r.createCell(5).setCellValue(row.get("deliveryDays") == null ? 0 : ((Number) row.get("deliveryDays")).doubleValue());
                r.createCell(6).setCellValue(String.valueOf(row.getOrDefault("negotiateNote", "")));
                r.createCell(7).setCellValue(String.valueOf(row.getOrDefault("remark", "")));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("导出失败: " + e.getMessage());
        }
    }

    public Map<String, Object> recommend(Long inquiryId) {
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null) throw new BizException("询价单不存在");
        PurchaseRequest req = requestMapper.selectById(inq.getRequestId());
        List<PurchaseRequestItem> items = itemMapper.selectList(new LambdaQueryWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, req.getId()));
        List<Supplier> all = supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 1).eq(Supplier::getBlacklisted, 0));
        Set<String> keys = new HashSet<>();
        for (PurchaseRequestItem it : items) {
            if (it.getMaterialName() != null) {
                if (it.getMaterialName().contains("纸") || it.getMaterialName().contains("耗材")) keys.add("办公耗材");
                if (it.getMaterialName().contains("椅") || it.getMaterialName().contains("桌") || it.getMaterialName().contains("家具")) keys.add("办公家具");
                keys.add("办公设备");
            }
        }
        List<Supplier> candidates = all.stream()
                .filter(s -> keys.isEmpty() || keys.contains(s.getCategory()) || "办公设备".equals(s.getCategory()))
                .sorted(Comparator.comparing((Supplier s) -> s.getRating() == null ? 0 : s.getRating()).reversed())
                .limit(5).collect(Collectors.toList());
        List<Map<String, Object>> ruleList = new ArrayList<>();
        for (Supplier s : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("supplierId", s.getId());
            row.put("name", s.getName());
            row.put("category", s.getCategory());
            row.put("rating", s.getRating());
            row.put("admissionScore", s.getAdmissionScore());
            row.put("contact", s.getContact());
            ruleList.add(row);
        }
        String aiText; String aiJson;
        try {
            String system = "你是企业采购助手。根据候选供应商与采购明细，给出推荐排序和简要理由。只输出 JSON：{\"recommendations\":[{\"supplierId\":1,\"rank\":1,\"reason\":\"...\"}]}";
            String user = "采购标题:" + req.getTitle() + "\n预算:" + req.getBudgetAmount() + "\n明细:" + items.stream().map(i -> i.getMaterialName()+" x"+i.getQuantity()).collect(Collectors.joining(";")) + "\n候选:" + ruleList;
            aiText = deepSeekClient.chat(system, user);
            aiJson = aiText;
        } catch (BizException e) {
            aiText = e.getMessage();
            List<Map<String, Object>> fallback = new ArrayList<>();
            int rank = 1;
            for (Supplier s : candidates) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("supplierId", s.getId()); r.put("rank", rank++);
                r.put("reason", "规则推荐：品类匹配且评级 " + s.getRating() + " 星（大模型未启用）");
                fallback.add(r);
            }
            try { aiJson = objectMapper.writeValueAsString(Map.of("recommendations", fallback, "notice", e.getMessage())); }
            catch (Exception ignored) { aiJson = "{}"; }
        }
        inq.setAiRecommendJson(aiJson);
        inq.setUpdatedAt(LocalDateTime.now());
        inquiryMapper.updateById(inq);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidates", ruleList);
        result.put("aiRaw", aiText);
        result.put("aiRecommendJson", aiJson);
        return result;
    }
}
