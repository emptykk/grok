package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.Supplier;
import com.graduation.procurement.entity.SupplierScoreRecord;
import com.graduation.procurement.service.ExcelIoService;
import com.graduation.procurement.service.MasterDataService;
import com.graduation.procurement.service.SupplierExtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "供应商")
@RestController @RequestMapping("/api/suppliers") @RequiredArgsConstructor
public class SupplierController {
  private final MasterDataService service;
  private final SupplierExtService extService;
  private final ExcelIoService excelIoService;

  @GetMapping public Result<Page<Supplier>> page(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String keyword) {
    return Result.ok(service.pageSuppliers(page,size,keyword));
  }
  @PostMapping public Result<Void> create(@RequestBody Supplier s) { service.saveSupplier(s); return Result.ok(); }
  @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id, @RequestBody Supplier s) { s.setId(id); service.saveSupplier(s); return Result.ok(); }
  @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { service.deleteSupplier(id); return Result.ok(); }
  @GetMapping("/export") public ResponseEntity<byte[]> export(){
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=suppliers.xlsx")
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
      .body(excelIoService.exportSuppliers());
  }
  @GetMapping("/review-due") public Result<List<Supplier>> reviewDue(){ return Result.ok(extService.reviewDue()); }
  @PostMapping("/{id}/score") public Result<Void> score(@PathVariable Long id, @RequestBody Map<String,Object> body){
    BigDecimal score = new BigDecimal(String.valueOf(body.get("score")));
    extService.score(id, score, body.get("dimension")==null?null:String.valueOf(body.get("dimension")), body.get("remark")==null?null:String.valueOf(body.get("remark")));
    return Result.ok();
  }
  @GetMapping("/{id}/profile") public Result<Map<String,Object>> profile(@PathVariable Long id){ return Result.ok(extService.profile(id)); }
  @GetMapping("/{id}/scores") public Result<List<SupplierScoreRecord>> scores(@PathVariable Long id){ return Result.ok(extService.scores(id)); }
  @PostMapping("/{id}/blacklist") public Result<Void> blacklist(@PathVariable Long id, @RequestBody Map<String,Object> body){
    boolean flag = Boolean.parseBoolean(String.valueOf(body.getOrDefault("blacklisted", false)));
    extService.blacklist(id, flag, body.get("reason")==null?null:String.valueOf(body.get("reason")));
    return Result.ok();
  }
  @PostMapping("/{id}/review") public Result<Void> review(@PathVariable Long id, @RequestBody Map<String,Object> body){
    LocalDate next = body.get("nextReviewDate")==null||String.valueOf(body.get("nextReviewDate")).isBlank()? null : LocalDate.parse(String.valueOf(body.get("nextReviewDate")));
    extService.review(id, String.valueOf(body.get("result")), next, body.get("remark")==null?null:String.valueOf(body.get("remark")));
    return Result.ok();
  }
}
