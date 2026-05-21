package com.example.pptrefresh.query;

import com.example.pptrefresh.time.TradingDayCalendar;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavChartTimeRangeResolverTest {

    private final NavChartTimeRangeResolver resolver = new NavChartTimeRangeResolver();

    @Test
    void resolvesDayRangeAndFullTradingDayAxisFromSparseTemplateLabels() {
        List<String> sparse =
                List.of("2026-01-15", "2026-03-20", "2026-04-30");
        NavChartTimeRange range = resolver.resolve(sparse, LocalDate.of(2026, 4, 30));
        assertEquals(NavChartAxisGranularity.DAY, range.granularity());
        assertEquals(LocalDate.of(2026, 1, 15), range.startDate());
        assertEquals(LocalDate.of(2026, 4, 30), range.endDate());
        List<String> expected =
                TradingDayCalendar.labelsBetween(
                        LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 30));
        assertEquals(expected, range.axisLabels());
        assertTrue(range.axisLabels().size() > sparse.size());
    }

    @Test
    void clampsEndToAsOfDate() {
        List<String> labels = List.of("2026-01-01", "2026-06-30");
        NavChartTimeRange range = resolver.resolve(labels, LocalDate.of(2026, 4, 30));
        assertEquals(LocalDate.of(2026, 4, 30), range.endDate());
    }
}
