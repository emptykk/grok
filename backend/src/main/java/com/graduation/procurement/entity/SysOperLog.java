package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("sys_oper_log")
public class SysOperLog {
  @TableId(type = IdType.AUTO) private Long id;
  private String module; private String action; private String username; private String ip; private String detail;
  private String method; private String requestUri; private Long costMs; private Integer success;
  private LocalDateTime createdAt;
}
