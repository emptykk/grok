package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("return_order")
public class ReturnOrder {
  @TableId(type = IdType.AUTO) private Long id;
  private String returnNo; private Long orderId; private String type; private String status;
  private String reason; private Long createdBy; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
