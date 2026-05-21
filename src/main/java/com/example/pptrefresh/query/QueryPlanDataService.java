package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.query.metric.MetricCatalog;
import com.example.pptrefresh.query.metric.ResolvedMetric;
import com.example.pptrefresh.query.metric.TableMetricFetchService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 根据 QueryPlan 中的区间条件与指标列拉数并组装写回 cells / 图表 series。 */
@Service
public class QueryPlanDataService {

    private final QueryPlanDataClient dataClient;
    private final TableMetricFetchService tableMetricFetchService;

    public QueryPlanDataService(
            QueryPlanDataClient dataClient, TableMetricFetchService tableMetricFetchService) {
        this.dataClient = dataClient;
        this.tableMetricFetchService = tableMetricFetchService;
    }

    public List<List<String>> buildTableCells(
            QueryPlan plan, String fundCode, int tableRows, int tableCols) {
        QueryPlanRequired.requireTableDataSlots(plan);
        List<String> metricLabels = plan.tableMetrics();
        if (metricLabels == null || metricLabels.isEmpty()) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "TABLE_METRICS_MISSING",
                    "QueryPlan 缺少 tableMetrics",
                    plan.taskId(),
                    null);
        }
        MetricCatalog catalog =
                MetricCatalog.load(
                        plan.metricsCatalog() != null
                                ? plan.metricsCatalog()
                                : MetricCatalog.DEFAULT_RESOURCE);
        List<ResolvedMetric> resolved =
                tableMetricFetchService.resolveLabels(catalog, metricLabels);

        List<List<String>> cells = new ArrayList<>();
        List<String> header = tableHeaderFromPlan(plan);
        int colCount = header.isEmpty() ? tableCols : header.size();
        if (!header.isEmpty()) {
            cells.add(padRow(header, colCount));
        }
        boolean columnOriented =
                plan.dimensions().stream().anyMatch(s -> s.role() == DimensionSlotRole.DATA_COLUMN);
        if (columnOriented) {
            cells.addAll(buildColumnOrientedRows(plan, fundCode, resolved, colCount));
        } else {
            cells.addAll(buildRowOrientedRows(plan, fundCode, resolved, colCount));
        }
        return fitTable(cells, tableRows, tableCols);
    }

    private List<List<String>>  buildColumnOrientedRows(
            QueryPlan plan, String fundCode, List<ResolvedMetric> metrics, int colCount) {
        List<DimensionSlot> intervals =
                plan.dimensions().stream()
                        .filter(s -> s.role() == DimensionSlotRole.DATA_COLUMN && s.condition() != null)
                        .collect(Collectors.toList());
        List<Map<String, String>> valuesByInterval = new ArrayList<>();
        for (DimensionSlot slot : intervals) {
            valuesByInterval.add(
                    tableMetricFetchService.fetchForInterval(
                            fundCode, slot.condition(), metrics));
        }
        List<List<String>> rows = new ArrayList<>();
        for (ResolvedMetric metric : metrics) {
            List<String> row = new ArrayList<>();
            row.add(metric.displayLabel());
            for (Map<String, String> values : valuesByInterval) {
                row.add(values.getOrDefault(metric.metricId(), ""));
            }
            rows.add(padRow(row, colCount));
        }
        return rows;
    }

    private List<List<String>> buildRowOrientedRows(
            QueryPlan plan, String fundCode, List<ResolvedMetric> metrics, int colCount) {
        List<List<String>> rows = new ArrayList<>();
        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.role() != DimensionSlotRole.DATA_ROW || slot.condition() == null) {
                continue;
            }
            Map<String, String> values =
                    tableMetricFetchService.fetchForInterval(fundCode, slot.condition(), metrics);
            List<String> row = new ArrayList<>();
            row.add(slot.label());
            for (ResolvedMetric metric : metrics) {
                row.add(values.getOrDefault(metric.metricId(), ""));
            }
            rows.add(padRow(row, colCount));
        }
        return rows;
    }

    private static List<String> tableHeaderFromPlan(QueryPlan plan) {
        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.role() == DimensionSlotRole.HEADER
                    && slot.columnHeaders() != null
                    && !slot.columnHeaders().isEmpty()) {
                return slot.columnHeaders();
            }
        }
        return List.of();
    }

    private static List<String> padRow(List<String> row, int colCount) {
        List<String> out = new ArrayList<>(row);
        while (out.size() < colCount) {
            out.add("");
        }
        if (out.size() > colCount) {
            return new ArrayList<>(out.subList(0, colCount));
        }
        return out;
    }

    /** 横轴=大类资产，系列=季度（asset_class_allocation）。 */
    public ChartSeriesData buildAllocationChart(QueryPlan plan, String fundCode) {
        if (plan.writeBack() == null || plan.writeBack().categoryLabels().isEmpty()) {
            throw emptyChart(plan, "asset_class_allocation");
        }
        List<String> categories = plan.writeBack().categoryLabels();
        List<String> seriesNames = plan.chartSeriesNames();
        if (seriesNames == null || seriesNames.isEmpty()) {
            throw emptyChart(plan, "asset_class_allocation");
        }
        List<List<Double>> seriesValues = new ArrayList<>();
        for (String quarter : seriesNames) {
            seriesValues.add(fetchAllocationRow(plan.taskId(), fundCode, quarter, categories));
        }
        return new ChartSeriesData(categories, seriesNames, seriesValues);
    }

    private List<Double> fetchAllocationRow(
            String taskId, String fundCode, String quarter, List<String> assetClasses) {
        double[] raw = new double[assetClasses.size()];
        double sum = 0;
        for (int i = 0; i < assetClasses.size(); i++) {
            raw[i] =
                    dataClient.fetchAssetClassAllocationPct(
                            fundCode, quarter, assetClasses.get(i));
            sum += raw[i];
        }
        if (sum <= 0) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "ALLOCATION_DATA_EMPTY",
                    "资产配置占比合计为 0: quarter=" + quarter,
                    taskId,
                    null);
        }
        List<Double> row = new ArrayList<>();
        if (sum >= 99.0 && sum <= 101.0) {
            for (double v : raw) {
                row.add(round1(v));
            }
            return row;
        }
        for (double v : raw) {
            row.add(round1(v * 100.0 / sum));
        }
        return row;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public ChartSeriesData buildNavChart(QueryPlan plan, String fundCode) {
        if (plan.chartSeries() == null || plan.chartSeries().isEmpty()) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "NAV_CHART_SERIES_MISSING",
                    "nav 图表 QueryPlan 缺少 chartSeries（请使用 nav_line_series）",
                    plan.taskId(),
                    null);
        }
        return buildNavChartFromSeries(plan, fundCode);
    }

    private ChartSeriesData buildNavChartFromSeries(QueryPlan plan, String fundCode) {
        NavChartTimeRange range = plan.navTimeRange();
        if (range == null) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "NAV_TIME_RANGE_MISSING",
                    "nav 图表 QueryPlan 缺少 navTimeRange，无法批量取数",
                    plan.taskId(),
                    null);
        }
        List<String> categories = navAxisLabels(plan, range);
        if (categories.isEmpty()) {
            throw emptyChart(plan, "nav");
        }
        List<String> seriesNames = new ArrayList<>();
        List<List<Double>> seriesValues = new ArrayList<>();
        for (ChartSeriesSlot series : plan.chartSeries()) {
            seriesNames.add(series.label());
            NavSeriesPoints batch = fetchNavSeriesBatch(fundCode, series, range);
            List<Double> row = new ArrayList<>();
            for (String label : categories) {
                row.add(batch.require(label, plan.taskId(), series.label()));
            }
            seriesValues.add(row);
        }
        return new ChartSeriesData(categories, seriesNames, seriesValues);
    }

    private NavSeriesPoints fetchNavSeriesBatch(
            String fundCode, ChartSeriesSlot series, NavChartTimeRange range) {
        if (series.role() == ChartSeriesRole.FUND) {
            return dataClient.fetchFundNavReturnsInRange(fundCode, range);
        }
        return dataClient.fetchBenchmarkNavReturnsInRange(
                fundCode, series.queryKey(), range);
    }

    private static List<String> navAxisLabels(QueryPlan plan, NavChartTimeRange range) {
        if (!range.axisLabels().isEmpty()) {
            return range.axisLabels();
        }
        if (plan.writeBack() != null && !plan.writeBack().categoryLabels().isEmpty()) {
            return plan.writeBack().categoryLabels();
        }
        List<String> labels = new ArrayList<>();
        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.role() == DimensionSlotRole.CATEGORY
                    && slot.condition() != null
                    && slot.condition().kind() != QueryConditionKind.DATE_RANGE) {
                labels.add(slot.label());
            }
        }
        return labels;
    }

    private static RefreshException emptyChart(QueryPlan plan, String kind) {
        return new RefreshException(
                FailureStage.QUERY_PLAN_BUILD,
                "QUERY_PLAN_CHART_EMPTY",
                "QueryPlan 未能组装 " + kind + " 图表 categories",
                plan.taskId(),
                null);
    }

    private static List<List<String>> fitTable(
            List<List<String>> cells, int tableRows, int tableCols) {
        List<List<String>> out = new ArrayList<>();
        for (int r = 0; r < tableRows; r++) {
            List<String> src = r < cells.size() ? cells.get(r) : List.of();
            List<String> row = new ArrayList<>();
            for (int c = 0; c < tableCols; c++) {
                row.add(c < src.size() ? src.get(c) : "");
            }
            out.add(row);
        }
        return out;
    }
}
