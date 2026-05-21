package com.example.pptrefresh.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingDayCalendarTest {

    @Test
    void excludesWeekends() {
        var labels =
                TradingDayCalendar.labelsBetween(
                        LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 30));
        assertTrue(labels.contains("2026-04-24"));
        assertTrue(labels.contains("2026-04-30"));
        assertEquals(5, labels.size());
    }
}
