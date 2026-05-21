package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.RefreshException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NavBenchmarkLexiconTest {

    @Test
    void resolvesKnownBenchmarkLabel() {
        NavBenchmarkLexicon lexicon = NavBenchmarkLexicon.load(NavBenchmarkLexicon.DEFAULT_RESOURCE);
        assertEquals("bond_hybrid_index", lexicon.resolveBenchmarkKey("偏债混合基金指数", "nav_chart"));
    }

    @Test
    void failsOnUnknownLabel() {
        NavBenchmarkLexicon lexicon = NavBenchmarkLexicon.load(NavBenchmarkLexicon.DEFAULT_RESOURCE);
        assertThrows(
                RefreshException.class,
                () -> lexicon.resolveBenchmarkKey("未知基准XYZ", "nav_chart"));
    }
}
