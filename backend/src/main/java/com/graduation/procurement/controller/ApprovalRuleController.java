package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.ApprovalRule;
import com.graduation.procurement.service.ApprovalRuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name="审批规则")
@RestController @RequestMapping("/api/approval-rules") @RequiredArgsConstructor
public class ApprovalRuleController {
  private final ApprovalRuleService service;
  @GetMapping public Result<List<ApprovalRule>> list(){ return Result.ok(service.list()); }
  @GetMapping("/resolve") public Result<Map<String,Object>> resolve(@RequestParam BigDecimal amount){ return Result.ok(service.resolve(amount)); }
  @PostMapping public Result<Long> create(@RequestBody ApprovalRule body){ return Result.ok(service.save(body)); }
  @PutMapping("/{id}") public Result<Long> update(@PathVariable Long id,@RequestBody ApprovalRule body){ body.setId(id); return Result.ok(service.save(body)); }
  @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id){ service.delete(id); return Result.ok(); }
}
