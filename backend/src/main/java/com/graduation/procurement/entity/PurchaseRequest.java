package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("purchase_request")
public class PurchaseRequest {
  @TableId(type = IdType.AUTO) private Long id;
  private String requestNo; private String title; private Long applicantId; private String deptName;
  private BigDecimal budgetAmount; private Long budgetSubjectId; private Integer requiredLevels;
  private String status; private String remark; private String aiPrompt;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
