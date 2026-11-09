package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.ai.DeepSeekClient;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.ContractInfo;
import com.graduation.procurement.entity.PurchaseOrder;
import com.graduation.procurement.mapper.ContractInfoMapper;
import com.graduation.procurement.mapper.PurchaseOrderMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final ContractInfoMapper mapper;
    private final PurchaseOrderMapper orderMapper;
    private final DeepSeekClient deepSeekClient;
    private final OperLogService operLogService;

    public Page<ContractInfo> page(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<ContractInfo> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq(ContractInfo::getStatus, status);
        if (keyword != null && !keyword.isBlank()) q.and(w -> w.like(ContractInfo::getTitle, keyword).or().like(ContractInfo::getContractNo, keyword));
        q.orderByDesc(ContractInfo::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    public ContractInfo get(Long id) {
        ContractInfo c = mapper.selectById(id);
        if (c == null) throw new BizException("合同不存在");
        return c;
    }

    @Transactional
    public Long create(ContractInfo body) {
        if (!SecurityUtils.hasAnyRole("ADMIN", "PURCHASE_MANAGER", "FINANCE")) throw new BizException("无权限");
        PurchaseOrder order = orderMapper.selectById(body.getOrderId());
        if (order == null) throw new BizException("订单不存在");
        body.setContractNo(PurchaseRequestService.genNo("CT"));
        body.setSupplierId(order.getSupplierId());
        if (body.getAmount() == null) body.setAmount(order.getTotalAmount());
        body.setStatus("DRAFT");
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        mapper.insert(body);
        operLogService.log("合同", "新建", body.getContractNo());
        return body.getId();
    }

    @Transactional
    public void update(Long id, ContractInfo body) {
        ContractInfo c = get(id);
        if (!"DRAFT".equals(c.getStatus())) throw new BizException("仅草稿可编辑");
        c.setTitle(body.getTitle());
        c.setAmount(body.getAmount());
        c.setStartDate(body.getStartDate());
        c.setEndDate(body.getEndDate());
        c.setContentText(body.getContentText());
        c.setRemark(body.getRemark());
        c.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(c);
        operLogService.log("合同", "更新", c.getContractNo());
    }

    @Transactional
    public void activate(Long id) {
        ContractInfo c = get(id);
        c.setStatus("ACTIVE");
        c.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(c);
        operLogService.log("合同", "生效", c.getContractNo());
    }

    @Transactional
    public String riskSummary(Long id) {
        ContractInfo c = get(id);
        String text = c.getContentText() == null ? "" : c.getContentText();
        String summary;
        try {
            summary = deepSeekClient.chat(
                    "你是采购合同法务助手。请用中文列出合同风险点（付款、违约、质保、验收），不超过300字。",
                    "合同标题:" + c.getTitle() + "\n金额:" + c.getAmount() + "\n正文:\n" + text);
        } catch (BizException e) {
            summary = "（大模型未配置）建议人工关注：付款节点、逾期违约金、质保期、验收标准是否明确。原文摘要长度=" + text.length() + "。" + e.getMessage();
        }
        c.setRiskSummary(summary);
        c.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(c);
        operLogService.log("合同", "风险摘要", c.getContractNo());
        return summary;
    }

    public List<ContractInfo> expiring(int days) {
        LocalDate end = LocalDate.now().plusDays(days <= 0 ? 30 : days);
        return mapper.selectList(new LambdaQueryWrapper<ContractInfo>()
                .eq(ContractInfo::getStatus, "ACTIVE")
                .le(ContractInfo::getEndDate, end)
                .ge(ContractInfo::getEndDate, LocalDate.now())
                .orderByAsc(ContractInfo::getEndDate));
    }
}
