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
        List<Map<String, Object>> dims = new ArrayList<>();
        for (DimensionSlot slot : plan.dimensions()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("slotIndex", slot.slotIndex());
            d.put("role", slot.role().name());
            d.put("label", slot.label());
            if (slot.columnHeaders() != null && !slot.columnHeaders().isEmpty()) {
                d.put("columnHeaders", slot.columnHeaders());
            }
            if (slot.rowHeaders() != null && !slot.rowHeaders().isEmpty()) {
                d.put("rowHeaders", slot.rowHeaders());
            }
            if (slot.condition() != null) {
                d.put("condition", conditionMap(slot.condition()));
            }
            dims.add(d);
        }
        root.put("dimensions", dims);
        if (plan.writeBack() != null) {
            Map<String, Object> wb = new LinkedHashMap<>();
            wb.put("tableRows", plan.writeBack().tableRows());
            wb.put("tableCols", plan.writeBack().tableCols());
            wb.put("categoryLabels", plan.writeBack().categoryLabels());
            root.put("writeBack", wb);
        }
        return root;
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
        return m;
    }
}
