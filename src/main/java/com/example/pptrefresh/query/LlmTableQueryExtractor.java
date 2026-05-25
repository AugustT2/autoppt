package com.example.pptrefresh.query;

import com.example.pptrefresh.config.PptRefreshProperties;
import com.example.pptrefresh.llm.PromptCatalog;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 从表格矩阵识别区间、指标与可选 intervalAxis；区间轴以矩阵定位为准，日期由 Java 处理。
 */
@Component
public class LlmTableQueryExtractor {

    private static final Logger log = LoggerFactory.getLogger(LlmTableQueryExtractor.class);

    private static final int MAX_MATRIX_CELLS = 500;

    private final PptRefreshProperties properties;
    private final PromptCatalog promptCatalog;
    private final ChatModel chatModel;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmTableQueryExtractor(
            PptRefreshProperties properties,
            PromptCatalog promptCatalog,
            @Autowired(required = false) ChatModel chatModel) {
        this.properties = properties;
        this.promptCatalog = promptCatalog;
        this.chatModel = chatModel;
    }

    public boolean isAvailable() {
        return properties.getLlm().isEnabled() && chatModel != null;
    }

    public TableQueryIntent infer(
            List<List<String>> matrix, String taskIntent, IntervalLexicon lexicon) {
        return infer(matrix, taskIntent, null, lexicon);
    }

    public TableQueryIntent infer(
            List<List<String>> matrix,
            String taskIntent,
            String taskHints,
            IntervalLexicon lexicon) {
        if (!isAvailable()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "LLM_DISABLED",
                    "表格查询意图识别需要 ppt.refresh.llm.enabled=true",
                    null,
                    null);
        }
        try {
            ChatResponse response =
                    chatModel.chat(
                            SystemMessage.from(promptCatalog.tableQuerySystem()),
                            UserMessage.from(buildUserMessage(matrix, taskIntent, taskHints, lexicon)));
            String raw = response.aiMessage().text();
            log.debug("表格查询 LLM 原始响应: {}", raw);
            TableQueryIntent intent = parseResponse(raw);
            log.info(
                    "表格查询 LLM 解析结果 intervalLabels={} metrics={} axis={}",
                    intent.intervalLabels(),
                    intent.metrics(),
                    intent.intervalAxis().orElse(null));
            return intent;
        } catch (RefreshException e) {
            log.warn("表格查询 LLM 业务失败: {} {}", e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("表格查询 LLM 调用异常", e);
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "TABLE_QUERY_LLM_FAILED",
                    "表格查询意图 LLM 识别失败: " + e.getMessage(),
                    null,
                    e);
        }
    }

    private String buildUserMessage(
            List<List<String>> matrix,
            String taskIntent,
            String taskHints,
            IntervalLexicon lexicon) {
        List<List<String>> trimmed = trimMatrix(matrix);
        StringBuilder sb = new StringBuilder();
        sb.append("任务：").append(taskIntent == null ? "刷新表格数据" : taskIntent).append('\n');
        if (taskHints != null && !taskHints.isBlank()) {
            sb.append("补充：").append(taskHints.trim()).append('\n');
        }
        sb.append("区间语义词表（仅帮助理解含义；intervalLabels 仍须从矩阵逐字复制，优先用表中已有文案）：")
                .append(String.join("、", lexicon.labels().keySet()))
                .append('\n');
        sb.append("表格矩阵（行优先）：\n");
        try {
            sb.append(mapper.writeValueAsString(trimmed));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return sb.toString();
    }

    private static List<List<String>> trimMatrix(List<List<String>> matrix) {
        int rows = matrix.size();
        int cols = rows == 0 ? 0 : matrix.get(0).size();
        if (rows * cols > MAX_MATRIX_CELLS) {
            rows = Math.min(rows, MAX_MATRIX_CELLS / Math.max(cols, 1));
        }
        List<List<String>> out = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            out.add(new ArrayList<>(matrix.get(r)));
        }
        return out;
    }

    private TableQueryIntent parseResponse(String raw) throws Exception {
        String json = extractJson(raw);
        TableQueryLlmResponse dto = mapper.readValue(json, TableQueryLlmResponse.class);
        List<String> labels = cleanList(dto.getIntervalLabels());
        List<String> metrics = cleanList(dto.getMetrics());
        if (labels.isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "TABLE_QUERY_EMPTY",
                    "LLM 未返回 intervalLabels",
                    null,
                    null);
        }
        if (metrics.isEmpty()) {
            throw new RefreshException(
                    FailureStage.DIMENSION_EXTRACT,
                    "TABLE_QUERY_EMPTY",
                    "LLM 未返回 metrics",
                    null,
                    null);
        }
        Optional<TableLabelAxis> axis = parseOptionalAxis(dto.getIntervalAxis());
        return new TableQueryIntent(labels, metrics, axis);
    }

    private static Optional<TableLabelAxis> parseOptionalAxis(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(TableLabelAxis.fromYaml(raw));
    }

    private static List<String> cleanList(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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
