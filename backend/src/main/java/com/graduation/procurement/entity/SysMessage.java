package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("sys_message")
public class SysMessage {
  @TableId(type = IdType.AUTO) private Long id;
  private Long userId; private String title; private String content;
  private String bizType; private Long bizId; private Integer readFlag; private LocalDateTime createdAt;
}
