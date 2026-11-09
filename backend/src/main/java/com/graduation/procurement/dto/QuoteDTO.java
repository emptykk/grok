package com.graduation.procurement.dto;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class QuoteDTO {
  private Long supplierId; private BigDecimal unitPrice; private BigDecimal totalPrice;
  private Integer deliveryDays; private String remark; private Integer roundNo; private String negotiateNote;
}
