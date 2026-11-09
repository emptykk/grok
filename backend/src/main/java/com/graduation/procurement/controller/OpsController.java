package com.graduation.procurement.controller;

import com.graduation.procurement.common.Result;
import com.graduation.procurement.service.OpsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "运行中心")
@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {
    private final OpsService opsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(opsService.overview());
    }
}
