package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("knowledge_doc")
public class KnowledgeDoc {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String title;
  private String fileName;
  private String filePath;
  private Integer chunkCount;
  private String status;
  private Long uploaderId;
  private LocalDateTime createdAt;
}
