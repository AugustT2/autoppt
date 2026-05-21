package com.example.pptrefresh.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FundLabelMatcherTest {

    @Test
    void matchesWhenLabelContainsProductName() {
        assertTrue(
                FundLabelMatcher.matches("蓝海稳健增长混合A", "偏债混-蓝海稳健增长混合A"));
    }

    @Test
    void matchesExactAfterNormalize() {
        assertTrue(FundLabelMatcher.matches("蓝海稳健增长混合A", "蓝海稳健增长混合A"));
    }

    @Test
    void rejectsUnrelated() {
        assertFalse(FundLabelMatcher.matches("沪深300", "蓝海稳健增长混合A"));
    }
}
