package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name = "看板")
@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor
public class DashboardController {
  private final DashboardService service;
  @GetMapping("/summary") public Result<Map<String,Object>> summary() { return Result.ok(service.summary()); }
  @GetMapping("/process") public Result<Map<String,Object>> process() { return Result.ok(service.processBoard()); }
}
