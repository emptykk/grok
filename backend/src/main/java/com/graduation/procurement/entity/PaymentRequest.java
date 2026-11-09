package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("payment_request")
public class PaymentRequest {
  @TableId(type = IdType.AUTO) private Long id;
  private String paymentNo; private Long orderId; private Long invoiceId; private BigDecimal amount;
  private String status; private Long applicantId; private String remark;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
