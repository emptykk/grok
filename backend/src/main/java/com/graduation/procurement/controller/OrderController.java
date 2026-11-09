package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.PurchaseOrder;
import com.graduation.procurement.service.ExcelIoService;
import com.graduation.procurement.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "订单")
@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
  private final OrderService service;
  private final ExcelIoService excelIoService;
  @GetMapping public Result<Page<PurchaseOrder>> page(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String status) {
    return Result.ok(service.page(page,size,status));
  }
  @GetMapping("/export") public ResponseEntity<byte[]> export(){
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=orders.xlsx")
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
      .body(excelIoService.exportOrders());
  }
  @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable Long id) { return Result.ok(service.detail(id)); }
  @PostMapping public Result<Long> create(@RequestBody Map<String,Object> body) {
    Long inquiryId = Long.valueOf(body.get("inquiryId").toString());
    LocalDate expect = body.get("expectDate") == null ? null : LocalDate.parse(body.get("expectDate").toString());
    String remark = body.get("remark") == null ? null : body.get("remark").toString();
    return Result.ok(service.createFromInquiry(inquiryId, expect, remark));
  }
}
