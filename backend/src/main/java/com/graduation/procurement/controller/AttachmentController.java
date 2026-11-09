package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.Attachment;
import com.graduation.procurement.service.AttachmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Tag(name = "附件")
@RestController @RequestMapping("/api/attachments") @RequiredArgsConstructor
public class AttachmentController {
  private final AttachmentService service;
  @PostMapping("/upload") public Result<Attachment> upload(@RequestParam String bizType, @RequestParam Long bizId, @RequestParam("file") MultipartFile file) throws Exception {
    return Result.ok(service.upload(bizType, bizId, file));
  }
  @GetMapping public Result<List<Attachment>> list(@RequestParam String bizType, @RequestParam Long bizId) {
    return Result.ok(service.list(bizType, bizId));
  }
}
