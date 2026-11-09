package com.graduation.procurement.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeepSeekClient {
    private final AiConfigService aiConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public boolean available() {
        return aiConfigService.available();
    }

    public String chat(String system, String user) {
        if (!available()) {
            throw new BizException(1001, "未配置 DeepSeek API Key（请管理员在「大模型配置」中填写，或设置环境变量 DEEPSEEK_API_KEY）。业务主流程仍可手工操作。");
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiConfigService.model());
            body.put("temperature", 0.2);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)
            ));
            String json = objectMapper.writeValueAsString(body);
            String base = aiConfigService.baseUrl().replaceAll("/$", "");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(aiConfigService.timeoutSeconds()))
                    .header("Authorization", "Bearer " + aiConfigService.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new BizException(1001, "DeepSeek 调用失败 HTTP " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(1001, "DeepSeek 调用异常: " + e.getMessage());
        }
    }
}
