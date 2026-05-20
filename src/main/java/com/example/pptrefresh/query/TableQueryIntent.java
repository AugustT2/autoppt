package com.example.pptrefresh.query;

import java.util.List;
import java.util.Optional;

/** LLM 从表格矩阵识别的查询意图：要查哪些区间、哪些指标（不含日期）。 */
public final class TableQueryIntent {

    private final List<String> intervalLabels;
    private final List<String> metrics;
    private final Optional<TableLabelAxis> intervalAxis;

    public TableQueryIntent(List<String> intervalLabels, List<String> metrics) {
        this(intervalLabels, metrics, Optional.empty());
    }

    public TableQueryIntent(
            List<String> intervalLabels,
            List<String> metrics,
            Optional<TableLabelAxis> intervalAxis) {
        this.intervalLabels = List.copyOf(intervalLabels);
        this.metrics = List.copyOf(metrics);
        this.intervalAxis = intervalAxis == null ? Optional.empty() : intervalAxis;
    }

    public List<String> intervalLabels() {
        return intervalLabels;
    }

    public List<String> metrics() {
        return metrics;
    }

    /** LLM 可选输出的区间轴；最终以矩阵定位为准。 */
    public Optional<TableLabelAxis> intervalAxis() {
        return intervalAxis;
    }
}
