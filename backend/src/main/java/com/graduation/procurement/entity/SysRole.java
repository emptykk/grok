package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
@Data @TableName("sys_role")
public class SysRole {
  @TableId(type = IdType.AUTO) private Long id;
  private String code; private String name; private String dataScope;
}
