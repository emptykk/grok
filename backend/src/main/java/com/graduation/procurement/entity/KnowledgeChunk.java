package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long docId;
  private Integer chunkIndex;
  private String content;
  private Integer tokenHint;
}
