package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long orderId;
  private String materialName;
  private String spec;
  private String unit;
  private BigDecimal quantity;
  private BigDecimal receivedQty;
  private BigDecimal unitPrice;
  private BigDecimal amount;
}
