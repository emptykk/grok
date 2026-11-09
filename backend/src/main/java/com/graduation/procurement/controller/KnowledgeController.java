package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.KnowledgeDoc;
import com.graduation.procurement.entity.KnowledgeQa;
import com.graduation.procurement.service.KnowledgeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Tag(name = "制度知识库")
@RestController @RequestMapping("/api/knowledge") @RequiredArgsConstructor
public class KnowledgeController {
  private final KnowledgeService service;
  @GetMapping("/docs") public Result<List<KnowledgeDoc>> docs() { return Result.ok(service.listDocs()); }
  @PostMapping("/upload") public Result<KnowledgeDoc> upload(@RequestParam("file") MultipartFile file, @RequestParam(required=false) String title) throws Exception {
    return Result.ok(service.upload(file, title));
  }
  @DeleteMapping("/docs/{id}") public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.ok(); }
  @PostMapping("/ask") public Result<Map<String,Object>> ask(@RequestBody Map<String,String> body) { return Result.ok(service.ask(body.get("question"))); }
  @GetMapping("/qa-history") public Result<Page<KnowledgeQa>> history(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){
    return Result.ok(service.qaHistory(page,size));
  }
  @GetMapping("/qa-history/{id}") public Result<KnowledgeQa> qa(@PathVariable Long id){ return Result.ok(service.qaDetail(id)); }
}
