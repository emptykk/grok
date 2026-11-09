package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("inventory")
public class Inventory {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long materialId;
  private BigDecimal quantity;
  private LocalDateTime updatedAt;
}
