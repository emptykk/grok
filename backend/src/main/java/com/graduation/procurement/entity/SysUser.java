package com.graduation.procurement.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("sys_user")
public class SysUser {
  @TableId(type = IdType.AUTO) private Long id;
  private String username; private String passwordHash; private String realName;
  private String deptName; private String phone; private String email; private Integer status;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
