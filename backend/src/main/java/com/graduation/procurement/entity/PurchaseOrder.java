package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@TableName("purchase_order")
public class PurchaseOrder {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String orderNo;
  private Long requestId;
  private Long inquiryId;
  private Long supplierId;
  private BigDecimal totalAmount;
  private String status;
  private LocalDate expectDate;
  private String remark;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
