package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("budget_subject")
public class BudgetSubject {
  @TableId(type = IdType.AUTO) private Long id;
  private String code; private String name; private String deptName; private Integer budgetYear;
  private BigDecimal totalAmount; private BigDecimal usedAmount; private String remark; private Integer status;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
