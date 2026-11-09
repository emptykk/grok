package com.graduation.procurement.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class AiGenerateRequest {
  @NotBlank private String prompt;
}
