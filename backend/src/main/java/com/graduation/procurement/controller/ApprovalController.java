package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.dto.ApprovalDTO;
import com.graduation.procurement.entity.PurchaseRequest;
import com.graduation.procurement.service.ApprovalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "审批")
@RestController @RequestMapping("/api/approvals") @RequiredArgsConstructor
public class ApprovalController {
  private final ApprovalService service;
  @GetMapping("/todo") public Result<List<PurchaseRequest>> todo() { return Result.ok(service.todo()); }
  @GetMapping("/{requestId}/timeline") public Result<List<Map<String,Object>>> timeline(@PathVariable Long requestId) {
    return Result.ok(service.timeline(requestId));
  }
  @PostMapping("/{requestId}") public Result<Void> approve(@PathVariable Long requestId, @Valid @RequestBody ApprovalDTO dto) {
    service.approve(requestId, dto); return Result.ok();
  }
}
