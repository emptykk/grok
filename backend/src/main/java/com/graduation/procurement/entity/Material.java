package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("material")
public class Material {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String code;
  private String name;
  private String spec;
  private String unit;
  private String category;
  private BigDecimal safeStock;
  private String remark;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
