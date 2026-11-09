package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.security.SecurityUtils;
import com.graduation.procurement.service.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name="定时任务")
@RestController @RequestMapping("/api/jobs") @RequiredArgsConstructor
public class JobController {
  private final JobService jobService;
  @PostMapping("/inventory-alert") public Result<Map<String,Object>> inv(){
    if (!SecurityUtils.hasAnyRole("ADMIN","WAREHOUSE")) throw new com.graduation.procurement.common.BizException("无权限");
    return Result.ok(Map.of("alerts", jobService.runInventoryAlert()));
  }
  @PostMapping("/contract-expiry") public Result<Map<String,Object>> ct(){
    if (!SecurityUtils.hasAnyRole("ADMIN","PURCHASE_MANAGER","FINANCE")) throw new com.graduation.procurement.common.BizException("无权限");
    return Result.ok(Map.of("count", jobService.runContractExpiry()));
  }
}
