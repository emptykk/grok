package com.graduation.procurement.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ApprovalDTO {
  @NotBlank private String result; // PASS / REJECT
  private String comment;
}
