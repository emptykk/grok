package com.graduation.procurement.controller;

import com.graduation.procurement.common.Result;
import com.graduation.procurement.dto.AiGenerateRequest;
import com.graduation.procurement.service.AiConfigService;
import com.graduation.procurement.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "智能助手")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;
    private final AiConfigService aiConfigService;

    @PostMapping("/generate-request")
    public Result<List<Map<String, Object>>> generate(@Valid @RequestBody AiGenerateRequest req) {
        return Result.ok(aiService.generateRequestItems(req.getPrompt()));
    }

    @PostMapping("/anomaly-analysis")
    public Result<Map<String, Object>> anomaly(@RequestBody(required = false) Map<String, Object> body) {
        Integer months = null;
        if (body != null && body.get("months") != null) months = Integer.valueOf(String.valueOf(body.get("months")));
        return Result.ok(aiService.anomalyAnalysis(months));
    }

    @Operation(summary = "智能填单助手")
    @PostMapping("/form-assist")
    public Result<Map<String, Object>> formAssist(@RequestBody Map<String, Object> body) {
        return Result.ok(aiService.formAssist(body == null ? Map.of() : body));
    }

    @Operation(summary = "审批助手")
    @PostMapping("/approval-assist")
    public Result<Map<String, Object>> approvalAssist(@RequestBody Map<String, Object> body) {
        Long requestId = body == null || body.get("requestId") == null ? null
                : Long.valueOf(String.valueOf(body.get("requestId")));
        return Result.ok(aiService.approvalAssist(requestId));
    }

    @Operation(summary = "比价智能结论")
    @PostMapping("/inquiry-verdict")
    public Result<Map<String, Object>> inquiryVerdict(@RequestBody Map<String, Object> body) {
        Long inquiryId = body == null || body.get("inquiryId") == null ? null
                : Long.valueOf(String.valueOf(body.get("inquiryId")));
        boolean persist = body != null && Boolean.parseBoolean(String.valueOf(body.getOrDefault("persist", true)));
        return Result.ok(aiService.inquiryVerdict(inquiryId, persist));
    }

    @Operation(summary = "管理周报一键生成")
    @PostMapping("/weekly-report")
    public Result<Map<String, Object>> weeklyReport(@RequestBody(required = false) Map<String, Object> body) {
        Integer days = null;
        if (body != null && body.get("days") != null) days = Integer.valueOf(String.valueOf(body.get("days")));
        return Result.ok(aiService.weeklyReport(days));
    }

    @Operation(summary = "查询大模型配置（密钥脱敏）")
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        return Result.ok(aiConfigService.getPublicConfig());
    }

    @Operation(summary = "保存大模型配置（管理员）")
    @PutMapping("/config")
    public Result<Map<String, Object>> saveConfig(@RequestBody Map<String, Object> body) {
        return Result.ok(aiConfigService.save(body == null ? Map.of() : body));
    }
}
