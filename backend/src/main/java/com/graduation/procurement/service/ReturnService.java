package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import com.graduation.procurement.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReturnService {
    private final ReturnOrderMapper returnMapper;
    private final ReturnOrderItemMapper itemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final MaterialMapper materialMapper;
    private final InventoryMapper inventoryMapper;
    private final OperLogService operLogService;
    private final NotifyService notifyService;

    @Data
    public static class CreateDTO {
        private Long orderId; private String type; private String reason; private List<Item> items;
        @Data public static class Item { private Long orderItemId; private BigDecimal quantity; private String remark; }
    }

    public List<ReturnOrder> list() {
        return returnMapper.selectList(new LambdaQueryWrapper<ReturnOrder>().orderByDesc(ReturnOrder::getCreatedAt));
    }

    public Map<String, Object> detail(Long id) {
        ReturnOrder r = returnMapper.selectById(id);
        if (r == null) throw new BizException("退换货单不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("returnOrder", r);
        m.put("items", itemMapper.selectList(new LambdaQueryWrapper<ReturnOrderItem>().eq(ReturnOrderItem::getReturnId, id)));
        return m;
    }

    @Transactional
    public Long create(CreateDTO dto) {
        PurchaseOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) throw new BizException("订单不存在");
        if (!"RETURN".equals(dto.getType()) && !"EXCHANGE".equals(dto.getType())) throw new BizException("type 必须为 RETURN 或 EXCHANGE");
        ReturnOrder r = new ReturnOrder();
        r.setReturnNo(PurchaseRequestService.genNo("RT"));
        r.setOrderId(dto.getOrderId());
        r.setType(dto.getType());
        r.setStatus("PENDING");
        r.setReason(dto.getReason());
        r.setCreatedBy(SecurityUtils.currentUser().getId());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        returnMapper.insert(r);
        if (dto.getItems() != null) {
            for (CreateDTO.Item it : dto.getItems()) {
                PurchaseOrderItem oi = orderItemMapper.selectById(it.getOrderItemId());
                if (oi == null) throw new BizException("订单明细无效");
                ReturnOrderItem ri = new ReturnOrderItem();
                ri.setReturnId(r.getId());
                ri.setOrderItemId(it.getOrderItemId());
                ri.setMaterialName(oi.getMaterialName());
                ri.setQuantity(it.getQuantity());
                ri.setRemark(it.getRemark());
                itemMapper.insert(ri);
            }
        }
        operLogService.log("退换货", "创建", r.getReturnNo());
        return r.getId();
    }

    @Transactional
    public void approve(Long id, String result, String comment) {
        if (!SecurityUtils.hasAnyRole("PURCHASE_MANAGER", "ADMIN", "WAREHOUSE")) throw new BizException("无审批权限");
        ReturnOrder r = returnMapper.selectById(id);
        if (r == null) throw new BizException("单据不存在");
        if (!"PENDING".equals(r.getStatus())) throw new BizException("当前状态不可审批");
        r.setStatus("PASS".equalsIgnoreCase(result) ? "APPROVED" : "REJECTED");
        r.setUpdatedAt(LocalDateTime.now());
        returnMapper.updateById(r);
        operLogService.log("退换货", r.getStatus(), r.getReturnNo() + " " + (comment == null ? "" : comment));
        notifyService.notifyUser(r.getCreatedBy(), "退换货审批结果", r.getReturnNo() + " => " + r.getStatus(), "RETURN", r.getId());
    }

    @Transactional
    public void complete(Long id) {
        if (!SecurityUtils.hasAnyRole("WAREHOUSE", "ADMIN")) throw new BizException("需要仓管权限");
        ReturnOrder r = returnMapper.selectById(id);
        if (r == null) throw new BizException("单据不存在");
        if (!"APPROVED".equals(r.getStatus())) throw new BizException("需先审批通过");
        List<ReturnOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<ReturnOrderItem>().eq(ReturnOrderItem::getReturnId, id));
        if ("RETURN".equals(r.getType())) {
            for (ReturnOrderItem it : items) {
                Material mat = materialMapper.selectOne(new LambdaQueryWrapper<Material>().eq(Material::getName, it.getMaterialName()).last("LIMIT 1"));
                if (mat != null) {
                    Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>().eq(Inventory::getMaterialId, mat.getId()));
                    if (inv != null) {
                        BigDecimal q = inv.getQuantity().subtract(it.getQuantity());
                        inv.setQuantity(q.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : q);
                        inv.setUpdatedAt(LocalDateTime.now());
                        inventoryMapper.updateById(inv);
                    }
                }
            }
        }
        r.setStatus("COMPLETED");
        r.setUpdatedAt(LocalDateTime.now());
        returnMapper.updateById(r);
        operLogService.log("退换货", "完成", r.getReturnNo());
    }
}
