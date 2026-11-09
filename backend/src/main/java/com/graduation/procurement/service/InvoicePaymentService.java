package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.Invoice;
import com.graduation.procurement.entity.PaymentRequest;
import com.graduation.procurement.entity.PurchaseOrder;
import com.graduation.procurement.mapper.InvoiceMapper;
import com.graduation.procurement.mapper.PaymentRequestMapper;
import com.graduation.procurement.mapper.PurchaseOrderMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoicePaymentService {
    private final InvoiceMapper invoiceMapper;
    private final PaymentRequestMapper paymentMapper;
    private final PurchaseOrderMapper orderMapper;
    private final OperLogService operLogService;
    private final NotifyService notifyService;

    public List<Invoice> listInvoices() {
        return invoiceMapper.selectList(new LambdaQueryWrapper<Invoice>().orderByDesc(Invoice::getCreatedAt));
    }

    @Transactional
    public Long createInvoice(Invoice body) {
        PurchaseOrder order = orderMapper.selectById(body.getOrderId());
        if (order == null) throw new BizException("订单不存在");
        body.setSupplierId(order.getSupplierId());
        body.setStatus("PENDING");
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        invoiceMapper.insert(body);
        operLogService.log("发票", "登记", body.getInvoiceNo());
        return body.getId();
    }

    @Transactional
    public void matchInvoice(Long id, String status, String remark) {
        if (!SecurityUtils.hasAnyRole("FINANCE", "ADMIN", "PURCHASE_MANAGER")) throw new BizException("无对账权限");
        Invoice inv = invoiceMapper.selectById(id);
        if (inv == null) throw new BizException("发票不存在");
        if (!"MATCHED".equalsIgnoreCase(status) && !"REJECTED".equalsIgnoreCase(status)) throw new BizException("status 需为 MATCHED 或 REJECTED");
        inv.setStatus(status.toUpperCase());
        if (remark != null) inv.setRemark(remark);
        inv.setUpdatedAt(LocalDateTime.now());
        invoiceMapper.updateById(inv);
        operLogService.log("发票", "对账", inv.getInvoiceNo() + ":" + inv.getStatus());
    }

    public List<PaymentRequest> listPayments() {
        return paymentMapper.selectList(new LambdaQueryWrapper<PaymentRequest>().orderByDesc(PaymentRequest::getCreatedAt));
    }

    @Transactional
    public Long createPayment(PaymentRequest body) {
        PurchaseOrder order = orderMapper.selectById(body.getOrderId());
        if (order == null) throw new BizException("订单不存在");
        body.setPaymentNo(PurchaseRequestService.genNo("PAY"));
        body.setApplicantId(SecurityUtils.currentUser().getId());
        body.setStatus("DRAFT");
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        paymentMapper.insert(body);
        operLogService.log("付款", "新建", body.getPaymentNo());
        return body.getId();
    }

    @Transactional
    public void submitPayment(Long id) {
        PaymentRequest p = paymentMapper.selectById(id);
        if (p == null) throw new BizException("付款申请不存在");
        if (!"DRAFT".equals(p.getStatus()) && !"REJECTED".equals(p.getStatus())) throw new BizException("当前状态不可提交");
        p.setStatus("SUBMITTED");
        p.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(p);
        operLogService.log("付款", "提交", p.getPaymentNo());
    }

    @Transactional
    public void approvePayment(Long id, String result, String comment) {
        if (!SecurityUtils.hasAnyRole("FINANCE", "ADMIN")) throw new BizException("需要财务权限");
        PaymentRequest p = paymentMapper.selectById(id);
        if (p == null) throw new BizException("付款申请不存在");
        if (!"SUBMITTED".equals(p.getStatus())) throw new BizException("仅已提交可审批");
        p.setStatus("PASS".equalsIgnoreCase(result) ? "APPROVED" : "REJECTED");
        p.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(p);
        operLogService.log("付款", "审批", p.getPaymentNo() + ":" + p.getStatus() + " " + (comment == null ? "" : comment));
        notifyService.notifyUser(p.getApplicantId(), "付款审批结果", p.getPaymentNo() + " => " + p.getStatus(), "PAYMENT", p.getId());
    }

    @Transactional
    public void markPaid(Long id) {
        if (!SecurityUtils.hasAnyRole("FINANCE", "ADMIN")) throw new BizException("需要财务权限");
        PaymentRequest p = paymentMapper.selectById(id);
        if (p == null) throw new BizException("付款申请不存在");
        if (!"APPROVED".equals(p.getStatus())) throw new BizException("需先审批通过");
        p.setStatus("PAID");
        p.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(p);
        operLogService.log("付款", "标记已付", p.getPaymentNo());
    }
}
