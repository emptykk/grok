package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
@Data
@TableName("purchase_request_item")
public class PurchaseRequestItem {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long requestId;
  private Long materialId;
  private String materialName;
  private String spec;
  private String unit;
  private BigDecimal quantity;
  private BigDecimal estimatePrice;
  private BigDecimal amount;
  private String remark;
}
