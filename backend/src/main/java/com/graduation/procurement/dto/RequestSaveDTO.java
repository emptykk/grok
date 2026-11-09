package com.graduation.procurement.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class RequestSaveDTO {
  private Long id; private String title; private BigDecimal budgetAmount; private Long budgetSubjectId;
  private String remark; private String aiPrompt; private List<Item> items;
  @Data public static class Item {
    private Long materialId; private String materialName; private String spec; private String unit;
    private BigDecimal quantity; private BigDecimal estimatePrice; private String remark;
  }
}
