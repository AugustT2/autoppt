package com.example.pptrefresh.query;

import java.time.LocalDate;
import java.util.List;

public final class QueryPlan {

    private final String taskId;
    private final LocalDate asOfDate;
    private final String asOfQuarter;
    private final List<DimensionSlot> dimensions;
    private final QueryPlanWriteBack writeBack;
    /** 表格任务：需查询并刷新的指标列（如收益率、同类排名）。 */
    private final List<String> tableMetrics;

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack) {
        this(taskId, asOfDate, asOfQuarter, dimensions, writeBack, null);
    }

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack,
            List<String> tableMetrics) {
        this.taskId = taskId;
        this.asOfDate = asOfDate;
        this.asOfQuarter = asOfQuarter;
        this.dimensions = List.copyOf(dimensions);
        this.writeBack = writeBack;
        this.tableMetrics =
                tableMetrics == null ? null : List.copyOf(tableMetrics);
    }

    public String taskId() {
        return taskId;
    }

    public LocalDate asOfDate() {
        return asOfDate;
    }

    public String asOfQuarter() {
        return asOfQuarter;
    }

    public List<DimensionSlot> dimensions() {
        return dimensions;
    }

    public QueryPlanWriteBack writeBack() {
        return writeBack;
    }

    public List<String> tableMetrics() {
        return tableMetrics;
    }
}
