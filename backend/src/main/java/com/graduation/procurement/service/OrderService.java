package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.common.RequestStatuses;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final InquiryMapper inquiryMapper;
    private final InquiryQuoteMapper quoteMapper;
    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestItemMapper requestItemMapper;
    private final SupplierMapper supplierMapper;
    private final OperLogService operLogService;

    @Transactional
    public Long createFromInquiry(Long inquiryId, LocalDate expectDate, String remark) {
        Inquiry inq = inquiryMapper.selectById(inquiryId);
        if (inq == null || inq.getSelectedSupplierId() == null) throw new BizException("请先选定供应商");
        PurchaseRequest req = requestMapper.selectById(inq.getRequestId());
        if (!RequestStatuses.PENDING_ORDER.equals(req.getStatus())) throw new BizException("申请状态不允许下单");

        InquiryQuote selected = quoteMapper.selectList(new LambdaQueryWrapper<InquiryQuote>()
                .eq(InquiryQuote::getInquiryId, inquiryId)
                .eq(InquiryQuote::getSupplierId, inq.getSelectedSupplierId()))
                .stream().findFirst().orElse(null);

        List<PurchaseRequestItem> items = requestItemMapper.selectList(new LambdaQueryWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, req.getId()));
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(PurchaseRequestService.genNo("PO"));
        order.setRequestId(req.getId());
        order.setInquiryId(inquiryId);
        order.setSupplierId(inq.getSelectedSupplierId());
        order.setStatus(RequestStatuses.PENDING_RECEIVE);
        order.setExpectDate(expectDate);
        order.setRemark(remark);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal unit = selected != null && selected.getUnitPrice() != null ? selected.getUnitPrice() : null;
        for (PurchaseRequestItem it : items) {
            BigDecimal up = unit != null ? unit : (it.getEstimatePrice() == null ? BigDecimal.ZERO : it.getEstimatePrice());
            total = total.add(up.multiply(it.getQuantity()));
        }
        if (selected != null && selected.getTotalPrice() != null) total = selected.getTotalPrice();
        order.setTotalAmount(total);
        orderMapper.insert(order);

        for (PurchaseRequestItem it : items) {
            PurchaseOrderItem oi = new PurchaseOrderItem();
            oi.setOrderId(order.getId());
            oi.setMaterialName(it.getMaterialName());
            oi.setSpec(it.getSpec());
            oi.setUnit(it.getUnit());
            oi.setQuantity(it.getQuantity());
            oi.setReceivedQty(BigDecimal.ZERO);
            BigDecimal up = unit != null ? unit : (it.getEstimatePrice() == null ? BigDecimal.ZERO : it.getEstimatePrice());
            oi.setUnitPrice(up);
            oi.setAmount(up.multiply(it.getQuantity()));
            orderItemMapper.insert(oi);
        }

        req.setStatus(RequestStatuses.PENDING_RECEIVE);
        req.setUpdatedAt(LocalDateTime.now());
        requestMapper.updateById(req);
        inq.setStatus("ORDERED");
        inquiryMapper.updateById(inq);
        operLogService.log("订单", "创建", order.getOrderNo());
        return order.getId();
    }

    public Page<PurchaseOrder> page(int page, int size, String status) {
        LambdaQueryWrapper<PurchaseOrder> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq(PurchaseOrder::getStatus, status);
        q.orderByDesc(PurchaseOrder::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), q);
    }

    public Map<String, Object> detail(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException("订单不存在");
        List<PurchaseOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, id));
        Supplier sp = supplierMapper.selectById(order.getSupplierId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", order);
        m.put("items", items);
        m.put("supplier", sp);
        return m;
    }
}
