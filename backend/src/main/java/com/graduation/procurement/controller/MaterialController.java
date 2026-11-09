package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.Material;
import com.graduation.procurement.service.ExcelIoService;
import com.graduation.procurement.service.MasterDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Tag(name = "物资")
@RestController @RequestMapping("/api/materials") @RequiredArgsConstructor
public class MaterialController {
  private final MasterDataService service;
  private final ExcelIoService excelIoService;
  @GetMapping public Result<Page<Material>> page(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String keyword) {
    return Result.ok(service.pageMaterials(page,size,keyword));
  }
  @PostMapping public Result<Void> create(@RequestBody Material m) { service.saveMaterial(m); return Result.ok(); }
  @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id, @RequestBody Material m) { m.setId(id); service.saveMaterial(m); return Result.ok(); }
  @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { service.deleteMaterial(id); return Result.ok(); }
  @GetMapping("/export") public ResponseEntity<byte[]> export(){
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=materials.xlsx")
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
      .body(excelIoService.exportMaterials());
  }
  @PostMapping("/import") public Result<Map<String,Object>> importExcel(@RequestParam("file") MultipartFile file){
    return Result.ok(Map.of("count", excelIoService.importMaterials(file)));
  }
}
