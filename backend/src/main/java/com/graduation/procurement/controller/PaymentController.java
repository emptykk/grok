package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.PaymentRequest;
import com.graduation.procurement.service.InvoicePaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name="付款申请")
@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
  private final InvoicePaymentService service;
  @GetMapping public Result<List<PaymentRequest>> list(){ return Result.ok(service.listPayments()); }
  @PostMapping public Result<Long> create(@RequestBody PaymentRequest body){ return Result.ok(service.createPayment(body)); }
  @PostMapping("/{id}/submit") public Result<Void> submit(@PathVariable Long id){ service.submitPayment(id); return Result.ok(); }
  @PostMapping("/{id}/approve") public Result<Void> approve(@PathVariable Long id,@RequestBody Map<String,String> body){
    service.approvePayment(id, body.get("result"), body.get("comment")); return Result.ok(); }
  @PostMapping("/{id}/mark-paid") public Result<Void> paid(@PathVariable Long id){ service.markPaid(id); return Result.ok(); }
}
