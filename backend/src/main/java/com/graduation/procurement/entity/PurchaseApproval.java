package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("purchase_approval")
public class PurchaseApproval {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long requestId;
  private String level;
  private Long approverId;
  private String result;
  private String comment;
  private LocalDateTime createdAt;
}
