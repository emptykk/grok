package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
@Data
@TableName("inbound_order_item")
public class InboundOrderItem {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long inboundId;
  private Long orderItemId;
  private String materialName;
  private BigDecimal quantity;
}
