package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("inquiry")
public class Inquiry {
  @TableId(type = IdType.AUTO) private Long id;
  private Long requestId; private String status; private Integer currentRound;
  private Long selectedSupplierId; private String aiRecommendJson;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
