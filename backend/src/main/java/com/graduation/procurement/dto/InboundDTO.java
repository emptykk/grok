package com.graduation.procurement.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class InboundDTO {
  private Long orderId; private String remark; private Integer diffFlag; private String diffRemark;
  private List<Item> items;
  @Data public static class Item { private Long orderItemId; private BigDecimal quantity; }
}
