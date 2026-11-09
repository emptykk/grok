package com.graduation.procurement.controller;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.config.PublicDeployProperties;
import com.graduation.procurement.dto.LoginRequest;
import com.graduation.procurement.security.DemoGateFilter;
import com.graduation.procurement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  private final PublicDeployProperties publicDeploy;
  private final DemoGateFilter demoGateFilter;

  @Operation(summary = "登录")
  @PostMapping("/login")
  public Result<Map<String,Object>> login(@Valid @RequestBody LoginRequest req) {
    return Result.ok(authService.login(req.getUsername(), req.getPassword()));
  }

  @GetMapping("/bootstrap")
  public Result<Map<String, Object>> bootstrap() {
    return Result.ok(Map.of("publicMode", publicDeploy.isPublicMode()));
  }

  @PostMapping("/gate")
  public Result<Void> gate(@RequestBody Map<String, String> body, HttpServletRequest request, HttpServletResponse response) {
    if (!publicDeploy.isPublicMode()) return Result.ok();
    String given = body == null ? "" : String.valueOf(body.getOrDefault("secret", ""));
    if (!DemoGateFilter.constantEquals(publicDeploy.getDemoGateSecret(), given)) {
      response.setStatus(401);
      return Result.fail(401, "门禁未通过");
    }
    Cookie cookie = new Cookie(DemoGateFilter.COOKIE, demoGateFilter.token());
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(12 * 3600);
    boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    cookie.setSecure(secure);
    cookie.setAttribute("SameSite", "Lax");
    response.addCookie(cookie);
    return Result.ok();
  }

  @GetMapping("/me")
  public Result<Map<String,Object>> me() { return Result.ok(authService.me()); }

  @PutMapping("/password")
  public Result<Void> password(@RequestBody Map<String,String> body) {
    authService.changePassword(body.get("oldPassword"), body.get("newPassword"));
    return Result.ok();
  }
}
