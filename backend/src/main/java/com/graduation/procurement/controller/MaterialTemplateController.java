package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.MaterialTemplate;
import com.graduation.procurement.service.MaterialTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name="物资模板")
@RestController @RequestMapping("/api/material-templates") @RequiredArgsConstructor
public class MaterialTemplateController {
  private final MaterialTemplateService service;
  @GetMapping public Result<List<MaterialTemplate>> mine(){ return Result.ok(service.mine()); }
  @PostMapping public Result<Long> save(@RequestBody MaterialTemplate t){ return Result.ok(service.save(t)); }
  @PostMapping("/{id}/use") public Result<MaterialTemplate> use(@PathVariable Long id){ return Result.ok(service.use(id)); }
  @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id){ service.delete(id); return Result.ok(); }
}
