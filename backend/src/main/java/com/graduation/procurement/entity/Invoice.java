package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @TableName("invoice")
public class Invoice {
  @TableId(type = IdType.AUTO) private Long id;
  private String invoiceNo; private Long orderId; private Long supplierId;
  private BigDecimal amount; private BigDecimal taxAmount; private LocalDate invoiceDate;
  private String status; private String remark; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
