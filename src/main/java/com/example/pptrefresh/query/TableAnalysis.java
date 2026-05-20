package com.example.pptrefresh.query;

import java.util.List;

/**
 * 表格刷新完整分析结果：区间标签 + 指标列 + 写回布局（布局由 Java 从矩阵推断）。
 */
public final class TableAnalysis {

    private final List<String> intervalLabels;
    private final List<String> metrics;
    private final List<String> columnHeaders;
    private final List<String> rowHeaders;
    private final TableLabelAxis intervalAxis;
    private final int intervalLabelIndex;
    private final String source;

    public TableAnalysis(
            List<String> intervalLabels,
            List<String> metrics,
            List<String> columnHeaders,
            List<String> rowHeaders,
            TableLabelAxis intervalAxis,
            int intervalLabelIndex,
            String source) {
        this.intervalLabels = List.copyOf(intervalLabels);
        this.metrics = List.copyOf(metrics);
        this.columnHeaders = List.copyOf(columnHeaders);
        this.rowHeaders = List.copyOf(rowHeaders);
        this.intervalAxis = intervalAxis;
        this.intervalLabelIndex = intervalLabelIndex;
        this.source = source;
    }

    public List<String> intervalLabels() {
        return intervalLabels;
    }

    public List<String> metrics() {
        return metrics;
    }

    public List<String> columnHeaders() {
        return columnHeaders;
    }

    public List<String> rowHeaders() {
        return rowHeaders;
    }

    public TableLabelAxis intervalAxis() {
        return intervalAxis;
    }

    public int intervalLabelIndex() {
        return intervalLabelIndex;
    }

    public String source() {
        return source;
    }
}
