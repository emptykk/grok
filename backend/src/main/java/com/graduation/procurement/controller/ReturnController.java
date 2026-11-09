package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.ReturnOrder;
import com.graduation.procurement.service.ReturnService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name="退换货")
@RestController @RequestMapping("/api/returns") @RequiredArgsConstructor
public class ReturnController {
  private final ReturnService service;
  @GetMapping public Result<List<ReturnOrder>> list(){ return Result.ok(service.list()); }
  @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable Long id){ return Result.ok(service.detail(id)); }
  @PostMapping public Result<Long> create(@RequestBody ReturnService.CreateDTO dto){ return Result.ok(service.create(dto)); }
  @PostMapping("/{id}/approve") public Result<Void> approve(@PathVariable Long id,@RequestBody Map<String,String> body){
    service.approve(id, body.get("result"), body.get("comment")); return Result.ok(); }
  @PostMapping("/{id}/complete") public Result<Void> complete(@PathVariable Long id){ service.complete(id); return Result.ok(); }
}
