package com.example.pptrefresh.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntervalLexiconTest {

    private final IntervalLexicon lexicon =
            IntervalLexicon.load("/rules/lexicon/fund_performance_rows.yaml");

    @Test
    void resolvesTableHeaderLabelsFromTemplate() {
        assertEquals("CALENDAR_YEAR_2025", lexicon.resolveKind("2025年"));
        assertEquals("CALENDAR_YEAR_2026", lexicon.resolveKind("2026年"));
        assertEquals("YTD", lexicon.resolveKind("YTD"));
        assertEquals("SINCE_MANAGER_TENURE", lexicon.resolveKind("任职以来"));
        assertEquals("ROLLING_1Y", lexicon.resolveKind("近一年"));
        assertEquals("ROLLING_6M", lexicon.resolveKind("近六个月"));
    }

    @Test
    void resolvesSynonymsForLlmAliases() {
        assertNotNull(lexicon.resolveKind("过去一年"));
        assertEquals("ROLLING_1Y", lexicon.resolveKind("过去一年"));
        assertEquals("ROLLING_6M", lexicon.resolveKind("过去六个月"));
    }
}
