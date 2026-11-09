package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("email_send_log")
public class EmailSendLog {
  @TableId(type = IdType.AUTO) private Long id;
  private String toAddr; private String subject; private String content;
  private String status; private String relatedBiz; private LocalDateTime createdAt;
}
