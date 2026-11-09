package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("attachment")
public class Attachment {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String bizType;
  private Long bizId;
  private String fileName;
  private String filePath;
  private Long size;
  private LocalDateTime createdAt;
}
