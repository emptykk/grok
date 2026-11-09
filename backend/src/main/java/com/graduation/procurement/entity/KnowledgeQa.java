package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("knowledge_qa")
public class KnowledgeQa {
  @TableId(type = IdType.AUTO) private Long id;
  private Long userId; private String question; private String answer;
  private String referencesJson; private String highlightsJson; private LocalDateTime createdAt;
}
