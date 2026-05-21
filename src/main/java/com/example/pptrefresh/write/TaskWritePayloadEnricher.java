package com.example.pptrefresh.write;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.llm.TaskContext;
import com.example.pptrefresh.query.ChartSeriesData;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.QueryPlanDataService;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TaskType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * LLM 最终写回 JSON 常不完整；优先从同轮 Tool 结果合并，否则按 {@link QueryPlan} 直接组数。
 */
@Component
public class TaskWritePayloadEnricher {

    private static final Logger log = LoggerFactory.getLogger(TaskWritePayloadEnricher.class);

    private final QueryPlanDataService queryPlanDataService;
    private final ObjectMapper mapper = new ObjectMapper();

    public TaskWritePayloadEnricher(QueryPlanDataService queryPlanDataService) {
        this.queryPlanDataService = queryPlanDataService;
    }

    public TaskWritePayload enrich(
            TaskContext context, TaskWritePayload payload, String lastDataToolResultJson) {
        if (payload == null) {
            payload = new TaskWritePayload();
        }
        if (payload.getType() == null) {
            payload.setType(context.task().getType());
        }
        unwrapJsonEmbeddedInText(payload);
        TaskDefinition task = context.task();
        if (task.getType() == TaskType.chart && !hasChartData(payload)) {
            if (mergeChartFromToolJson(payload, lastDataToolResultJson)) {
                log.debug("task={} chart 写回已从 Tool 结果补全", task.getId());
            } else {
                requireQueryPlan(context);
                fillChartFromQueryPlan(context, payload);
                log.debug("task={} chart 写回已从 QueryPlan 补全", task.getId());
            }
        }
        if (task.getType() == TaskType.table && payload.getCells() == null) {
            QueryPlan plan = requireQueryPlan(context);
            if (context.structure() == null) {
                throw new RefreshException(
                        FailureStage.TASK_DTO_VALIDATE,
                        "TABLE_STRUCTURE_MISSING",
                        "表格结构未解析，无法从 QueryPlan 补全 cells",
                        task.getId(),
                        null);
            }
            payload.setCells(
                    queryPlanDataService.buildTableCells(
                            plan,
                            context.fundCode(),
                            context.structure().tableRows(),
                            context.structure().tableCols()));
            log.debug("task={} table cells 已从 QueryPlan 补全", task.getId());
        }
        return payload;
    }

    public static boolean isDataToolForTask(TaskDefinition task, String toolName) {
        if (task.getType() == TaskType.table) {
            return "fetchPerformanceTable".equals(toolName);
        }
        if (task.getType() == TaskType.chart) {
            if ("nav_chart".equals(task.getId())) {
                return "fetchNavChart".equals(toolName);
            }
            if ("allocation_chart".equals(task.getId())) {
                return "fetchAllocationChart".equals(toolName);
            }
            return "fetchAllocationChart".equals(toolName) || "fetchNavChart".equals(toolName);
        }
        return false;
    }

    private static boolean hasChartData(TaskWritePayload payload) {
        return payload.getCategories() != null
                && payload.getSeriesNames() != null
                && payload.getSeriesValues() != null;
    }

    private boolean mergeChartFromToolJson(TaskWritePayload payload, String toolJson) {
        if (!StringUtils.hasText(toolJson)) {
            return false;
        }
        try {
            JsonNode root = mapper.readTree(toolJson);
            if (payload.getCategories() == null && root.has("categories")) {
                payload.setCategories(readStringList(root.get("categories")));
            }
            if (payload.getSeriesNames() == null && root.has("seriesNames")) {
                payload.setSeriesNames(readStringList(root.get("seriesNames")));
            }
            if (payload.getSeriesValues() == null && root.has("seriesValues")) {
                payload.setSeriesValues(
                        mapper.convertValue(
                                root.get("seriesValues"),
                                new TypeReference<List<List<Double>>>() {}));
            }
            if (payload.getType() == null) {
                payload.setType(TaskType.chart);
            }
            return hasChartData(payload);
        } catch (Exception e) {
            log.warn("无法从 Tool JSON 合并 chart 字段: {}", e.getMessage());
            return false;
        }
    }

    private List<String> readStringList(JsonNode node) {
        return mapper.convertValue(node, new TypeReference<List<String>>() {});
    }

    private void fillChartFromQueryPlan(TaskContext context, TaskWritePayload payload) {
        QueryPlan plan = context.queryPlan();
        ChartSeriesData data;
        if ("nav_chart".equals(context.task().getId())) {
            data = queryPlanDataService.buildNavChart(plan, context.fundCode());
        } else {
            data = queryPlanDataService.buildAllocationChart(plan, context.fundCode());
        }
        payload.setType(TaskType.chart);
        payload.setCategories(data.categories());
        payload.setSeriesNames(data.seriesNames());
        payload.setSeriesValues(data.seriesValues());
    }

    private static QueryPlan requireQueryPlan(TaskContext context) {
        if (context.queryPlan() == null) {
            throw new RefreshException(
                    FailureStage.TASK_DTO_VALIDATE,
                    "QUERY_PLAN_REQUIRED",
                    "缺少 QueryPlan，无法补全表格/图表写回",
                    context.task().getId(),
                    null);
        }
        return context.queryPlan();
    }

    private void unwrapJsonEmbeddedInText(TaskWritePayload payload) {
        String text = payload.getText();
        if (!StringUtils.hasText(text) || !text.trim().startsWith("{")) {
            return;
        }
        try {
            TaskWritePayload inner = mapper.readValue(text.trim(), TaskWritePayload.class);
            if (inner.getType() != null) {
                payload.setType(inner.getType());
            }
            if (inner.getCategories() != null) {
                payload.setCategories(inner.getCategories());
            }
            if (inner.getSeriesNames() != null) {
                payload.setSeriesNames(inner.getSeriesNames());
            }
            if (inner.getSeriesValues() != null) {
                payload.setSeriesValues(inner.getSeriesValues());
            }
            if (inner.getCells() != null) {
                payload.setCells(inner.getCells());
            }
            if (inner.getText() != null && !inner.getText().trim().startsWith("{")) {
                payload.setText(inner.getText());
            } else if (hasChartData(payload) || payload.getCells() != null) {
                payload.setText(null);
            }
        } catch (Exception ignored) {
            // 普通 text 任务
        }
    }
}
