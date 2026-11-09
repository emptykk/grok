package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("supplier_score_record")
public class SupplierScoreRecord {
  @TableId(type = IdType.AUTO) private Long id;
  private Long supplierId; private BigDecimal score; private String dimension;
  private String remark; private Long reviewerId; private LocalDateTime createdAt;
}
