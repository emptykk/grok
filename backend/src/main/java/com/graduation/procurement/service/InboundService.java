package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.common.RequestStatuses;
import com.graduation.procurement.dto.InboundDTO;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InboundService {
    private final InboundOrderMapper inboundMapper;
    private final InboundOrderItemMapper inboundItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final PurchaseRequestMapper requestMapper;
    private final MaterialMapper materialMapper;
    private final InventoryMapper inventoryMapper;
    private final OperLogService operLogService;
    private final NotifyService notifyService;

    @Transactional
    public Long inbound(InboundDTO dto) {
        if (!SecurityUtils.hasAnyRole("WAREHOUSE", "ADMIN")) throw new BizException("需要仓管权限");
        PurchaseOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) throw new BizException("订单不存在");
        if (!RequestStatuses.PENDING_RECEIVE.equals(order.getStatus()) && !"PARTIAL".equals(order.getStatus())) {
            throw new BizException("订单状态不允许入库");
        }
        InboundOrder ib = new InboundOrder();
        ib.setInboundNo(PurchaseRequestService.genNo("IN"));
        ib.setOrderId(order.getId());
        ib.setWarehouseUserId(SecurityUtils.currentUser().getId());
        ib.setRemark(dto.getRemark());
        ib.setDiffFlag(dto.getDiffFlag() == null ? 0 : dto.getDiffFlag());
        ib.setDiffRemark(dto.getDiffRemark());
        ib.setCreatedAt(LocalDateTime.now());
        inboundMapper.insert(ib);

        for (InboundDTO.Item it : dto.getItems()) {
            if (it.getQuantity() == null || it.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            PurchaseOrderItem oi = orderItemMapper.selectById(it.getOrderItemId());
            if (oi == null || !oi.getOrderId().equals(order.getId())) throw new BizException("订单明细无效");
            BigDecimal remain = oi.getQuantity().subtract(oi.getReceivedQty() == null ? BigDecimal.ZERO : oi.getReceivedQty());
            // 允许短装；超装标记差异
            if (it.getQuantity().compareTo(remain) > 0) {
                ib.setDiffFlag(1);
                String msg = oi.getMaterialName() + " 超装/差异，应收剩余 " + remain + " 实收 " + it.getQuantity();
                ib.setDiffRemark((ib.getDiffRemark() == null ? "" : ib.getDiffRemark() + ";") + msg);
                inboundMapper.updateById(ib);
            }
            oi.setReceivedQty((oi.getReceivedQty() == null ? BigDecimal.ZERO : oi.getReceivedQty()).add(it.getQuantity()));
            orderItemMapper.updateById(oi);

            InboundOrderItem ii = new InboundOrderItem();
            ii.setInboundId(ib.getId());
            ii.setOrderItemId(oi.getId());
            ii.setMaterialName(oi.getMaterialName());
            ii.setQuantity(it.getQuantity());
            inboundItemMapper.insert(ii);

            Material mat = materialMapper.selectOne(new LambdaQueryWrapper<Material>().eq(Material::getName, oi.getMaterialName()).last("LIMIT 1"));
            if (mat != null) {
                Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>().eq(Inventory::getMaterialId, mat.getId()));
                if (inv == null) {
                    inv = new Inventory();
                    inv.setMaterialId(mat.getId());
                    inv.setQuantity(it.getQuantity());
                    inv.setUpdatedAt(LocalDateTime.now());
                    inventoryMapper.insert(inv);
                } else {
                    inv.setQuantity(inv.getQuantity().add(it.getQuantity()));
                    inv.setUpdatedAt(LocalDateTime.now());
                    inventoryMapper.updateById(inv);
                }
            }
        }

        List<PurchaseOrderItem> all = orderItemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, order.getId()));
        boolean allDone = all.stream().allMatch(x -> x.getReceivedQty() != null && x.getReceivedQty().compareTo(x.getQuantity()) >= 0);
        boolean any = all.stream().anyMatch(x -> x.getReceivedQty() != null && x.getReceivedQty().compareTo(BigDecimal.ZERO) > 0);
        boolean shortRecv = all.stream().anyMatch(x -> x.getReceivedQty() != null && x.getReceivedQty().compareTo(x.getQuantity()) < 0);
        if (shortRecv && any) { ib.setDiffFlag(1); if (ib.getDiffRemark() == null || ib.getDiffRemark().isBlank()) ib.setDiffRemark("存在短装"); inboundMapper.updateById(ib); }
        order.setStatus(allDone ? RequestStatuses.COMPLETED : (any ? "PARTIAL" : RequestStatuses.PENDING_RECEIVE));
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        if (allDone) {
            PurchaseRequest req = requestMapper.selectById(order.getRequestId());
            req.setStatus(RequestStatuses.COMPLETED);
            req.setUpdatedAt(LocalDateTime.now());
            requestMapper.updateById(req);
        }
        if (ib.getDiffFlag() != null && ib.getDiffFlag() == 1) {
            notifyService.notifyUser(SecurityUtils.currentUser().getId(), "到货差异记录", ib.getInboundNo() + " " + ib.getDiffRemark(), "INBOUND", ib.getId());
        }
        operLogService.log("入库", "验收", ib.getInboundNo());
        return ib.getId();
    }

    public List<InboundOrder> list() {
        return inboundMapper.selectList(new LambdaQueryWrapper<InboundOrder>().orderByDesc(InboundOrder::getCreatedAt));
    }
}
