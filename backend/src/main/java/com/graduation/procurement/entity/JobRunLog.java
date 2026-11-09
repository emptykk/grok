package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_run_log")
public class JobRunLog {
  @TableId(type = IdType.AUTO) private Long id;
  private String jobCode;
  private String jobName;
  private String triggerType;
  private String status;
  private String summary;
  private String detail;
  private String operatorName;
  private Integer durationMs;
  private LocalDateTime createdAt;
}
