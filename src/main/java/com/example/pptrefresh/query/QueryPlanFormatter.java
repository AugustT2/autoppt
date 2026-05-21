package com.example.pptrefresh.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryPlanFormatter {

    private final ObjectMapper mapper = new ObjectMapper();

    public String toJson(QueryPlan plan) {
        try {
            return mapper.writeValueAsString(toMap(plan));
        } catch (Exception e) {
            throw new IllegalStateException("无法序列化 QueryPlan", e);
        }
    }

    public Map<String, Object> toMap(QueryPlan plan) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("taskId", plan.taskId());
        root.put("asOfDate", plan.asOfDate().toString());
        root.put("asOfQuarter", plan.asOfQuarter());

        if (plan.tableMetrics() != null && !plan.tableMetrics().isEmpty()) {
            root.put("table", tableSection(plan));
        } else {
            root.put("dimensions", chartDimensions(plan));
            if (plan.chartSeries() != null && !plan.chartSeries().isEmpty()) {
                root.put("chartSeries", chartSeriesSection(plan));
            }
            if (plan.navTimeRange() != null) {
                root.put("navTimeRange", navTimeRangeMap(plan.navTimeRange()));
            }
        }

        if (plan.writeBack() != null) {
            Map<String, Object> wb = new LinkedHashMap<>();
            wb.put("tableRows", plan.writeBack().tableRows());
            wb.put("tableCols", plan.writeBack().tableCols());
            if (!plan.writeBack().categoryLabels().isEmpty()) {
                wb.put("categoryLabels", plan.writeBack().categoryLabels());
            }
            root.put("writeBack", wb);
        }
        return root;
    }

    private static Map<String, Object> tableSection(QueryPlan plan) {
        Map<String, Object> table = new LinkedHashMap<>();
        List<String> headers = List.of();
        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.role() == DimensionSlotRole.HEADER && slot.columnHeaders() != null) {
                headers = slot.columnHeaders();
                break;
            }
        }
        table.put("headers", headers);
        table.put("metrics", plan.tableMetrics());
        List<Map<String, Object>> intervals = new ArrayList<>();
        for (DimensionSlot slot : plan.dimensions()) {
            if (slot.condition() == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", slot.label());
            row.put("condition", conditionMap(slot.condition()));
            intervals.add(row);
        }
        table.put("intervals", intervals);
        return table;
    }

    private static List<Map<String, Object>> chartDimensions(QueryPlan plan) {
        List<Map<String, Object>> dims = new ArrayList<>();
        for (DimensionSlot slot : plan.dimensions()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("label", slot.label());
            if (slot.condition() != null) {
                d.put("condition", conditionMap(slot.condition()));
            }
            dims.add(d);
        }
        return dims;
    }

    private static Map<String, Object> conditionMap(QueryCondition c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", c.kind().name());
        m.put("label", c.label());
        if (c.startDate() != null) {
            m.put("startDate", c.startDate().toString());
        }
        if (c.endDate() != null) {
            m.put("endDate", c.endDate().toString());
        }
        if (c.quarter() != null) {
            m.put("quarter", c.quarter());
        }
        if (c.month() != null) {
            m.put("month", c.month());
        }
        if (c.day() != null) {
            m.put("day", c.day());
        }
        return m;
    }

    private static List<Map<String, Object>> chartSeriesSection(QueryPlan plan) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (ChartSeriesSlot slot : plan.chartSeries()) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("index", slot.index());
            s.put("role", slot.role().name());
            s.put("label", slot.label());
            if (slot.queryKey() != null) {
                s.put("queryKey", slot.queryKey());
            }
            series.add(s);
        }
        return series;
    }

    private static Map<String, Object> navTimeRangeMap(NavChartTimeRange range) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("startDate", range.startDate().toString());
        m.put("endDate", range.endDate().toString());
        m.put("granularity", range.granularity().name());
        m.put("axisLabels", range.axisLabels());
        return m;
    }
}
