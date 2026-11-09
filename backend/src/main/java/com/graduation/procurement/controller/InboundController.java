package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.dto.InboundDTO;
import com.graduation.procurement.entity.InboundOrder;
import com.graduation.procurement.service.InboundService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "入库")
@RestController @RequestMapping("/api/inbounds") @RequiredArgsConstructor
public class InboundController {
  private final InboundService service;
  @GetMapping public Result<List<InboundOrder>> list() { return Result.ok(service.list()); }
  @PostMapping public Result<Long> create(@RequestBody InboundDTO dto) { return Result.ok(service.inbound(dto)); }
}
