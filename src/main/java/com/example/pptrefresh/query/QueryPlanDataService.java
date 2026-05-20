package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 根据 QueryPlan 中的区间条件与指标列拉数并组装写回 cells / 图表 series。 */
@Service
public class QueryPlanDataService {

    private final QueryPlanDataClient dataClient;

    public QueryPlanDataService(QueryPlanDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public List<List<String>> buildTableCells(
            QueryPlan plan, String fundCode, int tableRows, int tableCols) {
        QueryPlanRequired.requireTableDataSlots(plan);
        List<String> metrics = plan.tableMetrics();
        if (metrics == null || metrics.isEmpty()) {
            throw new RefreshException(
                    FailureStage.QUERY_PLAN_BUILD,
                    "TABLE_METRICS_MISSING",
                    "QueryPlan 缺少 tableMetrics",
                    plan.taskId(),
                    null);
        }
        List<List<String>> cells = new ArrayList<>();
        List<String> header = tableHeaderFromPlan(plan);
        int colCount = header.isEmpty() ? tableCols : header.size();
        if (!header.isEmpty()) {
            cells.add(padRow(header, colCount));
        }
        boolean columnOriented =
                plan.dimensions().stream().anyMatch(s -> s.role() == DimensionSlotRole.DATA_COLUMN);
        if (columnOriented) {
            cells.addAll(buildColumnOrientedRows(plan, fundCode, metrics, colCount));
        } else {
            for (DimensionSlot slot : plan.dimensions()) {
                if (slot.role() != DimensionSlotRole.DATA_ROW || slot.condition() == null) {
                    continue;
                }
                PerformanceRowData row =
                        dataClient.fetchPerformanceRow(fundCode, slot.condition());
                cells.add(buildDataRow(slot.label(), row, metrics, colCount));
            }
        }
        return fitTable(cells, tableRows, tableCols);
    }

    private List<List<String>> buildColumnOrientedRows(
            QueryPlan plan, String fundCode, List<String> metrics, int colCount) {
        List<DimensionSlot> intervals =
                plan.dimensions().stream()
                        .filter(s -> s.role() == DimensionSlotRole.DATA_COLUMN && s.condition() != null)
                        .toList();
        List<List<String>> rows = new ArrayList<>();
        for (String metric : metrics) {
            List<String> row = new ArrayList<>();
            row.add(metric);
            for (DimensionSlot slot : intervals) {
                PerformanceRowData data =
                        dataClient.fetchPerformanceRow(fundCode, slot.condition());
                row.add(data.metricValue(metric));
            }
            rows.add(padRow(row, colCount));
        }
        return rows;
    }

    private static List<String> buildDataRow(
            String intervalLabel, PerformanceRowData row, List<String> metrics, int colCount) {
        List<String> cells = new ArrayList<>();
        cells.add(intervalLabel);
        for (String metric : metrics) {
            cells.add(row.metricValue(metric));
        }
        return padRow(cells, colCount);
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

    public ChartSeriesData buildAllocationChart(QueryPlan plan, String fundCode) {
        QueryPlanRequired.requireChartCategories(plan, "allocation");
        List<String> categories = new ArrayList<>();
        List<List<Double>> stock = new ArrayList<>();
        List<List<Double>> bond = new ArrayList<>();
        List<List<Double>> cash = new ArrayList<>();

        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.role() != DimensionSlotRole.CATEGORY || slot.condition() == null) {
                continue;
            }
            String quarter = slot.condition().quarter();
            if (quarter == null) {
                quarter = slot.label();
            }
            categories.add(quarter);
            double[] pct = dataClient.fetchAllocationPercents(fundCode, quarter);
            stock.add(List.of(pct[0]));
            bond.add(List.of(pct[1]));
            cash.add(List.of(pct[2]));
        }
        if (categories.isEmpty() && plan.writeBack() != null) {
            categories = new ArrayList<>(plan.writeBack().categoryLabels());
            for (String q : categories) {
                double[] pct = dataClient.fetchAllocationPercents(fundCode, q);
                stock.add(List.of(pct[0]));
                bond.add(List.of(pct[1]));
                cash.add(List.of(pct[2]));
            }
        }
        if (categories.isEmpty()) {
            throw emptyChart(plan, "allocation");
        }
        return new ChartSeriesData(
                categories,
                QueryPlanRequired.allocationSeriesNames(),
                List.of(flattenSeries(stock), flattenSeries(bond), flattenSeries(cash)));
    }

    public ChartSeriesData buildNavChart(
            QueryPlan plan, String fundCode, String benchmarkName) {
        QueryPlanRequired.requireChartCategories(plan, "nav");
        List<String> categories = new ArrayList<>();
        List<Double> fundSeries = new ArrayList<>();
        List<Double> benchSeries = new ArrayList<>();

        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.role() != DimensionSlotRole.CATEGORY || slot.condition() == null) {
                continue;
            }
            String month = slot.condition().month();
            if (month == null) {
                month = slot.label();
            }
            categories.add(month);
            fundSeries.add(dataClient.fetchCumulativeReturnPct(fundCode, month, false));
            benchSeries.add(dataClient.fetchCumulativeReturnPct(fundCode, month, true));
        }
        if (categories.isEmpty() && plan.writeBack() != null) {
            categories = new ArrayList<>(plan.writeBack().categoryLabels());
            for (String m : categories) {
                fundSeries.add(dataClient.fetchCumulativeReturnPct(fundCode, m, false));
                benchSeries.add(dataClient.fetchCumulativeReturnPct(fundCode, m, true));
            }
        }
        if (categories.isEmpty()) {
            throw emptyChart(plan, "nav");
        }
        String bench = benchmarkName == null || benchmarkName.isBlank() ? "业绩基准" : benchmarkName;
        return new ChartSeriesData(
                categories, List.of("本基金", bench), List.of(fundSeries, benchSeries));
    }

    private static RefreshException emptyChart(QueryPlan plan, String kind) {
        return new RefreshException(
                FailureStage.QUERY_PLAN_BUILD,
                "QUERY_PLAN_CHART_EMPTY",
                "QueryPlan 未能组装 " + kind + " 图表 categories",
                plan.taskId(),
                null);
    }

    private static List<Double> flattenSeries(List<List<Double>> perCategory) {
        List<Double> out = new ArrayList<>();
        for (List<Double> one : perCategory) {
            out.addAll(one);
        }
        return out;
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
