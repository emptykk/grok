package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("approval_rule")
public class ApprovalRule {
  @TableId(type = IdType.AUTO) private Long id;
  private String name; private BigDecimal minAmount; private BigDecimal maxAmount;
  private Integer levels; private Integer enabled; private String remark; private LocalDateTime createdAt;
}
