package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("material_template")
public class MaterialTemplate {
  @TableId(type = IdType.AUTO) private Long id;
  private Long userId; private String name; private Long materialId; private String materialName;
  private String spec; private String unit; private BigDecimal defaultQty; private BigDecimal defaultPrice;
  private Integer useCount; private LocalDateTime lastUsedAt; private LocalDateTime createdAt;
}
