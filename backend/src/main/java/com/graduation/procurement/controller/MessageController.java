package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.EmailSendLog;
import com.graduation.procurement.entity.SysMessage;
import com.graduation.procurement.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name="消息通知")
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class MessageController {
  private final MessageService service;
  @GetMapping("/messages") public Result<Page<SysMessage>> page(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,
      @RequestParam(defaultValue="false") boolean onlyUnread){ return Result.ok(service.page(page,size,onlyUnread)); }
  @GetMapping("/messages/unread-count") public Result<Map<String,Object>> unread(){ return Result.ok(service.unreadCount()); }
  @PostMapping("/messages/{id}/read") public Result<Void> read(@PathVariable Long id){ service.read(id); return Result.ok(); }
  @PostMapping("/messages/read-all") public Result<Void> readAll(){ service.readAll(); return Result.ok(); }
  @GetMapping("/email-logs") public Result<Page<EmailSendLog>> emails(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){
    return Result.ok(service.emailLogs(page,size)); }
}
