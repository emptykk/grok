package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("inquiry_quote")
public class InquiryQuote {
  @TableId(type = IdType.AUTO) private Long id;
  private Long inquiryId; private Integer roundNo; private Long supplierId;
  private BigDecimal unitPrice; private BigDecimal totalPrice; private Integer deliveryDays;
  private String remark; private String negotiateNote; private LocalDateTime createdAt;
}
