package com.example.pptrefresh.llm;

import com.example.pptrefresh.config.PptRefreshProperties;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从若干页纯文本推断一行基金/产品展示名（无 tools）。仅在规则策略 {@code LLM_EXTRACT} 且 llm.enabled 时使用。
 */
@Component
public class LlmProductNameExtractor {

    private static final int MAX_CHARS = 6000;

    private final PptRefreshProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmProductNameExtractor(PptRefreshProperties properties, RestTemplate llmRestTemplate) {
        this.properties = properties;
        this.restTemplate = llmRestTemplate;
    }

    public String extractLine(String slidePlainText, String hint) {
        if (!properties.getLlm().isEnabled()) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "LLM_DISABLED",
                    "规则要求 LLM_EXTRACT 但未启用 ppt.refresh.llm.enabled",
                    null,
                    null);
        }
        String bodyText = slidePlainText;
        if (bodyText.length() > MAX_CHARS) {
            bodyText = bodyText.substring(0, MAX_CHARS);
        }
        String user =
                "下列文本来自 PPT 指定页面，请只输出一行：产品或基金的展示简称（不要代码、不要解释、不要引号）。\n"
                        + (hint != null && !hint.isBlank() ? "提示：" + hint + "\n\n" : "")
                        + bodyText;
        try {
            String url = normalizeBaseUrl(properties.getLlm().getBaseUrl()) + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getLlm().getApiKey());

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(
                    Map.of(
                            "role",
                            "system",
                            "content",
                            "你只输出一行中文或中英混合的短名称，不要其它任何字符。"));
            messages.add(Map.of("role", "user", "content", user));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getLlm().getModelName());
            body.put("temperature", 0);
            body.put("messages", messages);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url, new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
            JsonNode root = mapper.readTree(response.getBody());
            String content = root.get("choices").get(0).get("message").path("content").asText("");
            String line = content.trim().split("\\R", 2)[0].trim();
            if (line.isEmpty()) {
                throw new RefreshException(
                        FailureStage.PRODUCT_NAME_RESOLVE,
                        "LLM_EMPTY_NAME",
                        "LLM 未返回有效产品名",
                        null,
                        null);
            }
            return line;
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "LLM_NAME_HTTP_FAILED",
                    "产品名 LLM 调用失败: " + e.getMessage(),
                    null,
                    e);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String u = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (u.endsWith("/v1")) {
            return u;
        }
        return u + "/v1";
    }
}
