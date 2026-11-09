package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("inbound_order")
public class InboundOrder {
  @TableId(type = IdType.AUTO) private Long id;
  private String inboundNo; private Long orderId; private Long warehouseUserId;
  private Integer diffFlag; private String diffRemark; private String remark; private LocalDateTime createdAt;
}
