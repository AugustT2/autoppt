package com.example.pptrefresh.llm;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import com.example.pptrefresh.write.TaskWritePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WritePayloadParser {

    private static final Logger log = LoggerFactory.getLogger(WritePayloadParser.class);

    private final ObjectMapper mapper = new ObjectMapper();

    public TaskWritePayload parse(TaskDefinition task, String raw) {
        try {
            String json = extractJson(raw);
            TaskWritePayload payload = mapper.readValue(json, TaskWritePayload.class);
            normalizePayload(payload, task);
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

    /**
     * 模型常把 chart/table 写回塞进 {@code text} 字符串（如 {@code {"text":"{\"type\":\"chart\",...}"}}），
     * 需展开内层 JSON。
     */
    private void normalizePayload(TaskWritePayload payload, TaskDefinition task) {
        unwrapJsonEmbeddedInText(payload);
        if (payload.getType() == null) {
            payload.setType(task.getType());
        }
        if (task.getType() == TaskType.chart && hasChartFields(payload)) {
            payload.setType(TaskType.chart);
            if (isJsonLike(payload.getText())) {
                payload.setText(null);
            }
        }
        if (task.getType() == TaskType.table && payload.getCells() != null && isJsonLike(payload.getText())) {
            payload.setType(TaskType.table);
            payload.setText(null);
        }
    }

    private void unwrapJsonEmbeddedInText(TaskWritePayload payload) {
        String text = payload.getText();
        if (!StringUtils.hasText(text) || !text.trim().startsWith("{")) {
            return;
        }
        try {
            TaskWritePayload inner = mapper.readValue(text.trim(), TaskWritePayload.class);
            mergeInto(payload, inner);
            log.debug(
                    "已展开 text 内嵌写回 JSON: type={} chart={} table={}",
                    inner.getType(),
                    hasChartFields(inner),
                    inner.getCells() != null);
        } catch (Exception ignored) {
            // text 为普通文案，非 JSON
        }
    }

    private static void mergeInto(TaskWritePayload target, TaskWritePayload source) {
        if (source.getType() != null) {
            target.setType(source.getType());
        }
        if (source.getText() != null && !isJsonLike(source.getText())) {
            target.setText(source.getText());
        }
        if (source.getCells() != null) {
            target.setCells(source.getCells());
        }
        if (source.getCategories() != null) {
            target.setCategories(source.getCategories());
        }
        if (source.getSeriesNames() != null) {
            target.setSeriesNames(source.getSeriesNames());
        }
        if (source.getSeriesValues() != null) {
            target.setSeriesValues(source.getSeriesValues());
        }
    }

    private static boolean hasChartFields(TaskWritePayload payload) {
        return payload.getCategories() != null
                && payload.getSeriesNames() != null
                && payload.getSeriesValues() != null;
    }

    private static boolean isJsonLike(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String t = text.trim();
        return t.startsWith("{") && t.endsWith("}");
    }
}
