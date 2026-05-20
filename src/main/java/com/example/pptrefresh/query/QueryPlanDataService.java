package com.example.pptrefresh.query;

import com.example.pptrefresh.sample.ZhongOuSampleData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 根据 {@link QueryPlan} 逐维度拉数并组装表格 cells / 图表 series。 */
@Service
public class QueryPlanDataService {

    private final QueryPlanDataClient dataClient;

    public QueryPlanDataService(QueryPlanDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public List<List<String>> buildTableCells(
            QueryPlan plan, String fundCode, int tableRows, int tableCols) {
        List<List<String>> cells = new ArrayList<>();
        List<String> header = tableHeaderFromPlan(plan);
        int colCount = header.isEmpty() ? tableCols : header.size();
        if (!header.isEmpty()) {
            cells.add(padRow(header, colCount));
        }
        boolean columnOriented =
                plan.dimensions().stream().anyMatch(s -> s.role() == DimensionSlotRole.DATA_COLUMN);
        if (columnOriented) {
            cells.addAll(buildColumnOrientedRows(plan, fundCode, header, colCount));
        } else {
            for (DimensionSlot slot : plan.dimensions()) {
                if (slot.role() != DimensionSlotRole.DATA_ROW || slot.condition() == null) {
                    continue;
                }
                PerformanceRowData row =
                        dataClient.fetchPerformanceRow(fundCode, slot.condition());
                cells.add(
                        padRow(
                                List.of(
                                        slot.label(),
                                        row.returnPct(),
                                        row.peerRank(),
                                        row.percentile()),
                                colCount));
            }
        }
        return fitTable(cells, tableRows, tableCols);
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

    /** COLUMN 轴：表头横排区间，数据按「指标行 × 区间列」写回。 */
    private List<List<String>> buildColumnOrientedRows(
            QueryPlan plan, String fundCode, List<String> header, int colCount) {
        List<DimensionSlot> intervals =
                plan.dimensions().stream()
                        .filter(s -> s.role() == DimensionSlotRole.DATA_COLUMN && s.condition() != null)
                        .toList();
        if (intervals.isEmpty()) {
            return List.of();
        }
        List<String> metricLabels = tableRowHeadersFromPlan(plan);
        int metricRows = metricLabels.isEmpty() ? 3 : metricLabels.size();
        List<List<String>> rows = new ArrayList<>();
        for (int m = 0; m < metricRows; m++) {
            List<String> row = new ArrayList<>();
            row.add(metricLabelAt(metricLabels, m));
            for (DimensionSlot slot : intervals) {
                PerformanceRowData data =
                        dataClient.fetchPerformanceRow(fundCode, slot.condition());
                row.add(metricValueAt(data, m));
            }
            rows.add(padRow(row, colCount));
        }
        return rows;
    }

    private static List<String> tableRowHeadersFromPlan(QueryPlan plan) {
        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.role() == DimensionSlotRole.HEADER
                    && slot.rowHeaders() != null
                    && !slot.rowHeaders().isEmpty()) {
                return slot.rowHeaders();
            }
        }
        return List.of();
    }

    private static String metricLabelAt(List<String> metricLabels, int metricIndex) {
        if (metricIndex < metricLabels.size() && !metricLabels.get(metricIndex).isBlank()) {
            return metricLabels.get(metricIndex);
        }
        return switch (metricIndex) {
            case 0 -> "收益率";
            case 1 -> "同类排名";
            default -> "分位数";
        };
    }

    private static String metricValueAt(PerformanceRowData data, int metricIndex) {
        return switch (metricIndex) {
            case 0 -> data.returnPct();
            case 1 -> data.peerRank();
            default -> data.percentile();
        };
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
        return new ChartSeriesData(
                categories,
                ZhongOuSampleData.ALLOCATION_SERIES_NAMES,
                List.of(flattenSeries(stock), flattenSeries(bond), flattenSeries(cash)));
    }

    public ChartSeriesData buildNavChart(
            QueryPlan plan, String fundCode, String benchmarkName) {
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
        String bench = benchmarkName == null || benchmarkName.isBlank() ? "业绩基准" : benchmarkName;
        return new ChartSeriesData(
                categories, List.of("本基金", bench), List.of(fundSeries, benchSeries));
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
