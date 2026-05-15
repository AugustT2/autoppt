package com.example.pptrefresh.llm;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.write.TaskWritePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class WritePayloadParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public TaskWritePayload parse(TaskDefinition task, String raw) {
        try {
            String json = extractJson(raw);
            TaskWritePayload payload = mapper.readValue(json, TaskWritePayload.class);
            if (payload.getType() == null) {
                payload.setType(task.getType());
            }
            return payload;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.TASK_DTO_VALIDATE,
                    "JSON_PARSE",
                    "无法解析 LLM 写回 JSON: " + e.getMessage(),
                    task.getId(),
                    e);
        }
    }

    private static String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start + 1, end).trim();
            }
        }
        int brace = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (brace >= 0 && last > brace) {
            return trimmed.substring(brace, last + 1);
        }
        return trimmed;
    }
}
