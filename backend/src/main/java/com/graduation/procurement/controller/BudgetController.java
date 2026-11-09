package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.BudgetSubject;
import com.graduation.procurement.service.BudgetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name="预算科目")
@RestController @RequestMapping("/api/budgets") @RequiredArgsConstructor
public class BudgetController {
  private final BudgetService service;
  @GetMapping public Result<Page<BudgetSubject>> page(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,
      @RequestParam(required=false) Integer year,@RequestParam(required=false) String deptName,@RequestParam(required=false) String keyword) {
    return Result.ok(service.page(page,size,year,deptName,keyword));
  }
  @GetMapping("/available") public Result<List<BudgetSubject>> available(){ return Result.ok(service.available()); }
  @GetMapping("/export") public void export(HttpServletResponse response) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      XSSFSheet sheet = wb.createSheet("预算科目");
      Row h = sheet.createRow(0);
      String[] hs = {"编码","名称","部门","年度","总额","已用","剩余","状态"};
      for (int i=0;i<hs.length;i++) h.createCell(i).setCellValue(hs[i]);
      int i=1;
      for (BudgetSubject b: service.listAll()) {
        Row row = sheet.createRow(i++);
        double total = b.getTotalAmount()==null?0:b.getTotalAmount().doubleValue();
        double used = b.getUsedAmount()==null?0:b.getUsedAmount().doubleValue();
        row.createCell(0).setCellValue(b.getCode());
        row.createCell(1).setCellValue(b.getName());
        row.createCell(2).setCellValue(b.getDeptName());
        row.createCell(3).setCellValue(b.getBudgetYear()==null?0:b.getBudgetYear());
        row.createCell(4).setCellValue(total);
        row.createCell(5).setCellValue(used);
        row.createCell(6).setCellValue(Math.max(0, total-used));
        row.createCell(7).setCellValue(b.getStatus()!=null && b.getStatus()==0 ? "停用" : "启用");
      }
      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.setHeader("Content-Disposition", "attachment; filename="+ URLEncoder.encode("预算科目.xlsx", StandardCharsets.UTF_8));
      wb.write(response.getOutputStream());
    }
  }
  @GetMapping("/{id}") public Result<BudgetSubject> get(@PathVariable Long id){ return Result.ok(service.get(id)); }
  @PostMapping public Result<Long> create(@RequestBody BudgetSubject body){ return Result.ok(service.save(body)); }
  @PutMapping("/{id}") public Result<Long> update(@PathVariable Long id,@RequestBody BudgetSubject body){ body.setId(id); return Result.ok(service.save(body)); }
  @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id){ service.delete(id); return Result.ok(); }
}
