package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @TableName("supplier")
public class Supplier {
  @TableId(type = IdType.AUTO) private Long id;
  private String name; private String contact; private String phone; private String category;
  private Integer rating; private Integer status; private String remark;
  private BigDecimal admissionScore; private Integer blacklisted; private String blacklistReason;
  private LocalDate lastReviewDate; private LocalDate nextReviewDate;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
