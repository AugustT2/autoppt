package com.example.pptrefresh.query;

import java.time.LocalDate;
import java.util.List;

/**
 * QueryPlan 是 Java 代码在调用 LLM 之前 生成的一个“取数清单”，主要包含了查询条件（dimensions）和查询指标。
 *
 * 它的核心作用是：帮 LLM 省去“算日期”和“懂结构”的麻烦，让 LLM 只专注于“调接口”。
 *
 * 具体包含三部分：
 *
 * 时间参数（已解析）：把 PPT 里的“近一年”直接算成 2025-04-30 ~ 2026-04-30。
 * 位置映射：告诉 LLM 取回来的数据应该填在表格的第几行第几列。
 * 业务意图：说明这个格子是要查“收益率”还是“夏普比率”。
 */
public final class QueryPlan {

    private final String taskId;
    private final LocalDate asOfDate;
    private final String asOfQuarter;
    private final List<DimensionSlot> dimensions;
    private final QueryPlanWriteBack writeBack;
    /** 表格任务：需查询并刷新的指标列（如收益率、同类排名）。 */
    private final List<String> tableMetrics;
    /** 指标词表 classpath 路径（如 /rules/lexicon/table_metrics.yaml）。 */
    private final String metricsCatalog;
    /** 资产配置图：系列名（季度），横轴见 writeBack.categoryLabels。 */
    private final List<String> chartSeriesNames;
    /** 累计收益折线图：模板系列槽（本基金 + 若干基准）。 */
    private final List<ChartSeriesSlot> chartSeries;
    /** 累计收益折线图：批量取数时间窗（起止日 + 横轴展示标签）。 */
    private final NavChartTimeRange navTimeRange;

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack) {
        this(taskId, asOfDate, asOfQuarter, dimensions, writeBack, null, null, null, null, null);
    }

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack,
            List<String> tableMetrics) {
        this(taskId, asOfDate, asOfQuarter, dimensions, writeBack, tableMetrics, null, null, null, null);
    }

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack,
            List<String> tableMetrics,
            String metricsCatalog) {
        this(taskId, asOfDate, asOfQuarter, dimensions, writeBack, tableMetrics, metricsCatalog, null, null, null);
    }

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack,
            List<String> tableMetrics,
            String metricsCatalog,
            List<String> chartSeriesNames) {
        this(
                taskId,
                asOfDate,
                asOfQuarter,
                dimensions,
                writeBack,
                tableMetrics,
                metricsCatalog,
                chartSeriesNames,
                null,
                null);
    }

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack,
            List<String> tableMetrics,
            String metricsCatalog,
            List<String> chartSeriesNames,
            List<ChartSeriesSlot> chartSeries) {
        this(
                taskId,
                asOfDate,
                asOfQuarter,
                dimensions,
                writeBack,
                tableMetrics,
                metricsCatalog,
                chartSeriesNames,
                chartSeries,
                null);
    }

    public QueryPlan(
            String taskId,
            LocalDate asOfDate,
            String asOfQuarter,
            List<DimensionSlot> dimensions,
            QueryPlanWriteBack writeBack,
            List<String> tableMetrics,
            String metricsCatalog,
            List<String> chartSeriesNames,
            List<ChartSeriesSlot> chartSeries,
            NavChartTimeRange navTimeRange) {
        this.taskId = taskId;
        this.asOfDate = asOfDate;
        this.asOfQuarter = asOfQuarter;
        this.dimensions = List.copyOf(dimensions);
        this.writeBack = writeBack;
        this.tableMetrics =
                tableMetrics == null ? null : List.copyOf(tableMetrics);
        this.metricsCatalog = metricsCatalog;
        this.chartSeriesNames =
                chartSeriesNames == null ? null : List.copyOf(chartSeriesNames);
        this.chartSeries = chartSeries == null ? null : List.copyOf(chartSeries);
        this.navTimeRange = navTimeRange;
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

    public String metricsCatalog() {
        return metricsCatalog;
    }

    public List<String> chartSeriesNames() {
        if (chartSeries != null && !chartSeries.isEmpty()) {
            return chartSeries.stream().map(ChartSeriesSlot::label).collect(java.util.stream.Collectors.toList());
        }
        return chartSeriesNames;
    }

    public List<ChartSeriesSlot> chartSeries() {
        return chartSeries;
    }

    public NavChartTimeRange navTimeRange() {
        return navTimeRange;
    }
}
