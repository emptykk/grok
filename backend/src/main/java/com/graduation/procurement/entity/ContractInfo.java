package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @TableName("contract_info")
public class ContractInfo {
  @TableId(type = IdType.AUTO) private Long id;
  private String contractNo; private Long orderId; private Long supplierId; private String title;
  private BigDecimal amount; private LocalDate startDate; private LocalDate endDate; private String status;
  private String contentText; private String riskSummary; private String remark;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
