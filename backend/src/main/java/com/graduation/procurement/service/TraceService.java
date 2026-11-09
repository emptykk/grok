package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TraceService {
    private final PurchaseRequestMapper requestMapper;
    private final PurchaseApprovalMapper approvalMapper;
    private final SysUserMapper userMapper;
    private final InquiryMapper inquiryMapper;
    private final InquiryQuoteMapper quoteMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper orderMapper;
    private final InboundOrderMapper inboundMapper;
    private final ContractInfoMapper contractMapper;
    private final InvoiceMapper invoiceMapper;
    private final PaymentRequestMapper paymentMapper;
    private final ReturnOrderMapper returnMapper;

    public Map<String, Object> trace(Long requestId) {
        PurchaseRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("申请不存在");
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(event(req.getCreatedAt(), "申请", "创建申请",
                req.getRequestNo() + " " + req.getTitle() + " · 预算 ¥" + req.getBudgetAmount(), "done"));

        List<PurchaseApproval> approvals = approvalMapper.selectList(new LambdaQueryWrapper<PurchaseApproval>()
                .eq(PurchaseApproval::getRequestId, requestId).orderByAsc(PurchaseApproval::getCreatedAt));
        for (PurchaseApproval a : approvals) {
            SysUser u = userMapper.selectById(a.getApproverId());
            String who = u == null ? "审批人" : u.getRealName();
            boolean reject = "REJECT".equalsIgnoreCase(a.getResult());
            events.add(event(a.getCreatedAt(), "审批",
                    a.getLevel() + (reject ? " 驳回" : " 通过"),
                    who + (a.getComment() == null || a.getComment().isBlank() ? "" : "：" + a.getComment()),
                    reject ? "reject" : "done"));
        }

        Inquiry inq = inquiryMapper.selectOne(new LambdaQueryWrapper<Inquiry>().eq(Inquiry::getRequestId, requestId));
        if (inq != null) {
            events.add(event(inq.getCreatedAt(), "询价", "开启询价",
                    "状态 " + inq.getStatus() + " · 第 " + (inq.getCurrentRound() == null ? 1 : inq.getCurrentRound()) + " 轮", "done"));
            List<InquiryQuote> quotes = quoteMapper.selectList(new LambdaQueryWrapper<InquiryQuote>()
                    .eq(InquiryQuote::getInquiryId, inq.getId()).orderByAsc(InquiryQuote::getRoundNo).orderByAsc(InquiryQuote::getCreatedAt));
            Map<Integer, List<InquiryQuote>> byRound = new LinkedHashMap<>();
            for (InquiryQuote q : quotes) {
                int round = q.getRoundNo() == null ? 1 : q.getRoundNo();
                byRound.computeIfAbsent(round, k -> new ArrayList<>()).add(q);
            }
            for (Map.Entry<Integer, List<InquiryQuote>> e : byRound.entrySet()) {
                StringBuilder detail = new StringBuilder();
                LocalDateTime when = null;
                for (InquiryQuote q : e.getValue()) {
                    if (when == null) when = q.getCreatedAt();
                    Supplier s = supplierMapper.selectById(q.getSupplierId());
                    if (detail.length() > 0) detail.append("；");
                    detail.append(s == null ? ("供应商#" + q.getSupplierId()) : s.getName())
                            .append(" ¥").append(q.getTotalPrice())
                            .append(" / ").append(q.getDeliveryDays()).append("天");
                }
                events.add(event(when, "询价", "第 " + e.getKey() + " 轮报价", detail.toString(), "done"));
            }
            if (inq.getSelectedSupplierId() != null) {
                Supplier picked = supplierMapper.selectById(inq.getSelectedSupplierId());
                events.add(event(inq.getUpdatedAt(), "询价", "选定供应商",
                        picked == null ? String.valueOf(inq.getSelectedSupplierId()) : picked.getName(), "done"));
            }
        }

        PurchaseOrder order = orderMapper.selectOne(new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getRequestId, requestId));
        if (order != null) {
            Supplier sp = supplierMapper.selectById(order.getSupplierId());
            events.add(event(order.getCreatedAt(), "订单", "生成订单 " + order.getOrderNo(),
                    (sp == null ? "" : sp.getName() + " · ") + "¥" + order.getTotalAmount() + " · " + order.getStatus(), "done"));
            List<InboundOrder> inbounds = inboundMapper.selectList(new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getOrderId, order.getId()));
            for (InboundOrder in : inbounds) {
                boolean diff = in.getDiffFlag() != null && in.getDiffFlag() == 1;
                events.add(event(in.getCreatedAt(), "入库", diff ? "入库并记录差异 " + in.getInboundNo() : "入库验收 " + in.getInboundNo(),
                        diff ? (in.getDiffRemark() == null ? "有到货差异" : in.getDiffRemark()) : "数量规格核对通过",
                        diff ? "warn" : "done"));
            }
            List<ReturnOrder> returns = returnMapper.selectList(new LambdaQueryWrapper<ReturnOrder>().eq(ReturnOrder::getOrderId, order.getId()));
            for (ReturnOrder r : returns) {
                events.add(event(r.getCreatedAt(), "退换货", r.getReturnNo() + " " + r.getType(),
                        (r.getReason() == null ? "" : r.getReason()) + " · " + r.getStatus(), "warn"));
            }
            List<ContractInfo> contracts = contractMapper.selectList(new LambdaQueryWrapper<ContractInfo>().eq(ContractInfo::getOrderId, order.getId()));
            for (ContractInfo c : contracts) {
                events.add(event(c.getCreatedAt(), "合同", c.getContractNo() + " " + c.getTitle(),
                        "到期 " + c.getEndDate() + " · " + c.getStatus(), "done"));
            }
            List<Invoice> invoices = invoiceMapper.selectList(new LambdaQueryWrapper<Invoice>().eq(Invoice::getOrderId, order.getId()));
            for (Invoice inv : invoices) {
                events.add(event(inv.getCreatedAt(), "发票", "登记发票 " + inv.getInvoiceNo(),
                        "¥" + inv.getAmount() + " · " + inv.getStatus(), "done"));
            }
            List<PaymentRequest> pays = paymentMapper.selectList(new LambdaQueryWrapper<PaymentRequest>().eq(PaymentRequest::getOrderId, order.getId()));
            for (PaymentRequest p : pays) {
                events.add(event(p.getCreatedAt(), "付款", p.getPaymentNo(), "¥" + p.getAmount() + " · " + p.getStatus(), "done"));
            }
        }

        events.sort(Comparator.comparing(e -> String.valueOf(e.get("time"))));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("request", req);
        m.put("events", events);
        m.put("hasOrder", order != null);
        m.put("hasInquiry", inq != null);
        return m;
    }

    private Map<String, Object> event(LocalDateTime time, String phase, String title, String detail, String tone) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("time", time == null ? "" : time.toString().replace('T', ' '));
        row.put("phase", phase);
        row.put("title", title);
        row.put("detail", detail);
        row.put("tone", tone);
        return row;
    }
}
