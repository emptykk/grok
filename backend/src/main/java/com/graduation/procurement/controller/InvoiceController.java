package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.Invoice;
import com.graduation.procurement.service.InvoicePaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name="发票")
@RestController @RequestMapping("/api/invoices") @RequiredArgsConstructor
public class InvoiceController {
  private final InvoicePaymentService service;
  @GetMapping public Result<List<Invoice>> list(){ return Result.ok(service.listInvoices()); }
  @PostMapping public Result<Long> create(@RequestBody Invoice body){ return Result.ok(service.createInvoice(body)); }
  @PutMapping("/{id}/match") public Result<Void> match(@PathVariable Long id,@RequestBody Map<String,String> body){
    service.matchInvoice(id, body.get("status"), body.get("remark")); return Result.ok(); }
}
