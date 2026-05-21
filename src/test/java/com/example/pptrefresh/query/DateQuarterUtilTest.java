package com.example.pptrefresh.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateQuarterUtilTest {

    @Test
    void rollingQuartersEndingAt_asOf2026Q1() {
        List<String> q = DateQuarterUtil.rollingQuartersEndingAt("2026Q1", 4);
        assertEquals(List.of("2025Q2", "2025Q3", "2025Q4", "2026Q1"), q);
    }

    @Test
    void rollingQuartersEndingAt_yearBoundary() {
        List<String> q = DateQuarterUtil.rollingQuartersEndingAt("2026Q1", 2);
        assertEquals(List.of("2025Q4", "2026Q1"), q);
    }
}
