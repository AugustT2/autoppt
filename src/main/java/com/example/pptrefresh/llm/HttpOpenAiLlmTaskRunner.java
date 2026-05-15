package com.example.pptrefresh.llm;

import com.example.pptrefresh.config.PptRefreshProperties;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.tools.DemoToolExecutor;
import com.example.pptrefresh.write.TaskWritePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat Completions（含 tools 多轮）。不依赖 LangChain4j，可在 JDK 15 运行；
 * 安装 JDK 17+ 后可再封装为 LangChain4j 实现。
 */
public class HttpOpenAiLlmTaskRunner implements LlmTaskRunner {

    private static final int MAX_TOOL_ROUNDS = 8;

    private final PptRefreshProperties properties;
    private final WritePayloadParser parser;
    private final DemoToolExecutor toolExecutor;
    private final RestTemplate restTemplate;
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpOpenAiLlmTaskRunner(
            PptRefreshProperties properties,
            WritePayloadParser parser,
            DemoToolExecutor toolExecutor,
            RestTemplate restTemplate) {
        this.properties = properties;
        this.parser = parser;
        this.toolExecutor = toolExecutor;
        this.restTemplate = restTemplate;
    }

    @Override
    public TaskWritePayload fetch(TaskContext context) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", PromptBuilder.SYSTEM));
            messages.add(
                    Map.of(
                            "role",
                            "user",
                            "content",
                            promptBuilder.buildUserMessage(
                                    context.deckType(),
                                    context.productDisplayName(),
                                    context.fundCode(),
                                    context.timeContext(),
                                    context.task(),
                                    context.structure())));

            String url = normalizeBaseUrl(properties.getLlm().getBaseUrl()) + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getLlm().getApiKey());

            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", properties.getLlm().getModelName());
                body.put("messages", messages);
                body.put("tools", toolExecutor.toolDefinitions());

                ResponseEntity<String> response =
                        restTemplate.postForEntity(
                                url, new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode choice = root.get("choices").get(0);
                JsonNode message = choice.get("message");
                String finish = choice.path("finish_reason").asText("");

                JsonNode toolCalls = message.get("tool_calls");
                if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                    messages.add(mapper.convertValue(message, Map.class));
                    for (JsonNode call : toolCalls) {
                        String id = call.get("id").asText();
                        String name = call.get("function").get("name").asText();
                        String args = call.get("function").get("arguments").asText();
                        String result = toolExecutor.execute(name, args);
                        messages.add(
                                Map.of(
                                        "role",
                                        "tool",
                                        "tool_call_id",
                                        id,
                                        "content",
                                        result));
                    }
                    continue;
                }

                String content = message.path("content").asText("");
                if ("tool_calls".equals(finish) && (content == null || content.isBlank())) {
                    continue;
                }
                return parser.parse(context.task(), content);
            }
            throw new RefreshException(
                    FailureStage.TASK_LLM,
                    "TOOL_LOOP_LIMIT",
                    "Tool 调用轮次超过上限",
                    context.task().getId(),
                    null);
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.TASK_LLM,
                    "LLM_HTTP_FAILED",
                    "LLM HTTP 调用失败: " + e.getMessage(),
                    context.task().getId(),
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
