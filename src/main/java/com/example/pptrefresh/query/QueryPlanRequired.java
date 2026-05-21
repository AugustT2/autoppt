package com.example.pptrefresh.query;

import java.util.List;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.llm.TaskContext;
import com.example.pptrefresh.llm.TaskContextHolder;

/** 表格/图表取数前校验 QueryPlan 已注入。 */
public final class QueryPlanRequired {

    private QueryPlanRequired() {}

    public static QueryPlan fromTaskContext() {
        TaskContext ctx = TaskContextHolder.get();
        if (ctx == null || ctx.queryPlan() == null) {
            String taskId = ctx != null ? ctx.task().getId() : null;
            throw new RefreshException(
                    FailureStage.TASK_TOOL,
                    "QUERY_PLAN_REQUIRED",
                    "缺少 QueryPlan，无法查询表格/图表数据（请确认编排已为 table/chart 任务构建 QueryPlan）",
                    taskId,
                    null);
        }
        return ctx.queryPlan();
    }

    public static void requireChartCategories(QueryPlan plan, String chartKind) {
        boolean hasCategory =
                plan.dimensions().stream()
                        .anyMatch(s -> s.role() == DimensionSlotRole.CATEGORY && s.condition() != null);
        boolean hasWriteBack =
                plan.writeBack() != null && !plan.writeBack().categoryLabels().isEmpty();
        if (!hasCategory && !hasWriteBack) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "QUERY_PLAN_CHART_EMPTY",
                    "QueryPlan 无图表维度: " + chartKind,
                    plan.taskId(),
                    null);
        }
    }

    public static void requireTableDataSlots(QueryPlan plan) {
        boolean hasData =
                plan.dimensions().stream()
                        .anyMatch(
                                s ->
                                        s.condition() != null
                                                && (s.role() == DimensionSlotRole.DATA_ROW
                                                        || s.role() == DimensionSlotRole.DATA_COLUMN));
        if (!hasData) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "QUERY_PLAN_TABLE_EMPTY",
                    "QueryPlan 无表格数据行/列维度",
                    plan.taskId(),
                    null);
        }
    }
}
