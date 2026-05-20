package com.example.pptrefresh.query;

import java.time.LocalDate;
import java.util.List;

public final class QueryPlan {

    private final String taskId;
    private final LocalDate asOfDate;
    private final String asOfQuarter;
    private final List<DimensionSlot> dimensions;
    private final QueryPlanWriteBack writeBack;

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack) {
        this.taskId = taskId;
        this.asOfDate = asOfDate;
        this.asOfQuarter = asOfQuarter;
        this.dimensions = List.copyOf(dimensions);
        this.writeBack = writeBack;
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
}
