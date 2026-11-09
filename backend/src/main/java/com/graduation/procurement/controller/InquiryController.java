package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.dto.QuoteDTO;
import com.graduation.procurement.entity.Inquiry;
import com.graduation.procurement.service.InquiryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name = "询价")
@RestController @RequestMapping("/api/inquiries") @RequiredArgsConstructor
public class InquiryController {
  private final InquiryService service;
  @PostMapping public Result<Inquiry> create(@RequestBody Map<String,Long> body) { return Result.ok(service.create(body.get("requestId"))); }
  @GetMapping("/by-request/{requestId}") public Result<Map<String,Object>> byRequest(@PathVariable Long requestId) { return Result.ok(service.detailByRequest(requestId)); }
  @PostMapping("/{id}/quotes") public Result<Void> quote(@PathVariable Long id, @RequestBody QuoteDTO dto) { service.addQuote(id, dto); return Result.ok(); }
  @PostMapping("/{id}/select") public Result<Void> select(@PathVariable Long id, @RequestBody Map<String,Long> body) { service.selectSupplier(id, body.get("supplierId")); return Result.ok(); }
  @PostMapping("/{id}/recommend") public Result<Map<String,Object>> recommend(@PathVariable Long id) { return Result.ok(service.recommend(id)); }
  @PostMapping("/{id}/rounds") public Result<Void> nextRound(@PathVariable Long id, @RequestBody(required=false) Map<String,String> body) {
    service.startNextRound(id, body==null?null:body.get("remark")); return Result.ok(); }
  @GetMapping("/{id}/rounds") public Result<Map<String,Object>> rounds(@PathVariable Long id){ return Result.ok(service.rounds(id)); }
  @GetMapping("/{id}/compare-report") public Result<Map<String,Object>> compare(@PathVariable Long id){ return Result.ok(service.compareReport(id)); }
  @GetMapping("/{id}/compare-report/export") public ResponseEntity<byte[]> export(@PathVariable Long id){
    byte[] data = service.exportCompareExcel(id);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compare-"+id+".xlsx")
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
      .body(data);
  }
}
