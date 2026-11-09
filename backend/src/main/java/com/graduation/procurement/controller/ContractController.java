package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.ContractInfo;
import com.graduation.procurement.service.ContractService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name="合同")
@RestController @RequestMapping("/api/contracts") @RequiredArgsConstructor
public class ContractController {
  private final ContractService service;
  @GetMapping public Result<Page<ContractInfo>> page(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,
      @RequestParam(required=false) String status,@RequestParam(required=false) String keyword){ return Result.ok(service.page(page,size,status,keyword)); }
  @GetMapping("/expiring") public Result<List<ContractInfo>> expiring(@RequestParam(defaultValue="30") int days){ return Result.ok(service.expiring(days)); }
  @GetMapping("/{id}") public Result<ContractInfo> get(@PathVariable Long id){ return Result.ok(service.get(id)); }
  @PostMapping public Result<Long> create(@RequestBody ContractInfo body){ return Result.ok(service.create(body)); }
  @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id,@RequestBody ContractInfo body){ service.update(id,body); return Result.ok(); }
  @PostMapping("/{id}/activate") public Result<Void> activate(@PathVariable Long id){ service.activate(id); return Result.ok(); }
  @PostMapping("/{id}/risk-summary") public Result<Map<String,String>> risk(@PathVariable Long id){ return Result.ok(Map.of("summary", service.riskSummary(id))); }
}
