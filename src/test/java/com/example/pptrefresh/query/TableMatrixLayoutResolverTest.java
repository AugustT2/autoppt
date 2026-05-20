package com.example.pptrefresh.query;

import com.example.pptrefresh.exception.RefreshException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableMatrixLayoutResolverTest {

    @Test
    void columnLayoutWhenIntervalLabelsShareOneRow() {
        List<List<String>> matrix =
                List.of(
                        List.of("", "任职以来", "过去一年", "过去两年"),
                        List.of("收益率", "1%", "2%", "3%"),
                        List.of("业绩排名", "10", "20", "30"));
        TableAnalysis analysis =
                TableMatrixLayoutResolver.resolve(
                        matrix,
                        new TableQueryIntent(
                                List.of("任职以来", "过去一年", "过去两年"),
                                List.of("收益率", "业绩排名")),
                        "test");
        assertEquals(TableLabelAxis.COLUMN, analysis.intervalAxis());
        assertEquals(0, analysis.intervalLabelIndex());
    }

    @Test
    void rowLayoutWhenIntervalLabelsShareOneColumn() {
        List<List<String>> matrix =
                List.of(
                        List.of("区间", "收益率", "同类排名"),
                        List.of("近一年", "1%", "10/100"),
                        List.of("近两年", "2%", "20/100"));
        TableAnalysis analysis =
                TableMatrixLayoutResolver.resolve(
                        matrix,
                        new TableQueryIntent(List.of("近一年", "近两年"), List.of("收益率", "同类排名")),
                        "test");
        assertEquals(TableLabelAxis.ROW, analysis.intervalAxis());
        assertEquals(0, analysis.intervalLabelIndex());
    }

    @Test
    void matrixAxisWinsWhenLlmAxisDisagrees() {
        List<List<String>> matrix =
                List.of(
                        List.of("", "任职以来", "过去一年"),
                        List.of("收益率", "1%", "2%"));
        TableQueryIntent intent =
                new TableQueryIntent(
                        List.of("任职以来", "过去一年"),
                        List.of("收益率"),
                        Optional.of(TableLabelAxis.ROW));
        TableAnalysis analysis = TableMatrixLayoutResolver.resolve(matrix, intent, "test");
        assertEquals(TableLabelAxis.COLUMN, analysis.intervalAxis());
    }

    @Test
    void throwsWhenLabelsNotOnSameRowOrColumn() {
        List<List<String>> matrix =
                List.of(
                        List.of("任职以来", "其他"),
                        List.of("无关", "近一年"));
        assertThrows(
                RefreshException.class,
                () ->
                        TableMatrixLayoutResolver.resolve(
                                matrix,
                                new TableQueryIntent(List.of("任职以来", "近一年"), List.of("x")),
                                "test"));
    }
}
