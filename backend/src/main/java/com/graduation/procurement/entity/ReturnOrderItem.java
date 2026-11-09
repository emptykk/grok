package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
@Data @TableName("return_order_item")
public class ReturnOrderItem {
  @TableId(type = IdType.AUTO) private Long id;
  private Long returnId; private Long orderItemId; private String materialName;
  private BigDecimal quantity; private String remark;
}
