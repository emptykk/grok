package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.service.MasterDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "库存")
@RestController @RequestMapping("/api/inventories") @RequiredArgsConstructor
public class InventoryController {
  private final MasterDataService service;
  @GetMapping public Result<List<Map<String,Object>>> list() { return Result.ok(service.inventories()); }
  @GetMapping("/alerts") public Result<List<Map<String,Object>>> alerts() {
    return Result.ok(service.inventories().stream().filter(x -> Boolean.TRUE.equals(x.get("alert"))).collect(Collectors.toList()));
  }
}
