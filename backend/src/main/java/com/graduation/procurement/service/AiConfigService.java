package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.ai.DeepSeekProperties;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.SysConfig;
import com.graduation.procurement.mapper.SysConfigMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiConfigService {
    public static final String KEY_API = "deepseek.api-key";
    public static final String KEY_BASE = "deepseek.base-url";
    public static final String KEY_MODEL = "deepseek.model";

    private final SysConfigMapper configMapper;
    private final DeepSeekProperties props;
    private final OperLogService operLogService;

    public String getValue(String key, String defaultVal) {
        SysConfig c = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (c != null && c.getConfigValue() != null && !c.getConfigValue().isBlank()) {
            return c.getConfigValue().trim();
        }
        return defaultVal == null ? "" : defaultVal;
    }

    public String apiKey() { return getValue(KEY_API, props.getApiKey()); }
    public String baseUrl() { return getValue(KEY_BASE, props.getBaseUrl()); }
    public String model() { return getValue(KEY_MODEL, props.getModel()); }
    public int timeoutSeconds() { return props.getTimeoutSeconds() <= 0 ? 60 : props.getTimeoutSeconds(); }

    public boolean available() {
        String k = apiKey();
        return k != null && !k.isBlank();
    }

    public Map<String, Object> getPublicConfig() {
        String key = apiKey();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("baseUrl", baseUrl());
        m.put("model", model());
        m.put("configured", available());
        m.put("apiKeyMasked", mask(key));
        m.put("apiKeySet", available());
        m.put("source", dbHas(KEY_API) || dbHas(KEY_BASE) || dbHas(KEY_MODEL) ? "database" : "env-or-default");
        return m;
    }

    private boolean dbHas(String key) {
        SysConfig c = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        return c != null && c.getConfigValue() != null && !c.getConfigValue().isBlank();
    }

    private String mask(String key) {
        if (key == null || key.isBlank()) return "";
        if (key.length() <= 4) return "****";
        return "****" + key.substring(key.length() - 4);
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        if (!SecurityUtils.hasRole("ADMIN")) throw new BizException("仅管理员可配置大模型");
        String baseUrl = str(body.get("baseUrl"));
        String model = str(body.get("model"));
        String apiKey = str(body.get("apiKey"));
        if (baseUrl != null && !baseUrl.isBlank()) upsert(KEY_BASE, baseUrl.replaceAll("/$", ""), "DeepSeek Base URL");
        if (model != null && !model.isBlank()) upsert(KEY_MODEL, model, "DeepSeek Model");
        if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("*")) {
            upsert(KEY_API, apiKey, "DeepSeek API Key");
        }
        operLogService.log("系统配置", "更新大模型配置", "baseUrl/model/apiKey");
        return getPublicConfig();
    }

    private void upsert(String key, String value, String remark) {
        SysConfig c = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (c == null) {
            c = new SysConfig();
            c.setConfigKey(key);
            c.setConfigValue(value);
            c.setRemark(remark);
            c.setUpdatedAt(LocalDateTime.now());
            configMapper.insert(c);
        } else {
            c.setConfigValue(value);
            c.setRemark(remark);
            c.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(c);
        }
    }

    private String str(Object o) { return o == null ? null : String.valueOf(o).trim(); }
}
