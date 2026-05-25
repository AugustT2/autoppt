package com.example.pptrefresh.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntervalLabelAlignerTest {

    @Test
    void mapsLexiconSynonymToMatrixHeader() {
        IntervalLexicon lexicon = IntervalLexicon.load("/rules/lexicon/fund_performance_rows.yaml");
        List<List<String>> matrix =
                List.of(
                        List.of("指标", "任职以来", "近一年", "近两年"),
                        List.of("收益率", "1%", "2%", "3%"));
        List<String> aligned =
                IntervalLabelAligner.alignToMatrix(
                        List.of("任职以来", "过去一年", "过去两年"), matrix, lexicon);
        assertEquals(List.of("任职以来", "近一年", "近两年"), aligned);
    }
}
