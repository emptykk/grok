package com.graduation.procurement.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.entity.SysOperLog;
import com.graduation.procurement.entity.SysRole;
import com.graduation.procurement.entity.SysUser;
import com.graduation.procurement.service.MasterDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "系统管理")
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class SystemController {
  private final MasterDataService service;
  @GetMapping("/users") public Result<Page<SysUser>> users(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size) {
    return Result.ok(service.pageUsers(page,size));
  }
  @GetMapping("/roles") public Result<List<SysRole>> roles() { return Result.ok(service.roles()); }
  @PutMapping("/roles/{id}/data-scope") public Result<Void> dataScope(@PathVariable Long id, @RequestBody Map<String,String> body){
    service.updateRoleDataScope(id, body.get("dataScope")); return Result.ok();
  }
  @GetMapping("/logs") public Result<Page<SysOperLog>> logs(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size,
      @RequestParam(required=false) String username, @RequestParam(required=false) String module,
      @RequestParam(required=false) String action, @RequestParam(required=false) String keyword,
      @RequestParam(required=false) Integer success) {
    return Result.ok(service.logs(page,size,username,module,action,keyword,success));
  }
  @GetMapping("/logs/export") public void exportLogs(@RequestParam(required=false) String username, @RequestParam(required=false) String module,
      @RequestParam(required=false) String action, @RequestParam(required=false) String keyword,
      @RequestParam(required=false) Integer success, HttpServletResponse response) throws Exception {
    List<SysOperLog> list = service.logsForExport(username, module, action, keyword, success);
    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/csv;charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=oper-logs.csv");
    response.getOutputStream().write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
    PrintWriter w = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
    w.println("时间,用户,模块,动作,详情,IP,方法,路径,耗时ms,结果");
    for (SysOperLog l : list) {
      w.println(String.join(",",
          csv(l.getCreatedAt()), csv(l.getUsername()), csv(l.getModule()), csv(l.getAction()), csv(l.getDetail()),
          csv(l.getIp()), csv(l.getMethod()), csv(l.getRequestUri()), csv(l.getCostMs()),
          l.getSuccess()!=null && l.getSuccess()==0 ? "失败" : "成功"));
    }
    w.flush();
  }
  @GetMapping("/logs/{id}") public Result<SysOperLog> logDetail(@PathVariable Long id) { return Result.ok(service.logDetail(id)); }

  private String csv(Object v) {
    String s = v == null ? "" : String.valueOf(v).replace("\"", "\"\"");
    return "\"" + s + "\"";
  }
}
