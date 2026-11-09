package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.dto.RequestSaveDTO;
import com.graduation.procurement.entity.PurchaseRequest;
import com.graduation.procurement.service.PurchaseRequestService;
import com.graduation.procurement.service.TraceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "采购申请")
@RestController @RequestMapping("/api/requests") @RequiredArgsConstructor
public class RequestController {
  private final PurchaseRequestService service;
  private final TraceService traceService;

  @GetMapping public Result<Page<PurchaseRequest>> page(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size,
      @RequestParam(required=false) String status, @RequestParam(required=false) String keyword,
      @RequestParam(required=false) Long budgetSubjectId) {
    return Result.ok(service.page(page,size,status,keyword,budgetSubjectId));
  }
  @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable Long id) { return Result.ok(service.detail(id)); }
  @GetMapping("/{id}/trace") public Result<Map<String,Object>> trace(@PathVariable Long id) { return Result.ok(traceService.trace(id)); }
  @PostMapping public Result<Long> create(@RequestBody RequestSaveDTO dto) { return Result.ok(service.save(dto)); }
  @PutMapping("/{id}") public Result<Long> update(@PathVariable Long id, @RequestBody RequestSaveDTO dto) { dto.setId(id); return Result.ok(service.save(dto)); }
  @PostMapping("/{id}/submit") public Result<Void> submit(@PathVariable Long id) { service.submit(id); return Result.ok(); }
  @PostMapping("/{id}/withdraw") public Result<Void> withdraw(@PathVariable Long id) { service.withdraw(id); return Result.ok(); }
  @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.ok(); }

  @GetMapping("/export")
  public void export(@RequestParam(required=false) String status, HttpServletResponse response) throws Exception {
    List<PurchaseRequest> list = service.listForExport(status);
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      XSSFSheet sheet = wb.createSheet("采购申请");
      Row h = sheet.createRow(0);
      h.createCell(0).setCellValue("单号"); h.createCell(1).setCellValue("标题"); h.createCell(2).setCellValue("部门");
      h.createCell(3).setCellValue("预算"); h.createCell(4).setCellValue("状态");
      int i=1;
      for (PurchaseRequest r: list) {
        Row row = sheet.createRow(i++);
        row.createCell(0).setCellValue(r.getRequestNo());
        row.createCell(1).setCellValue(r.getTitle());
        row.createCell(2).setCellValue(r.getDeptName());
        row.createCell(3).setCellValue(r.getBudgetAmount()==null?0:r.getBudgetAmount().doubleValue());
        row.createCell(4).setCellValue(r.getStatus());
      }
      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.setHeader("Content-Disposition", "attachment; filename="+ URLEncoder.encode("采购申请.xlsx", StandardCharsets.UTF_8));
      wb.write(response.getOutputStream());
    }
  }
}
